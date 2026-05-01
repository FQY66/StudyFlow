import json
import os
import random
import re
import time
from datetime import datetime
from urllib.parse import urljoin, urlparse
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://iw.guet.edu.cn/"
LIST_PREFIX = "https://iw.guet.edu.cn/4749/"
TOTAL_PAGES = int(os.getenv("GUET_KX_TOTAL_PAGES", "164"))

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(BASE_DIR, "data", "raw")
OUT_ALL_LINKS = os.path.join(OUT_DIR, "guet_kuaixun_all_detail_links.txt")
OUT_UNIQUE_LINKS = os.path.join(OUT_DIR, "guet_kuaixun_unique_detail_links.txt")
OUT_DOCS = os.path.join(OUT_DIR, "guet_kuaixun_docs.jsonl")

SLEEP_SECONDS = float(os.getenv("GUET_KX_SLEEP", "1.8"))
JITTER_MIN = float(os.getenv("GUET_KX_JITTER_MIN", "0.4"))
JITTER_MAX = float(os.getenv("GUET_KX_JITTER_MAX", "1.2"))
TIMEOUT = int(os.getenv("GUET_KX_TIMEOUT", "20"))
MAX_RETRIES = int(os.getenv("GUET_KX_MAX_RETRIES", "3"))
RESPECT_ROBOTS = os.getenv("GUET_KX_RESPECT_ROBOTS", "1") == "1"
ROBOTS_USER_AGENT = os.getenv("GUET_KX_ROBOTS_UA", "*")
MIN_TEXT_LEN = int(os.getenv("GUET_KX_MIN_TEXT_LEN", "80"))

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}


def log(msg: str) -> None:
    print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


def polite_sleep() -> None:
    delay = max(0.1, SLEEP_SECONDS + random.uniform(JITTER_MIN, JITTER_MAX))
    time.sleep(delay)


def build_robot_parser() -> RobotFileParser | None:
    if not RESPECT_ROBOTS:
        return None
    rp = RobotFileParser()
    rp.set_url(urljoin(BASE_URL, "/robots.txt"))
    try:
        rp.read()
        return rp
    except Exception:
        return None


def allowed(rp: RobotFileParser | None, url: str) -> bool:
    if rp is None:
        return True
    try:
        return rp.can_fetch(ROBOTS_USER_AGENT, url)
    except Exception:
        return True


def fetch(url: str) -> str:
    last_err: Exception | None = None
    for attempt in range(1, MAX_RETRIES + 1):
        try:
            r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
            r.raise_for_status()
            r.encoding = r.apparent_encoding
            return r.text
        except Exception as e:
            last_err = e
            backoff = 1.2 + random.uniform(0.2, 0.8)
            log(f"请求失败({attempt}/{MAX_RETRIES}) {url} err={e}; backoff={backoff:.2f}s")
            time.sleep(backoff)
    raise RuntimeError(f"请求失败: {url}, err={last_err}")


def list_page_url(page: int) -> str:
    # 用户给出的样例包含 .htm 与 .psp，两种都尝试
    if page <= 2:
        return f"{LIST_PREFIX}list{page}.htm"
    return f"{LIST_PREFIX}list{page}.psp"


def is_detail_url(url: str) -> bool:
    p = urlparse(url).path.lower()

    # 排除列表页
    if re.search(r"/list\d*\.(htm|html|psp)$", p):
        return False

    # 站点真实详情页模式（例如）：
    # /2026/0427/c4749a151714/page.htm
    # /2026/0427/c4749a151714/page.psp
    if re.search(r"^/20\d{2}/\d{4}/c\d+a\d+/page\.(htm|html|psp)$", p):
        return True

    # 兜底：一般新闻详情末尾 page.htm / page.html / page.psp
    if re.search(r"/page\.(htm|html|psp)$", p):
        return True

    return False


def extract_list_links(html: str, page_url: str) -> list[str]:
    soup = BeautifulSoup(html, "html.parser")

    # 优先定位“校园快讯”模块
    candidates = []
    for sel in [
        ".wp_article_list a[href]",
        ".news_list a[href]",
        ".list a[href]",
        ".new_list a[href]",
        "table a[href]",
        "li a[href]",
    ]:
        links = soup.select(sel)
        if links:
            candidates = links
            break

    if not candidates:
        candidates = soup.select("a[href]")

    out: list[str] = []
    for a in candidates:
        href = (a.get("href") or "").strip()
        if not href or href.startswith("javascript"):
            continue

        text = re.sub(r"\s+", " ", a.get_text(" ", strip=True)).strip()
        if not text:
            continue

        full = urljoin(page_url, href)
        if not full.startswith(BASE_URL):
            continue

        # 过滤你给的第2/3张图所示内容（站点头部/导航/检索入口）
        if any(k in text for k in ["首页", "信息公开", "学校简介", "搜索", "关键字", "开始时间", "结束时间", "分类"]):
            continue

        if is_detail_url(full):
            out.append(full)

    return out


def extract_publish_time(soup: BeautifulSoup, txt: str) -> str:
    pats = [
        r"(20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2}(?:日)?(?:\s*\d{1,2}:\d{1,2}(?::\d{1,2})?)?)",
    ]
    for sel in [".arti_metas", ".info", ".article-info", ".time", ".date", ".meta"]:
        n = soup.select_one(sel)
        if n:
            t = n.get_text(" ", strip=True)
            for p in pats:
                m = re.search(p, t)
                if m:
                    return m.group(1)
    for p in pats:
        m = re.search(p, txt)
        if m:
            return m.group(1)
    return ""


def parse_detail(html: str) -> tuple[str, str, str]:
    soup = BeautifulSoup(html, "html.parser")

    # 去掉头部、导航、检索区（对应你第二、第三张图）
    for sel in [
        "header", "nav", "footer", "form", "script", "style", "noscript",
        ".head", ".header", ".top", ".banner", ".menu", ".nav", ".search", ".search_box", ".search-form",
        ".sidebar", ".right", ".left", ".list-search", ".channel-search",
    ]:
        for n in soup.select(sel):
            n.decompose()

    title = ""
    h1 = soup.find("h1")
    if h1 and h1.get_text(strip=True):
        title = h1.get_text(strip=True)
    elif soup.title and soup.title.get_text(strip=True):
        title = soup.title.get_text(strip=True)

    for img in soup.find_all("img"):
        img.decompose()

    blocks = []
    for sel in [
        "article", ".article", ".article-content", ".content", "#content",
        ".v_news_content", ".wp_articlecontent", ".TRS_Editor", ".news_content",
    ]:
        node = soup.select_one(sel)
        if node:
            txt = node.get_text("\n", strip=True)
            txt = re.sub(r"[ \t]+", " ", txt)
            txt = re.sub(r"\n{3,}", "\n\n", txt).strip()
            if txt:
                blocks.append(txt)

    content = max(blocks, key=len) if blocks else soup.get_text("\n", strip=True)

    noise_patterns = [
        r"首页", r"信息公开", r"学校简介", r"本科教学服务网", r"研究生教学服务网",
        r"国际教学服务网", r"招生就业", r"图书馆", r"北海校区", r"智慧校园", r"电话查询",
        r"关键字", r"开始时间", r"结束时间", r"分类", r"搜索",
    ]
    lines = []
    for ln in content.split("\n"):
        s = re.sub(r"\s+", " ", ln).strip()
        if not s:
            continue
        if any(re.search(p, s, flags=re.IGNORECASE) for p in noise_patterns):
            continue
        lines.append(s)

    content = "\n".join(lines).strip()
    publish_time = extract_publish_time(soup, content)
    return title, publish_time, content


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    rp = build_robot_parser()

    all_links: list[str] = []
    failed_pages: list[str] = []

    for p in range(1, TOTAL_PAGES + 1):
        url = list_page_url(p)
        log(f"[{p}/{TOTAL_PAGES}] 列表页: {url}")

        if not allowed(rp, url):
            log("  - robots 禁止，跳过")
            continue

        try:
            html = fetch(url)
            links = extract_list_links(html, url)
            all_links.extend(links)
            log(f"  - 提取 {len(links)} 条，累计 {len(all_links)}")
        except Exception as e:
            failed_pages.append(url)
            log(f"  - 抓取失败: {e}")

        polite_sleep()

    unique_links = list(dict.fromkeys(all_links))

    with open(OUT_ALL_LINKS, "w", encoding="utf-8") as f:
        for u in all_links:
            f.write(u + "\n")

    with open(OUT_UNIQUE_LINKS, "w", encoding="utf-8") as f:
        for u in unique_links:
            f.write(u + "\n")

    docs = []
    for i, u in enumerate(unique_links, start=1):
        log(f"[DOC {i}/{len(unique_links)}] {u}")
        if not allowed(rp, u):
            log("  - robots 禁止，跳过")
            continue
        try:
            html = fetch(u)
            title, publish_time, content = parse_detail(html)
            if len(content) < MIN_TEXT_LEN:
                log(f"  - 文本过短({len(content)})，跳过")
                polite_sleep()
                continue
            docs.append({
                "url": u,
                "title": title,
                "publish_time": publish_time,
                "content": content,
                "source": "guet_kuaixun",
            })
            log(f"  - OK title={title[:30]}... len={len(content)}")
        except Exception as e:
            log(f"  - 详情抓取失败: {e}")
        polite_sleep()

    with open(OUT_DOCS, "w", encoding="utf-8") as f:
        for d in docs:
            f.write(json.dumps(d, ensure_ascii=False) + "\n")

    print("\n=== 完成 ===")
    print(f"列表页总数: {TOTAL_PAGES}")
    print(f"详情链接(含重复): {len(all_links)}")
    print(f"详情链接(去重后): {len(unique_links)}")
    print(f"有效正文文档数: {len(docs)}")
    print(f"列表页失败数: {len(failed_pages)}")
    print(f"输出: {OUT_ALL_LINKS}")
    print(f"输出: {OUT_UNIQUE_LINKS}")
    print(f"输出: {OUT_DOCS}")


if __name__ == "__main__":
    main()
