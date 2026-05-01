import json
import os
import random
import re
import time
from datetime import datetime
from dataclasses import dataclass
from typing import Iterable
from urllib.parse import parse_qs, urljoin, urlparse
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

SEED_LIST_URLS = [
    "http://www.chinayanxue.org.cn/index/articles/lists.html?id=20",
    "http://www.chinayanxue.org.cn/index/articles/lists.html?id=18",
    "http://www.tiyan.org.cn/yanxuezixun.html",
]


BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_RAW_DIR = os.getenv("YANXUE_OUT_RAW_DIR", os.path.join(BASE_DIR, "data", "raw"))
OUT_CHUNK_DIR = os.getenv("YANXUE_OUT_CHUNK_DIR", os.path.join(BASE_DIR, "data", "chunks"))

MAX_LIST_PAGES = int(os.getenv("YANXUE_MAX_PAGES", "3"))
SLEEP_SECONDS = float(os.getenv("YANXUE_SLEEP", "2.5"))
REQUEST_TIMEOUT = int(os.getenv("YANXUE_TIMEOUT", "20"))
JITTER_MIN = float(os.getenv("YANXUE_JITTER_MIN", "0.8"))
JITTER_MAX = float(os.getenv("YANXUE_JITTER_MAX", "2.2"))
MAX_RETRIES = int(os.getenv("YANXUE_MAX_RETRIES", "3"))
BACKOFF_BASE = float(os.getenv("YANXUE_BACKOFF_BASE", "2.0"))
MAX_REQUESTS_PER_SITE = int(os.getenv("YANXUE_MAX_REQUESTS_PER_SITE", "120"))
MAX_CONSECUTIVE_ERRORS = int(os.getenv("YANXUE_MAX_CONSECUTIVE_ERRORS", "8"))
RESPECT_ROBOTS = os.getenv("YANXUE_RESPECT_ROBOTS", "1") == "1"
ROBOTS_USER_AGENT = os.getenv("YANXUE_ROBOTS_UA", "*")

MIN_CONTENT_LEN = int(os.getenv("YANXUE_MIN_CONTENT_LEN", "120"))
CHUNK_SIZE = int(os.getenv("YANXUE_CHUNK_SIZE", "700"))
CHUNK_OVERLAP = int(os.getenv("YANXUE_CHUNK_OVERLAP", "120"))
VERBOSE = os.getenv("YANXUE_VERBOSE", "1") == "1"


def log(msg: str) -> None:
    if VERBOSE:
        print(f"[{datetime.now().strftime('%H:%M:%S')}] {msg}")


@dataclass
class Doc:
    doc_id: str
    site: str
    url: str
    title: str
    publish_time: str
    text: str


def build_robot_parser(site: str) -> RobotFileParser | None:
    if not RESPECT_ROBOTS:
        return None
    robots_url = urljoin(site, "/robots.txt")
    rp = RobotFileParser()
    rp.set_url(robots_url)
    try:
        rp.read()
        return rp
    except Exception:
        return None


def is_allowed_by_robots(rp: RobotFileParser | None, url: str) -> bool:
    if rp is None:
        return True
    try:
        return rp.can_fetch(ROBOTS_USER_AGENT, url)
    except Exception:
        return True


def polite_sleep() -> None:
    delay = SLEEP_SECONDS + random.uniform(JITTER_MIN, JITTER_MAX)
    delay = max(delay, 0.1)
    log(f"sleep {delay:.2f}s")
    time.sleep(delay)


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
            log(f"FAIL {url} err={e}")
            if attempt < MAX_RETRIES:
                backoff = BACKOFF_BASE ** (attempt - 1) + random.uniform(0.3, 1.2)
                log(f"backoff {backoff:.2f}s")
                time.sleep(backoff)
    raise RuntimeError(f"请求失败且重试耗尽: {url}, err={last_err}")


def clean_text(text: str) -> str:
    text = text.replace("\u3000", " ")
    text = re.sub(r"[ \t]+", " ", text)
    text = re.sub(r"\r\n?", "\n", text)

    # 按行过滤典型噪声（页脚、导航、按钮）
    blocked_exact = {
        "首页", "首 页", "关于我们", "联系我们", "网站介绍", "版权声明", "供稿服务", "广告刊例",
        "打印本页", "关闭窗口", "返回顶部", "上一篇", "下一篇", "来源：", "作者：", "选项", ">",
    }
    blocked_keywords = [
        "copyright", "版权所有", "京icp", "工信部备案", "备案号", "中国研学旅行在线",
        "综合新闻", "各地政策", "活动课程", "研学基地", "地方动态", "专家观点", "研学导师",
        "联系我们", "网站介绍", "供稿服务", "广告刊例",
    ]

    cleaned_lines: list[str] = []
    for raw in text.split("\n"):
        line = raw.strip(" |｜\t")
        if not line:
            continue
        if line in blocked_exact:
            continue
        low = line.lower()
        if any(k in low for k in blocked_keywords):
            continue
        # 纯分隔符行
        if re.fullmatch(r"[-_=|｜·•>]+", line):
            continue
        cleaned_lines.append(line)

    text = "\n".join(cleaned_lines)
    text = re.sub(r"\n{3,}", "\n\n", text)
    return text.strip()


def is_article_detail_url(url: str) -> bool:
    p = urlparse(url)
    path = p.path.lower()

    # chinayanxue: /index/articles/articles.html?id=xxx 是详情
    if path.endswith("/index/articles/articles.html"):
        return True
    if path.endswith("/index/articles/lists.html"):
        return False

    # tiyan: article-9-1389.html / article-9-1404.html 这类详情
    if re.search(r"/article-\d+-\d+\.html$", path):
        return True

    return False


def extract_links(html: str, base_url: str) -> list[str]:
    soup = BeautifulSoup(html, "html.parser")
    links: list[str] = []
    for a in soup.select("a[href]"):
        href = (a.get("href") or "").strip()
        if not href or href.startswith("javascript"):
            continue
        full = urljoin(base_url, href)
        if not full.startswith(base_url):
            continue
        if any(k in full.lower() for k in [".jpg", ".png", ".gif", ".pdf", "#"]):
            continue
        if is_article_detail_url(full):
            links.append(full)
    return list(dict.fromkeys(links))


def extract_publish_time(soup: BeautifulSoup, raw_text: str) -> str:
    time_patterns = [
        r"(20\d{2}[-/.]0?\d[-/.]0?\d)",
        r"(20\d{2}年\d{1,2}月\d{1,2}日)",
    ]

    # 优先从明显字段中找
    for sel in [".time", ".date", ".publish-time", ".article-info", ".info"]:
        node = soup.select_one(sel)
        if node:
            t = node.get_text(" ", strip=True)
            for p in time_patterns:
                m = re.search(p, t)
                if m:
                    return m.group(1)

    # 全文兜底
    for p in time_patterns:
        m = re.search(p, raw_text)
        if m:
            return m.group(1)
    return ""


def parse_detail(url: str, html: str) -> tuple[str, str, str]:
    soup = BeautifulSoup(html, "html.parser")

    # 先删掉明显噪声区块
    for sel in ["header", "footer", "nav", "aside", ".footer", ".nav", ".menu", ".sidebar"]:
        for node in soup.select(sel):
            node.decompose()

    title = ""
    h1 = soup.find("h1")
    if h1 and h1.get_text(strip=True):
        title = h1.get_text(strip=True)
    elif soup.title and soup.title.get_text(strip=True):
        title = soup.title.get_text(strip=True)

    # 删除图片节点，确保正文纯文本
    for img in soup.find_all("img"):
        img.decompose()

    selectors = [
        "article",
        ".article",
        ".article-content",
        ".content",
        ".post-content",
        ".detail-content",
        ".con",
        "#content",
        ".show_text",
        ".news-content",
    ]

    best_text = ""
    best_score = -1
    for sel in selectors:
        node = soup.select_one(sel)
        if not node:
            continue
        candidate = node.get_text("\n", strip=True)
        candidate = clean_text(candidate)
        if not candidate:
            continue
        # 评分：长度 + 中文字符占比，尽量避开菜单区
        zh_count = len(re.findall(r"[\u4e00-\u9fff]", candidate))
        score = len(candidate) + zh_count * 2
        if score > best_score:
            best_score = score
            best_text = candidate

    if len(best_text) < MIN_CONTENT_LEN:
        fallback = soup.get_text("\n", strip=True)
        best_text = clean_text(fallback)

    # 若正文开头误包含标题/发布时间/作者/来源等头部信息，做轻量裁剪
    lines = [ln.strip() for ln in best_text.split("\n") if ln.strip()]
    trimmed: list[str] = []
    for ln in lines:
        if re.match(r"^(发布时间|作者|来源)\s*[：:]", ln):
            continue
        if ln in {"选项", ">"}:
            continue
        trimmed.append(ln)
    best_text = "\n".join(trimmed).strip()

    publish_time = extract_publish_time(soup, best_text)
    return title.strip(), publish_time.strip(), best_text.strip()


def iter_list_urls(seed_url: str) -> Iterable[str]:
    # 仅对带查询参数的列表页翻页，避免 ziran.html?page=7 这类非目标列表
    yield seed_url
    parsed = urlparse(seed_url)
    if not parsed.query:
        return
    for p in range(2, MAX_LIST_PAGES + 1):
        sep = "&" if "?" in seed_url else "?"
        yield f"{seed_url}{sep}page={p}"


def split_text(text: str, chunk_size: int, overlap: int) -> list[str]:
    chunks: list[str] = []
    start = 0
    n = len(text)
    while start < n:
        end = min(start + chunk_size, n)
        chunk = text[start:end].strip()
        if chunk:
            chunks.append(chunk)
        if end >= n:
            break
        start = max(end - overlap, start + 1)
    return chunks


def crawl_sites() -> list[Doc]:
    docs: list[Doc] = []
    seen_urls: set[str] = set()
    hosts = sorted({urlparse(u).scheme + "://" + urlparse(u).netloc + "/" for u in SEED_LIST_URLS})

    for site in hosts:
        host = urlparse(site).netloc
        print(f"\n=== 开始抓取站点: {host} ===")
        log(f"seeds={len([u for u in SEED_LIST_URLS if urlparse(u).netloc == host])}, max_pages={MAX_LIST_PAGES}")
        rp = build_robot_parser(site)
        request_count = 0
        consecutive_errors = 0

        seeds = [u for u in SEED_LIST_URLS if urlparse(u).netloc == host]
        candidate_links: list[str] = []

        for seed_url in seeds:
            for list_url in iter_list_urls(seed_url):
                if request_count >= MAX_REQUESTS_PER_SITE:
                    log(f"[{host}] 达到请求上限，停止列表抓取")
                    break
                if not is_allowed_by_robots(rp, list_url):
                    log(f"[robots跳过列表] {list_url}")
                    continue
                try:
                    html = fetch(list_url)
                    request_count += 1
                    # 只提取详情页链接，列表内容不保存
                    found_links = extract_links(html, site)
                    candidate_links.extend(found_links)
                    log(f"列表完成: {list_url} 提取详情 {len(found_links)}，累计候选 {len(candidate_links)}")
                    consecutive_errors = 0
                    polite_sleep()
                except Exception as e:
                    consecutive_errors += 1
                    log(f"列表失败: {list_url} err={e} consecutive_errors={consecutive_errors}")
                    if consecutive_errors >= MAX_CONSECUTIVE_ERRORS:
                        log(f"[{host}] 连续失败过多，停止列表抓取")
                        break
            if request_count >= MAX_REQUESTS_PER_SITE or consecutive_errors >= MAX_CONSECUTIVE_ERRORS:
                break

        candidate_links = list(dict.fromkeys(candidate_links))
        log(f"[{host}] 去重后候选详情链接: {len(candidate_links)}")

        for idx, link in enumerate(candidate_links, start=1):
            if request_count >= MAX_REQUESTS_PER_SITE or consecutive_errors >= MAX_CONSECUTIVE_ERRORS:
                log(f"[{host}] 提前停止详情抓取 request_count={request_count} errors={consecutive_errors}")
                break
            if link in seen_urls:
                log(f"[{host}] 跳过重复链接 {link}")
                continue
            if not is_allowed_by_robots(rp, link):
                log(f"[robots跳过详情] {link}")
                continue
            log(f"[{host}] 详情进度 {idx}/{len(candidate_links)} -> {link}")
            seen_urls.add(link)

            try:
                html = fetch(link)
                request_count += 1
                title, publish_time, body = parse_detail(link, html)
                if len(body) < MIN_CONTENT_LEN:
                    log(f"[{host}] 正文过短跳过 len={len(body)} url={link}")
                    polite_sleep()
                    continue
                docs.append(Doc(
                    doc_id=f"{host}-{len(docs) + 1}",
                    site=host,
                    url=link,
                    title=title,
                    publish_time=publish_time,
                    text=body,
                ))
                log(f"[{host}] 入库成功 title={title[:30]}..., date={publish_time or 'N/A'}")
                consecutive_errors = 0
                polite_sleep()
            except Exception as e:
                consecutive_errors += 1
                log(f"[{host}] 详情失败 url={link} err={e} consecutive_errors={consecutive_errors}")

    return docs


def save_outputs(docs: list[Doc]) -> None:
    os.makedirs(OUT_RAW_DIR, exist_ok=True)
    os.makedirs(OUT_CHUNK_DIR, exist_ok=True)

    docs_jsonl = os.path.join(OUT_RAW_DIR, "yanxue_docs.jsonl")
    chunk_txt = os.path.join(OUT_CHUNK_DIR, "embedding_chunks.txt")
    chunk_jsonl = os.path.join(OUT_CHUNK_DIR, "embedding_chunks.jsonl")

    chunk_count = 0
    log(f"开始写入输出文件: docs={len(docs)}")
    with open(docs_jsonl, "w", encoding="utf-8") as f_docs, open(chunk_txt, "w", encoding="utf-8") as f_txt, open(chunk_jsonl, "w", encoding="utf-8") as f_chunk:
        for doc_idx, doc in enumerate(docs, start=1):
            doc_obj = {
                "id": doc.doc_id,
                "title": doc.title,
                "publish_time": doc.publish_time,
                "text": doc.text,
                "metadata": {
                    "site": doc.site,
                    "url": doc.url,
                    "title": doc.title,
                    "publish_time": doc.publish_time,
                },
            }
            f_docs.write(json.dumps(doc_obj, ensure_ascii=False) + "\n")

            merged_for_embed = f"标题：{doc.title}\n发布时间：{doc.publish_time}\n正文：{doc.text}".strip()
            parts = split_text(merged_for_embed, CHUNK_SIZE, CHUNK_OVERLAP)
            log(f"写入文档 {doc_idx}/{len(docs)} -> chunks={len(parts)}")
            for i, part in enumerate(parts):
                chunk_id = f"{doc.doc_id}-c{i + 1}"
                chunk_meta = {
                    "site": doc.site,
                    "url": doc.url,
                    "title": doc.title,
                    "publish_time": doc.publish_time,
                    "doc_id": doc.doc_id,
                    "chunk_index": i,
                }
                f_txt.write(part + "\n")
                f_chunk.write(json.dumps({"id": chunk_id, "text": part, "metadata": chunk_meta}, ensure_ascii=False) + "\n")
                chunk_count += 1

    print(f"✅ 完成：文档 {len(docs)}，chunks {chunk_count}")
    print(f"- {docs_jsonl}")
    print(f"- {chunk_txt}")
    print(f"- {chunk_jsonl}")


def main() -> None:
    docs = crawl_sites()
    save_outputs(docs)


if __name__ == "__main__":
    main()
