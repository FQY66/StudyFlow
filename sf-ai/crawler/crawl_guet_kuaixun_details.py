import json
import os
import random
import re
import time
from datetime import datetime
from urllib.parse import urljoin
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://iw.guet.edu.cn/"
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
RAW_DIR = os.path.join(BASE_DIR, "data", "raw")

IN_LINKS = os.path.join(RAW_DIR, "guet_kuaixun_unique_detail_links.txt")
OUT_DOCS = os.path.join(RAW_DIR, "guet_kuaixun_docs.jsonl")

SLEEP_SECONDS = float(os.getenv("GUET_KX_SLEEP", "1.6"))
JITTER_MIN = float(os.getenv("GUET_KX_JITTER_MIN", "0.3"))
JITTER_MAX = float(os.getenv("GUET_KX_JITTER_MAX", "0.9"))
TIMEOUT = int(os.getenv("GUET_KX_TIMEOUT", "20"))
MAX_RETRIES = int(os.getenv("GUET_KX_MAX_RETRIES", "3"))
RESPECT_ROBOTS = os.getenv("GUET_KX_RESPECT_ROBOTS", "1") == "1"
ROBOTS_USER_AGENT = os.getenv("GUET_KX_ROBOTS_UA", "*")
MIN_TEXT_LEN = int(os.getenv("GUET_KX_MIN_TEXT_LEN", "30"))

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
            wait_s = 1.0 + random.uniform(0.2, 0.8)
            log(f"请求失败({attempt}/{MAX_RETRIES}) {url} err={e}; backoff={wait_s:.2f}s")
            time.sleep(wait_s)
    raise RuntimeError(f"请求失败: {url}, err={last_err}")


def load_links(path: str) -> list[str]:
    if not os.path.exists(path):
        raise FileNotFoundError(f"未找到链接文件: {path}")

    links: list[str] = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            u = line.strip()
            if not u:
                continue
            links.append(u)

    # 再次去重，保留顺序
    return list(dict.fromkeys(links))


def extract_publish_time(soup: BeautifulSoup, text_fallback: str) -> str:
    pats = [
        r"(20\d{2}[-/.年]\d{1,2}[-/.月]\d{1,2}(?:日)?(?:\s*\d{1,2}:\d{1,2}(?::\d{1,2})?)?)",
    ]

    for sel in [
        "#pubtime_baidu",
        ".arti_metas",
        ".info",
        ".article-info",
        ".time",
        ".date",
        ".meta",
        ".wp_article_time",
    ]:
        n = soup.select_one(sel)
        if n:
            t = n.get_text(" ", strip=True)
            for p in pats:
                m = re.search(p, t)
                if m:
                    return m.group(1)

    for p in pats:
        m = re.search(p, text_fallback)
        if m:
            return m.group(1)
    return ""


def normalize_text(text: str) -> str:
    text = text.replace("\xa0", " ")
    text = re.sub(r"[ \t\r\f\v]+", " ", text)
    text = re.sub(r" *\n *", "\n", text)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def clean_title(title: str) -> str:
    title = normalize_text(title)
    title = re.sub(r"[-_—]+\s*官方内网\s*[-_—]+\s*桂林电子科技大学校内信息网\s*$", "", title)
    title = re.sub(r"\s*官方内网\s*[-_—]+\s*桂林电子科技大学校内信息网\s*$", "", title)
    return title.strip()


def strip_article_tail(text: str) -> str:
    tail_markers = [
        r"\n供\s*稿\s*[:：]",
        r"\n一审一校\s*[:：]",
        r"\n二审二校\s*[:：]",
        r"\n三审三校\s*[:：]",
        r"\n\[?打印新闻\]?",
        r"\n\[?关闭窗口\]?",
        r"\n上一篇\s*[:：]",
        r"\n下一篇\s*[:：]",
    ]
    end = len(text)
    for pat in tail_markers:
        m = re.search(pat, text)
        if m:
            end = min(end, m.start())
    return text[:end].strip()


def parse_detail(html: str) -> tuple[str, str, str]:
    soup = BeautifulSoup(html, "html.parser")

    # 只移除不会承载正文的标签。不要先删除 .top/.head/.content 这类泛选择器，
    # 学校模板里正文可能嵌在这些外层容器中，过早删除会导致只剩“官方内网”。
    for sel in ["script", "style", "noscript", "iframe", "svg", "canvas"]:
        for n in soup.select(sel):
            n.decompose()

    title = ""
    title_selectors = [
        ".newsTitle",
        "td.newsTitle",
        "h1",
        ".arti_title",
        ".article-title",
        ".news_title",
        ".wp_article_title",
        ".title",
    ]
    for sel in title_selectors:
        n = soup.select_one(sel)
        if n and n.get_text(strip=True):
            title = clean_title(n.get_text(" ", strip=True))
            if title and title != "官方内网":
                break
    if (not title or title == "官方内网") and soup.title and soup.title.get_text(strip=True):
        title = clean_title(soup.title.get_text(" ", strip=True))

    meta_text = soup.get_text("\n", strip=True)
    publish_time = extract_publish_time(soup, meta_text)
    if not publish_time:
        m_pub = re.search(r"发布时间\s*[:：]\s*([^\n]+)", meta_text)
        if m_pub:
            publish_time = m_pub.group(1).strip()

    # 正文优先锁定桂电站群详情页正文容器；桂电页面通常是 td.newsContent 内含 div.wp_articlecontent。
    content = ""
    article_nodes = []
    for sel in [
        ".wp_articlecontent .wp_articlecontent_text",
        ".wp_articlecontent_text",
        ".wp_articlecontent",
        "#wp_articlecontent",
        ".TRS_Editor",
        ".v_news_content",
        ".news_content",
        ".article_content",
        ".article-content",
    ]:
        article_nodes.extend(soup.select(sel))

    candidates: list[str] = []
    for node in article_nodes:
        t = normalize_text(node.get_text("\n", strip=True))
        t = strip_article_tail(t)
        if title and t.startswith(title):
            t = t[len(title):].strip()
        if len(t) >= 10 and t != "官方内网":
            candidates.append(t)

    if candidates:
        content = max(candidates, key=len)

    # 兜底：从全页文本中截取“发布时间”之后、“供稿/上一篇”等之前，避免导航区干扰。
    if not content:
        full_text = normalize_text((soup.body or soup).get_text("\n", strip=True))
        start = 0
        m_start = re.search(r"发布时间\s*[:：][^\n]*", full_text)
        if m_start:
            start = m_start.end()
        elif title:
            m_title = re.search(re.escape(title), full_text)
            if m_title:
                start = m_title.end()

        content = strip_article_tail(full_text[start:].strip())

    content = re.sub(r"\n?\s*\[?打印新闻\]?\s*", "\n", content)
    content = re.sub(r"\n?\s*\[?关闭窗口\]?\s*", "\n", content)
    content = normalize_text(content)

    return title, publish_time, content


def main() -> None:
    os.makedirs(RAW_DIR, exist_ok=True)
    rp = build_robot_parser()

    links = load_links(IN_LINKS)
    log(f"读取链接数: {len(links)}")

    docs = []
    failed = []

    for i, url in enumerate(links, start=1):
        log(f"[{i}/{len(links)}] {url}")

        if not allowed(rp, url):
            log("  - robots 禁止，跳过")
            continue

        try:
            html = fetch(url)
            title, publish_time, content = parse_detail(html)
            char_count = len(content)
            preview = content[:120].replace("\n", " ")
            log(f"  - 正文字符数: {char_count}")
            log(f"  - 正文预览: {preview}")

            if char_count < MIN_TEXT_LEN:
                log(f"  - 文本过短({char_count}), 跳过")
            else:
                docs.append(
                    {
                        "url": url,
                        "title": title,
                        "publish_time": publish_time,
                        "content": content,
                        "source": "guet_kuaixun",
                    }
                )
                log(f"  - OK title={title[:28]}... len={char_count}")
        except Exception as e:
            failed.append(url)
            log(f"  - 失败: {e}")

        polite_sleep()

    with open(OUT_DOCS, "w", encoding="utf-8") as f:
        for d in docs:
            f.write(json.dumps(d, ensure_ascii=False) + "\n")

    print("\n=== 完成 ===")
    print(f"输入链接数: {len(links)}")
    print(f"有效文档数: {len(docs)}")
    print(f"失败数: {len(failed)}")
    print(f"输出: {OUT_DOCS}")


if __name__ == "__main__":
    main()