import json
import os
import random
import re
import time
from dataclasses import dataclass
from datetime import datetime
from urllib.parse import urljoin, urlparse
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}

BASE_URL = "https://www.peoplehibl.com/"
SEED_LIST_URLS = [
    "https://www.peoplehibl.com/html/zhengce/",
    "https://www.peoplehibl.com/html/zixun/",
]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_RAW = os.path.join(BASE_DIR, "data", "raw", "peoplehibl_docs.jsonl")

# 低强度抓取参数
MAX_PAGES = int(os.getenv("PEOPLEHIBL_MAX_PAGES", "3"))
SLEEP_SECONDS = float(os.getenv("PEOPLEHIBL_SLEEP", "3.0"))
JITTER_MIN = float(os.getenv("PEOPLEHIBL_JITTER_MIN", "1.0"))
JITTER_MAX = float(os.getenv("PEOPLEHIBL_JITTER_MAX", "2.5"))
REQUEST_TIMEOUT = int(os.getenv("PEOPLEHIBL_TIMEOUT", "20"))
MAX_RETRIES = int(os.getenv("PEOPLEHIBL_MAX_RETRIES", "2"))
MAX_REQUESTS = int(os.getenv("PEOPLEHIBL_MAX_REQUESTS", "120"))
RESPECT_ROBOTS = os.getenv("PEOPLEHIBL_RESPECT_ROBOTS", "1") == "1"
ROBOTS_USER_AGENT = os.getenv("PEOPLEHIBL_ROBOTS_UA", "*")
MIN_TEXT_LEN = int(os.getenv("PEOPLEHIBL_MIN_TEXT_LEN", "120"))
VERBOSE = os.getenv("PEOPLEHIBL_VERBOSE", "1") == "1"


def log(msg: str) -> None:
    if VERBOSE:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


@dataclass
class Doc:
    url: str
    title: str
    publish_time: str
    content: str


def polite_sleep() -> None:
    delay = SLEEP_SECONDS + random.uniform(JITTER_MIN, JITTER_MAX)
    delay = max(delay, 0.1)
    log(f"sleep {delay:.2f}s")
    time.sleep(delay)


def build_robot_parser() -> RobotFileParser | None:
    if not RESPECT_ROBOTS:
        log("robots 检查已关闭")
        return None
    rp = RobotFileParser()
    robots_url = urljoin(BASE_URL, "/robots.txt")
    rp.set_url(robots_url)
    try:
        rp.read()
        log(f"robots 已加载: {robots_url}")
        return rp
    except Exception as e:
        log(f"robots 加载失败，按允许处理: {e}")
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
            log(f"GET {url} (attempt {attempt}/{MAX_RETRIES})")
            resp = requests.get(url, headers=HEADERS, timeout=REQUEST_TIMEOUT)
            resp.raise_for_status()
            resp.encoding = resp.apparent_encoding
            log(f"OK {url} status={resp.status_code} size={len(resp.text)}")
            return resp.text
        except Exception as e:
            last_err = e
            backoff = 1.5 + random.uniform(0.2, 0.8)
            log(f"FAIL {url} err={e}; backoff {backoff:.2f}s")
            time.sleep(backoff)
    raise RuntimeError(f"请求失败: {url}, err={last_err}")


def iter_list_pages(seed: str):
    yield seed
    for p in range(2, MAX_PAGES + 1):
        sep = "&" if "?" in seed else "?"
        yield f"{seed}{sep}page={p}"


def is_detail_url(url: str) -> bool:
    # 例：https://www.peoplehibl.com/html/2026/zixun_0428/1938.html
    # 例：https://www.peoplehibl.com/html/2025/zhengce_1113/1506.html
    path = urlparse(url).path.lower()
    return re.search(r"^/html/20\d{2}/(zhengce|zixun)_\d{4}/\d+\.html$", path) is not None


def extract_detail_links(html: str, base_url: str) -> list[str]:
    soup = BeautifulSoup(html, "html.parser")
    links: list[str] = []
    for a in soup.select("a[href]"):
        href = (a.get("href") or "").strip()
        if not href or href.startswith("javascript"):
            continue
        full = urljoin(base_url, href)
        if not full.startswith(BASE_URL):
            continue
        if is_detail_url(full):
            links.append(full)
    return list(dict.fromkeys(links))


def extract_publish_time(soup: BeautifulSoup, txt: str) -> str:
    pats = [r"(20\d{2}[-/.]\d{1,2}[-/.]\d{1,2}\s*\d{0,2}:?\d{0,2}:?\d{0,2})", r"(20\d{2}年\d{1,2}月\d{1,2}日)"]
    for sel in [".time", ".date", ".article-info", ".info", ".source"]:
        node = soup.select_one(sel)
        if node:
            t = node.get_text(" ", strip=True)
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

    # 去掉你明确不要的区域：热门视频/热门资讯/公众号/页脚
    for sel in [
        "footer", ".footer", "#footer",
        ".hot-news", ".hot", ".remen", ".right", ".sidebar", ".aside",
        ".weixin", ".wx", ".gzh", ".qrcode", ".code",
        "nav", ".nav", ".menu", "header",
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

    candidates = []
    for sel in ["article", ".article", ".article-content", ".content", ".show_text", "#content", ".news-content"]:
        node = soup.select_one(sel)
        if node:
            txt = node.get_text("\n", strip=True)
            txt = re.sub(r"[ \t]+", " ", txt)
            txt = re.sub(r"\n{3,}", "\n\n", txt).strip()
            if txt:
                candidates.append(txt)

    content = max(candidates, key=len) if candidates else soup.get_text("\n", strip=True)

    # 行级去噪（侧栏/版权/相关推荐）
    noise = [
        r"热门视频", r"热门资讯", r"微信公众号", r"相关阅读", r"相关推荐",
        r"版权", r"京ICP备", r"工信部备案", r"关于我们", r"联系我们",
        r"人民研学网简介", r"战略合作", r"联系我们",
        r"互联网新闻信息服务许可证", r"10120180013",
        r"京ICP备17062222号[-—]?8",
        r"《人民网刊》杂志社有限责任公司", r"举报电话",
        r"010-65363533",
        r"Copyright\s*©\s*2019[-—]2026", r"All\s+Rights\s+Reserved",
    ]
    lines = []
    for ln in content.split("\n"):
        s = re.sub(r"\s+", " ", ln).strip()
        if not s:
            continue
        if any(re.search(p, s, flags=re.IGNORECASE) for p in noise):
            continue
        lines.append(s)
    content = "\n".join(lines).strip()

    publish_time = extract_publish_time(soup, content)
    return title, publish_time, content


def main() -> None:
    os.makedirs(os.path.dirname(OUT_RAW), exist_ok=True)
    log(f"启动爬虫: seeds={len(SEED_LIST_URLS)}, max_pages={MAX_PAGES}, max_requests={MAX_REQUESTS}")
    rp = build_robot_parser()
    request_count = 0

    detail_links: list[str] = []
    for seed in SEED_LIST_URLS:
        log(f"开始列表抓取 seed={seed}")
        for list_url in iter_list_pages(seed):
            if request_count >= MAX_REQUESTS:
                log("达到最大请求上限，停止列表抓取")
                break
            if not allowed(rp, list_url):
                log(f"robots 禁止访问列表页，跳过: {list_url}")
                continue
            try:
                html = fetch(list_url)
                request_count += 1
                found = extract_detail_links(html, BASE_URL)
                detail_links.extend(found)
                log(f"列表完成: {list_url} -> 提取详情 {len(found)}，累计 {len(detail_links)}")
                polite_sleep()
            except Exception as e:
                log(f"列表失败: {list_url} err={e}")
                continue

    detail_links = list(dict.fromkeys(detail_links))
    log(f"详情链接去重后数量: {len(detail_links)}")

    docs: list[Doc] = []
    for idx, url in enumerate(detail_links, start=1):
        if request_count >= MAX_REQUESTS:
            log("达到最大请求上限，停止详情抓取")
            break
        if not allowed(rp, url):
            log(f"robots 禁止访问详情页，跳过: {url}")
            continue
        log(f"详情进度 {idx}/{len(detail_links)} -> {url}")
        try:
            html = fetch(url)
            request_count += 1
            title, publish_time, content = parse_detail(html)
            if not title:
                log(f"跳过: 标题为空 url={url}")
                polite_sleep()
                continue
            if len(content) < MIN_TEXT_LEN:
                log(f"跳过: 正文过短 len={len(content)} url={url}")
                polite_sleep()
                continue
            docs.append(Doc(url=url, title=title, publish_time=publish_time, content=content))
            log(f"入库成功: title={title[:28]}..., date={publish_time or 'N/A'}, len={len(content)}")
            polite_sleep()
        except Exception as e:
            log(f"详情失败: {url} err={e}")
            continue

    log(f"开始写出文件: {OUT_RAW}")
    with open(OUT_RAW, "w", encoding="utf-8") as f:
        for d_idx, d in enumerate(docs, start=1):
            obj = {
                "url": d.url,
                "title": d.title,
                "publish_time": d.publish_time,
                "text": d.content,
                "metadata": {
                    "source": "peoplehibl",
                    "url": d.url,
                    "title": d.title,
                    "publish_time": d.publish_time,
                },
            }
            f.write(json.dumps(obj, ensure_ascii=False) + "\n")

    print(f"完成：{len(docs)} 篇详情")
    print(f"输出：{OUT_RAW}")


if __name__ == "__main__":
    main()
