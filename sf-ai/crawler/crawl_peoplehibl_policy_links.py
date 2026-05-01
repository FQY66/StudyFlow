import os
import random
import re
import time
from urllib.parse import urljoin, urlparse
from urllib.robotparser import RobotFileParser

import requests
from bs4 import BeautifulSoup

BASE_URL = "https://www.peoplehibl.com/"
LIST_BASE = "https://www.peoplehibl.com/html/zhengce/"
TOTAL_PAGES = int(os.getenv("PEOPLEHIBL_ZHENGCE_TOTAL_PAGES", "2"))

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(BASE_DIR, "data", "raw")
OUT_ALL_LINKS = os.path.join(OUT_DIR, "peoplehibl_zhengce_all_detail_links.txt")
OUT_UNIQUE_LINKS = os.path.join(OUT_DIR, "peoplehibl_zhengce_unique_detail_links.txt")

# 低强度访问
SLEEP_SECONDS = float(os.getenv("PEOPLEHIBL_SLEEP", "2.5"))
JITTER_MIN = float(os.getenv("PEOPLEHIBL_JITTER_MIN", "0.8"))
JITTER_MAX = float(os.getenv("PEOPLEHIBL_JITTER_MAX", "1.8"))
TIMEOUT = int(os.getenv("PEOPLEHIBL_TIMEOUT", "20"))
MAX_RETRIES = int(os.getenv("PEOPLEHIBL_MAX_RETRIES", "2"))
RESPECT_ROBOTS = os.getenv("PEOPLEHIBL_RESPECT_ROBOTS", "1") == "1"
ROBOTS_USER_AGENT = os.getenv("PEOPLEHIBL_ROBOTS_UA", "*")

HEADERS = {
    "User-Agent": (
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
        "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
    ),
    "Accept-Language": "zh-CN,zh;q=0.9,en;q=0.8",
}


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
    for _ in range(MAX_RETRIES):
        try:
            r = requests.get(url, headers=HEADERS, timeout=TIMEOUT)
            r.raise_for_status()
            r.encoding = r.apparent_encoding
            return r.text
        except Exception as e:
            last_err = e
            time.sleep(1.2 + random.uniform(0.2, 0.8))
    raise RuntimeError(f"请求失败: {url}, err={last_err}")


def list_page_url(page: int) -> str:
    # 政策分页路径（与资讯一致）：
    # 第1页: /html/zhengce/
    # 第2页: /html/zhengce/2.html
    # 第N页: /html/zhengce/N.html
    if page <= 1:
        return LIST_BASE
    return urljoin(LIST_BASE, f"{page}.html")


def is_detail_url(url: str) -> bool:
    # 政策详情示例:
    # https://www.peoplehibl.com/html/2025/zhengce_0928/1405.html
    # https://www.peoplehibl.com/html/2025/zhengce_1113/1506.html
    path = urlparse(url).path.lower()
    return re.search(r"^/html/20\d{2}/zhengce_\d{4}/\d+\.html$", path) is not None


def extract_detail_links(html: str) -> list[str]:
    soup = BeautifulSoup(html, "html.parser")

    # 先移除明显噪声区块
    for sel in [
        "footer", ".footer", "#footer",
        ".hot-news", ".hot", ".remen", ".right", ".sidebar", ".aside",
        ".weixin", ".wx", ".gzh", ".qrcode", ".code",
        "nav", ".nav", ".menu", "header",
    ]:
        for n in soup.select(sel):
            n.decompose()

    noise_text_patterns = [
        r"热门视频", r"热门资讯", r"微信公众号", r"相关阅读", r"相关推荐",
        r"版权", r"京ICP备", r"工信部备案", r"关于我们", r"联系我们",
        r"人民研学网简介", r"战略合作", r"互联网新闻信息服务许可证",
    ]

    out: list[str] = []
    for a in soup.select("a[href]"):
        href = (a.get("href") or "").strip()
        if not href or href.startswith("javascript"):
            continue

        anchor_text = re.sub(r"\s+", " ", a.get_text(" ", strip=True)).strip()
        if anchor_text and any(re.search(p, anchor_text, flags=re.IGNORECASE) for p in noise_text_patterns):
            continue

        full = urljoin(BASE_URL, href)
        if not full.startswith(BASE_URL):
            continue
        if any(k in full.lower() for k in [".jpg", ".png", ".gif", ".pdf", "#"]):
            continue

        if is_detail_url(full):
            out.append(full)

    return out


def main() -> None:
    os.makedirs(OUT_DIR, exist_ok=True)
    rp = build_robot_parser()

    all_links: list[str] = []
    blocked_pages: list[str] = []
    failed_pages: list[str] = []

    for p in range(1, TOTAL_PAGES + 1):
        url = list_page_url(p)
        print(f"[{p}/{TOTAL_PAGES}] 列表页: {url}")

        if not allowed(rp, url):
            blocked_pages.append(url)
            print("  - robots 禁止，跳过")
            continue

        try:
            html = fetch(url)
            links = extract_detail_links(html)
            all_links.extend(links)
            print(f"  - 提取到 {len(links)} 条政策详情链接，累计 {len(all_links)}")
        except Exception as e:
            failed_pages.append(url)
            print(f"  - 抓取失败: {e}")

        polite_sleep()

    unique_links = list(dict.fromkeys(all_links))

    with open(OUT_ALL_LINKS, "w", encoding="utf-8") as f:
        for u in all_links:
            f.write(u + "\n")

    with open(OUT_UNIQUE_LINKS, "w", encoding="utf-8") as f:
        for u in unique_links:
            f.write(u + "\n")

    print("\n=== 完成 ===")
    print(f"列表页总数: {TOTAL_PAGES}")
    print(f"政策详情链接(含重复): {len(all_links)}")
    print(f"政策详情链接(去重后): {len(unique_links)}")
    print(f"robots 跳过页: {len(blocked_pages)}")
    print(f"抓取失败页: {len(failed_pages)}")
    print(f"输出: {OUT_ALL_LINKS}")
    print(f"输出: {OUT_UNIQUE_LINKS}")


if __name__ == "__main__":
    main()
