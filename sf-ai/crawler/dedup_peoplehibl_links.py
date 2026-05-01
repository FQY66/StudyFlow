import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
IN_FILE = os.path.join(BASE_DIR, "data", "raw", "peoplehibl_all_detail_links.txt")
OUT_FILE = os.path.join(BASE_DIR, "data", "raw", "peoplehibl_unique_detail_links.txt")


def dedup_links(lines: list[str]) -> list[str]:
    """保持原顺序去重，忽略空行。"""
    seen = set()
    unique = []
    for line in lines:
        url = line.strip()
        if not url:
            continue
        if url not in seen:
            seen.add(url)
            unique.append(url)
    return unique


def main() -> None:
    if not os.path.exists(IN_FILE):
        raise FileNotFoundError(f"输入文件不存在: {IN_FILE}")

    with open(IN_FILE, "r", encoding="utf-8") as f:
        lines = f.readlines()

    unique_links = dedup_links(lines)

    os.makedirs(os.path.dirname(OUT_FILE), exist_ok=True)
    with open(OUT_FILE, "w", encoding="utf-8") as f:
        for url in unique_links:
            f.write(url + "\n")

    print("=== 去重完成 ===")
    print(f"输入文件: {IN_FILE}")
    print(f"输出文件: {OUT_FILE}")
    print(f"原始行数(含空行): {len(lines)}")
    print(f"去重后链接数: {len(unique_links)}")


if __name__ == "__main__":
    main()
