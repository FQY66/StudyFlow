import argparse
import hashlib
import json
from pathlib import Path
from typing import Dict, Iterable, List

import chromadb
import requests
from tqdm import tqdm

DEFAULT_FILES = [
    "data/raw/guet_kuaixun_docs.jsonl",
    "data/raw/peoplehibl_docs.jsonl",
    "data/raw/peoplehibl_zhengce_docs.jsonl",
]


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
    chunks = []
    start = 0
    while start < len(text):
        end = min(start + chunk_size, len(text))
        chunks.append(text[start:end])
        if end == len(text):
            break
        start = max(0, end - overlap)
    return chunks


def embed_with_ollama(texts: List[str], model: str, ollama_url: str) -> List[List[float]]:
    vectors = []
    for t in texts:
        resp = requests.post(
            f"{ollama_url.rstrip('/')}/api/embeddings",
            json={"model": model, "prompt": t},
            timeout=120,
        )
        resp.raise_for_status()
        data = resp.json()
        vectors.append(data["embedding"])
    return vectors


def main():
    parser = argparse.ArgumentParser(description="Build Chroma index from JSONL docs with Ollama embeddings")
    parser.add_argument("--base-dir", default=".", help="sf-ai directory path")
    parser.add_argument("--files", nargs="*", default=DEFAULT_FILES)
    parser.add_argument("--db-path", default="data/chroma")
    parser.add_argument("--collection", default="studyflow_knowledge")
    parser.add_argument("--embed-model", default="qwen3-embedding:4b")
    parser.add_argument("--ollama-url", default="http://127.0.0.1:11434")
    parser.add_argument("--batch-size", type=int, default=16)
    parser.add_argument("--verbose", action="store_true", help="打印更多调试日志")
    parser.add_argument("--preview-len", type=int, default=160, help="日志预览字符长度")
    parser.add_argument("--show-every", type=int, default=200, help="每处理多少条记录打印一次进度日志")
    parser.add_argument("--show-batch", action="store_true", help="打印每个向量批次的详细日志")
    args = parser.parse_args()

    base_dir = Path(args.base_dir).resolve()
    db_path = (base_dir / args.db_path).resolve()
    db_path.mkdir(parents=True, exist_ok=True)

    print("=" * 90)
    print("[START] 开始构建 Chroma 索引")
    print(f"[CFG] base_dir   = {base_dir}")
    print(f"[CFG] db_path    = {db_path}")
    print(f"[CFG] collection = {args.collection}")
    print(f"[CFG] embed_model= {args.embed_model}")
    print(f"[CFG] ollama_url = {args.ollama_url}")
    print(f"[CFG] batch_size = {args.batch_size}")
    print(f"[CFG] show_every = {args.show_every}")
    print(f"[CFG] files      = {args.files}")
    print("=" * 90)

    client = chromadb.PersistentClient(path=str(db_path))
    collection = client.get_or_create_collection(name=args.collection, metadata={"hnsw:space": "cosine"})
    before_count = collection.count()
    print(f"[INFO] collection 当前向量数: {before_count}")

    ids, docs, metas = [], [], []
    file_stats = {}

    for rel in args.files:
        fpath = (base_dir / rel).resolve()
        print("-" * 90)
        print(f"[FILE] 配置路径: {rel}")
        print(f"[FILE] 解析绝对路径: {fpath}")
        if not fpath.exists():
            print(f"[WARN] 文件不存在，跳过: {fpath}")
            continue

        stat = {"rows": 0, "rows_empty_text": 0, "chunks": 0}
        print(f"[INFO] 开始读取: {fpath}")
        for row in read_jsonl(fpath):
            stat["rows"] += 1
            if args.show_every > 0 and stat["rows"] % args.show_every == 0:
                print(
                    f"[PROGRESS] file={fpath.name} rows={stat['rows']} chunks={stat['chunks']} empty_text={stat['rows_empty_text']}"
                )
            meta = row.get("metadata", {}) or {}
            source = row.get("source", "") or meta.get("source", fpath.stem)
            url = row.get("url", "") or meta.get("url", "")
            title = row.get("title", "") or meta.get("title", "")
            publish_time = row.get("publish_time", "") or meta.get("publish_time", "")
            content = row.get("content", "") or row.get("text", "")

            chunks = chunk_text(content)
            if not chunks:
                stat["rows_empty_text"] += 1
                if args.verbose:
                    print(
                        f"[SKIP] 空正文 | title={title[:60]} | url={url}"
                    )
                continue

            if args.verbose:
                raw_preview = (content or "")[: args.preview_len].replace("\n", " ")
                print("=" * 90)
                print(f"[ROW] title={title}")
                print(f"[ROW] source={source} | publish_time={publish_time} | url={url}")
                print(f"[ROW] raw preview: {raw_preview}")
                print(f"[ROW] chunks={len(chunks)}")

            for i, chunk in enumerate(chunks):
                if not chunk:
                    continue
                raw_id = f"{source}|{url}|{i}|{chunk[:32]}"
                doc_id = hashlib.md5(raw_id.encode("utf-8")).hexdigest()
                ids.append(doc_id)
                assembled = f"标题：{title}\n时间：{publish_time}\n来源：{source}\n正文：{chunk}"
                docs.append(assembled)
                metas.append({
                    "source": source,
                    "url": url,
                    "title": title,
                    "publish_time": str(publish_time),
                    "chunk_index": i,
                })
                if args.verbose and i < 2:
                    chunk_preview = assembled[: args.preview_len].replace("\n", " ")
                    print(f"  [CHUNK {i}] preview: {chunk_preview}")
                stat["chunks"] += 1

        print(
            f"[FILE DONE] {fpath.name} | rows={stat['rows']} | empty_text_rows={stat['rows_empty_text']} | chunks={stat['chunks']}"
        )
        file_stats[str(fpath)] = stat

    print("\n[INFO] 各文件解析统计：")
    for f, s in file_stats.items():
        print(
            f"  - {f} | rows={s['rows']} | empty_text_rows={s['rows_empty_text']} | chunks={s['chunks']}"
        )

    print(f"\n[INFO] 准备写入向量，共 {len(docs)} chunks")
    if not docs:
        print("[INFO] 没有可写入内容，结束")
        return

    total_batches = (len(docs) + args.batch_size - 1) // args.batch_size
    for batch_idx, i in enumerate(tqdm(range(0, len(docs), args.batch_size), desc="Embedding+Upsert"), start=1):
        b_ids = ids[i : i + args.batch_size]
        b_docs = docs[i : i + args.batch_size]
        b_meta = metas[i : i + args.batch_size]

        if args.show_batch:
            first_meta = (b_meta[0] if b_meta else {}) or {}
            print(
                f"[BATCH {batch_idx}/{total_batches}] size={len(b_docs)} "
                f"first_source={first_meta.get('source','')} first_title={first_meta.get('title','')[:40]}"
            )

        vectors = embed_with_ollama(b_docs, args.embed_model, args.ollama_url)
        collection.upsert(ids=b_ids, documents=b_docs, metadatas=b_meta, embeddings=vectors)

        if args.show_batch:
            print(f"[BATCH {batch_idx}/{total_batches}] upsert done")

    after_count = collection.count()
    print("=" * 90)
    print(f"[OK] 完成。collection={args.collection}")
    print(f"[INFO] 写入前向量数: {before_count}")
    print(f"[INFO] 当前向量总数: {after_count}")
    print(f"[INFO] 本次新增(估算): {after_count - before_count}")
    print("=" * 90)


if __name__ == "__main__":
    main()
