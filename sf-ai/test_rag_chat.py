import argparse
import json
from pathlib import Path

import requests


def main():
    parser = argparse.ArgumentParser(description="Test StudyFlow RAG chat API and save UTF-8 result")
    parser.add_argument("--url", default="http://127.0.0.1:18001/rag/chat", help="RAG chat endpoint")
    parser.add_argument("--query", default="【民生周报】第五期说了什么")
    parser.add_argument("--top-k", type=int, default=5)
    parser.add_argument("--out", default="rag_result.json", help="save response as UTF-8 JSON")
    args = parser.parse_args()

    payload = {"query": args.query, "top_k": 3}

    print(f"[INFO] POST {args.url}")
    print(f"[INFO] payload: {payload}")

    resp = requests.post(args.url, json=payload, timeout=180)
    resp.raise_for_status()

    data = resp.json()

    # 终端打印（尽量保证中文可读）
    print("\n" + "=" * 90)
    print("[ANSWER]")
    print(data.get("answer", ""))

    refs = data.get("references", []) or []
    print("\n" + "=" * 90)
    print(f"[REFERENCES] total={len(refs)}")
    for i, r in enumerate(refs, 1):
        print("-" * 90)
        print(f"[{i}] title   : {r.get('title', '')}")
        print(f"    source  : {r.get('source', '')}")
        print(f"    url     : {r.get('url', '')}")
        print(f"    distance: {r.get('distance', '')}")

    # 文件保存（强制UTF-8，无转义）
    out_path = Path(args.out).resolve()
    out_path.write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding="utf-8")
    print("\n" + "=" * 90)
    print(f"[OK] 已写入: {out_path}")
    print("[TIP] 如果终端仍乱码，请直接打开该 JSON 文件查看（文件内是 UTF-8 中文）")


if __name__ == "__main__":
    main()
