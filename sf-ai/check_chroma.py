import argparse
import random

import chromadb


def main():
    parser = argparse.ArgumentParser(description="Check Chroma collection stats and sample docs")
    parser.add_argument("--db-path", default="data/chroma")
    parser.add_argument("--collection", default="studyflow_knowledge")
    parser.add_argument("--sample", type=int, default=3, help="how many random docs to print")
    parser.add_argument("--preview-len", type=int, default=180, help="preview length of each doc")
    args = parser.parse_args()

    client = chromadb.PersistentClient(path=args.db_path)
    collection = client.get_collection(name=args.collection)

    total = collection.count()
    print(f"[INFO] collection={args.collection}")
    print(f"[INFO] total vectors={total}")

    if total == 0:
        print("[WARN] 当前集合为空。")
        return

    sample_n = min(args.sample, total)
    # 用 offset 随机抽样（避免一次性拉全量）
    offsets = random.sample(range(total), sample_n)

    for i, off in enumerate(offsets, 1):
        got = collection.get(limit=1, offset=off, include=["documents", "metadatas"])
        doc = (got.get("documents") or [""])[0] or ""
        meta = (got.get("metadatas") or [{}])[0] or {}

        title = meta.get("title", "")
        source = meta.get("source", "")
        url = meta.get("url", "")
        preview = doc[: args.preview_len].replace("\n", " ")

        print("-" * 80)
        print(f"[SAMPLE {i}] offset={off}")
        print(f"title : {title}")
        print(f"source: {source}")
        print(f"url   : {url}")
        print(f"text  : {preview}")


if __name__ == "__main__":
    main()
