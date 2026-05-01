import argparse
import hashlib
import json
from pathlib import Path
from typing import Dict, Iterable, List

import chromadb
import requests


def read_jsonl(path: Path) -> Iterable[Dict]:
    with path.open("r", encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            yield json.loads(line)


def chunk_text(text: str, chunk_size: int = 700, overlap: int = 120) -> List[str]:
    text = " ".join((text or "").split())
    if len(text) <= chunk_size:
        return [text] if text else []
    out = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        out.append(text[start:end])
        if end == len(text):
            break
        start = max(0, end - overlap)
    return out


def embed_with_ollama(text: str, model: str, ollama_url: str) -> List[float]:
    resp = requests.post(
        f"{ollama_url.rstrip('/')}/api/embeddings",
        json={"model": model, "prompt": text},
        timeout=120,
    )
    resp.raise_for_status()
    return resp.json()["embedding"]


def main():
    parser = argparse.ArgumentParser(description="Debug ingest for peoplehibl_zhengce_docs.jsonl")
    parser.add_argument("--base-dir", default=".")
    parser.add_argument("--jsonl", default="data/raw/peoplehibl_zhengce_docs.jsonl")
    parser.add_argument("--db-path", default="data/chroma")
    parser.add_argument("--collection", default="debug_peoplehibl_zhengce")
    parser.add_argument("--embed-model", default="qwen3-embedding:4b")
    parser.add_argument("--ollama-url", default="http://127.0.0.1:11434")
    parser.add_argument("--limit", type=int, default=5, help="最多处理多少条原始文档")
    parser.add_argument("--preview-len", type=int, default=120)
    args = parser.parse_args()

    base_dir = Path(args.base_dir).resolve()
    jsonl_path = (base_dir / args.jsonl).resolve()
    db_path = (base_dir / args.db_path).resolve()
    db_path.mkdir(parents=True, exist_ok=True)

    print(f"[INFO] jsonl: {jsonl_path}")
    print(f"[INFO] db   : {db_path}")

    client = chromadb.PersistentClient(path=str(db_path))
    collection = client.get_or_create_collection(name=args.collection, metadata={"hnsw:space": "cosine"})

    # 调试时先清空这个debug集合，避免历史数据干扰
    try:
        client.delete_collection(args.collection)
    except Exception:
        pass
    collection = client.get_or_create_collection(name=args.collection, metadata={"hnsw:space": "cosine"})

    count_doc = 0
    count_chunk = 0

    for row in read_jsonl(jsonl_path):
        if count_doc >= args.limit:
            break

        meta = row.get("metadata", {}) or {}
        title = row.get("title", "") or meta.get("title", "")
        source = row.get("source", "") or meta.get("source", "")
        url = row.get("url", "") or meta.get("url", "")
        publish_time = str(row.get("publish_time", "") or meta.get("publish_time", ""))
        content = row.get("content", "") or row.get("text", "")

        print("=" * 90)
        print(f"[DOC {count_doc + 1}] title={title}")
        print(f"source={source} | time={publish_time}")
        print(f"url={url}")
        print(f"raw preview: {(content or '')[:args.preview_len]}")

        chunks = chunk_text(content)
        for i, ch in enumerate(chunks):
            text = f"标题：{title}\n时间：{publish_time}\n来源：{source}\n正文：{ch}"
            doc_id = hashlib.md5(f"{source}|{url}|{i}|{ch[:32]}".encode("utf-8")).hexdigest()

            # 入库前打印
            print(f"  [CHUNK {i}] pre-ingest preview: {text[:args.preview_len].replace(chr(10), ' ')}")

            vec = embed_with_ollama(text, args.embed_model, args.ollama_url)
            collection.upsert(
                ids=[doc_id],
                documents=[text],
                metadatas=[{
                    "title": title,
                    "source": source,
                    "url": url,
                    "publish_time": publish_time,
                    "chunk_index": i,
                }],
                embeddings=[vec],
            )
            count_chunk += 1

        count_doc += 1

    print("\n[INFO] 写入完成，开始回读检查...\n")

    got = collection.get(limit=min(5, collection.count()), include=["documents", "metadatas"])
    docs = got.get("documents") or []
    metas = got.get("metadatas") or []

    for idx, (d, m) in enumerate(zip(docs, metas), 1):
        print("-" * 90)
        print(f"[READBACK {idx}] title={m.get('title','')} | source={m.get('source','')}")
        print(f"url={m.get('url','')}")
        print(f"doc preview: {(d or '')[:args.preview_len].replace(chr(10), ' ')}")

    print("\n[OK] done")
    print(f"[OK] collection={args.collection}, docs={count_doc}, chunks={count_chunk}, stored={collection.count()}")


if __name__ == "__main__":
    main()
