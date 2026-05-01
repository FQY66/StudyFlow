import json
import os
import random
import re
import time
from dataclasses import dataclass
from datetime import datetime
from urllib.parse import urljoin
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

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IN_LINKS = os.path.join(BASE_DIR, "..", "data", "raw", "peoplehibl_zhengce_unique_detail_links.txt")
OUT_RAW = os.path.join(BASE_DIR, "..", "data", "raw", "peoplehibl_zhengce_docs.jsonl")

SLEEP_SECONDS = float(os.getenv("PEOPLEHIBL_SLEEP", "3.0"))
JITTER_MIN = float(os.getenv("PEOPLEHIBL_JITTER_MIN", "1.0"))
JITTER_MAX = float(os.getenv("PEOPLEHIBL_JITTER_MAX", "2.5"))
REQUEST_TIMEOUT = int(os.getenv("PEOPLEHIBL_TIMEOUT", "20"))
MAX_RETRIES = int(os.getenv("PEOPLEHIBL_MAX_RETRIES", "2"))
MAX_REQUESTS = int(os.getenv("PEOPLEHIBL_MAX_REQUESTS", "3000"))
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


def read_detail_links(path: str) -> list[str]:
    if not os.path.exists(path):
        raise FileNotFoundError(f"链接文件不存在: {path}")

    links: list[str] = []
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            u = line.strip()
            if not u or u.startswith("#"):
                continue
            if not u.startswith("http"):
                continue
            links.append(u)

    uniq = list(dict.fromkeys(links))
    log(f"读取链接 {len(links)} 条，去重后 {len(uniq)} 条")
    return uniq


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
    log(f"启动详情爬虫: links_file={IN_LINKS}, max_requests={MAX_REQUESTS}")
    rp = build_robot_parser()
    request_count = 0

    detail_links = read_detail_links(IN_LINKS)
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
            char_count = len(content)
            preview = content[:120].replace("\n", " ")
            log(f"  - 正文字符数: {char_count}")
            log(f"  - 正文预览: {preview}")

            if not title:
                log(f"跳过: 标题为空 url={url}")
                polite_sleep()
                continue
            if char_count < MIN_TEXT_LEN:
                log(f"跳过: 正文过短 len={char_count} url={url}")
                polite_sleep()
                continue

            docs.append(Doc(url=url, title=title, publish_time=publish_time, content=content))
            log(f"入库成功: title={title[:28]}..., date={publish_time or 'N/A'}, len={char_count}")
            polite_sleep()
        except Exception as e:
            log(f"详情失败: {url} err={e}")
            continue

    log(f"开始写出文件: {OUT_RAW}")
    with open(OUT_RAW, "w", encoding="utf-8") as f:
        for d in docs:
            obj = {
                "url": d.url,
                "title": d.title,
                "publish_time": d.publish_time,
                "text": d.content,
                "metadata": {
                    "source": "peoplehibl_zhengce",
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
