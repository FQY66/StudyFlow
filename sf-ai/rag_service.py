import hashlib
import re
from pathlib import Path
from typing import Dict, Generator, List

import chromadb
import requests


class RagService:
    def __init__(
        self,
        db_path: str = "data/chroma",
        collection: str = "studyflow_knowledge",
        ollama_url: str = "http://127.0.0.1:11434",
        embed_model: str = "qwen3-embedding:4b",
        chat_model: str = "qwen3:8b",
    ):
        self.ollama_url = ollama_url.rstrip("/")
        self.embed_model = embed_model
        self.chat_model = chat_model

        # 将相对路径锚定到当前文件所在目录，避免启动目录不同导致连错库
        resolved_db_path = Path(db_path)
        if not resolved_db_path.is_absolute():
            resolved_db_path = (Path(__file__).resolve().parent / resolved_db_path).resolve()

        self.client = chromadb.PersistentClient(path=str(resolved_db_path))
        self.collection = self.client.get_or_create_collection(
            name=collection, metadata={"hnsw:space": "cosine"}
        )

    def embed(self, text: str) -> List[float]:
        resp = requests.post(
            f"{self.ollama_url}/api/embeddings",
            json={"model": self.embed_model, "prompt": text},
            timeout=120,
        )
        resp.raise_for_status()
        return resp.json()["embedding"]

    def retrieve(self, query: str, top_k: int = 5) -> List[Dict]:
        vec = self.embed(query)
        result = self.collection.query(
            query_embeddings=[vec],
            n_results=top_k,
            include=["documents", "metadatas", "distances"],
        )
        docs = result.get("documents", [[]])[0]
        metas = result.get("metadatas", [[]])[0]
        dists = result.get("distances", [[]])[0]

        hits = []
        for d, m, s in zip(docs, metas, dists):
            hits.append({"text": d, "meta": m, "distance": s})
        return hits

    def build_prompt(self, query: str, hits: List[Dict]) -> str:
        context_blocks = []
        for i, h in enumerate(hits, 1):
            meta = h.get("meta", {}) or {}
            context_blocks.append(
                f"[知识{i}] 标题:{meta.get('title','')} 来源:{meta.get('source','')} 时间:{meta.get('publish_time','')}\n"
                f"{h.get('text','')}"
            )

        context = "\n\n".join(context_blocks) if context_blocks else "（未检索到相关知识）"

        return (
            "你是StudyFlow智能助手。请优先依据提供的知识回答；若知识不足，明确说明并给出通用建议。"
            "\n\n"
            "回答格式要求："
            "\n1) 只输出纯文本中文，不要使用Markdown。"
            "\n2) 不要使用 **、##、```、- 这类格式符号。"
            "\n3) 使用阿拉伯数字分点，格式为(1)(2)(3)，每个分点单独成行。"
            "\n\n"
            f"【检索知识】\n{context}\n\n"
            f"【用户问题】\n{query}\n\n"
            "请输出简洁、准确、可执行的中文回答。"
        )

    def _build_references(self, hits: List[Dict]) -> List[Dict]:
        return [
            {
                "title": (h.get("meta") or {}).get("title", ""),
                "url": (h.get("meta") or {}).get("url", ""),
                "source": (h.get("meta") or {}).get("source", ""),
                "distance": h.get("distance"),
            }
            for h in hits
        ]

    def _to_plain_text(self, text: str) -> str:
        if not text:
            return ""

        # 1) 删除常见 Markdown 包裹符（加粗、代码、标题）
        text = re.sub(r"\*\*(.*?)\*\*", r"\1", text)
        text = re.sub(r"__(.*?)__", r"\1", text)
        text = re.sub(r"`{1,3}(.*?)`{1,3}", r"\1", text, flags=re.DOTALL)
        text = re.sub(r"(?m)^\s{0,3}#{1,6}\s*", "", text)

        # 2) 统一中英文冒号后的空白，提升可读性
        text = re.sub(r"\s*([:：])\s*", r"\1 ", text)

        # 3) 去掉无序列表符号
        text = re.sub(r"(?m)^\s*[-*+]\s+", "", text)

        # 4) 识别编号并统一为“(n) 内容”格式
        text = re.sub(r"(?m)^\s*(\d+)\s*[\)\.、]\s*", r"(\1) ", text)

        # 5) 将粘连编号拆开：如“(1)aaa(2)bbb”或“1)aaa2)bbb”
        text = re.sub(r"\s*(?=\(\d+\)\s*)", "\n", text)
        text = re.sub(r"(?<!^)\s*(?=(\d+[\)\.、]))", "\n", text)

        # 6) 每个编号分点之间留一个空行，更美观
        text = re.sub(r"(?m)\n(?=\(\d+\)\s*)", "\n\n", text)

        # 7) 清理多余空白与空行
        text = re.sub(r"[ \t]+\n", "\n", text)
        text = re.sub(r"\n{3,}", "\n\n", text)

        return text.strip()

    def _chunk_text(self, text: str, chunk_size: int = 700, overlap: int = 120) -> List[str]:
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

    def ingest_document(self, title: str, content: str, source: str, url: str, publish_time: str) -> Dict:
        """摄入单篇文档到知识库"""
        if not content or not content.strip():
            return {"status": "skipped", "reason": "empty content"}

        chunks = self._chunk_text(content)
        ids, documents, metadatas, embeddings = [], [], [], []

        for i, chunk in enumerate(chunks):
            doc_id = hashlib.md5(f"{title}:{source}:{i}".encode("utf-8")).hexdigest()
            ids.append(doc_id)
            documents.append(chunk)
            metadatas.append({
                "title": title,
                "source": source,
                "url": url,
                "publish_time": publish_time,
            })
            embeddings.append(self.embed(chunk))

        # 先删除旧版本（同标题+同来源），再插入新版本
        self._delete_by_title_source(title, source)

        if ids:
            self.collection.add(ids=ids, documents=documents, metadatas=metadatas, embeddings=embeddings)

        return {"status": "ok", "chunks": len(ids)}

    def _delete_by_title_source(self, title: str, source: str):
        """按标题+来源删除文档"""
        try:
            old = self.collection.get(where={"$and": [
                {"title": title},
                {"source": source}
            ]})
            if old and old.get("ids"):
                self.collection.delete(ids=list(old["ids"]))
        except Exception:
            pass

    def delete_document(self, title: str, source: str) -> Dict:
        """从知识库中删除指定文档"""
        self._delete_by_title_source(title, source)
        return {"status": "ok", "deleted": True}

    def chat(self, query: str, top_k: int = 5) -> Dict:
        hits = self.retrieve(query, top_k=top_k)
        prompt = self.build_prompt(query, hits)

        resp = requests.post(
            f"{self.ollama_url}/api/generate",
            json={"model": self.chat_model, "prompt": prompt, "stream": False},
            timeout=180,
        )
        resp.raise_for_status()
        answer = resp.json().get("response", "")
        answer = self._to_plain_text(answer)

        return {"answer": answer, "references": self._build_references(hits)}

    def chat_stream(self, query: str, top_k: int = 5) -> Generator[Dict, None, None]:
        hits = self.retrieve(query, top_k=top_k)
        prompt = self.build_prompt(query, hits)

        yield {"type": "references", "references": self._build_references(hits)}

        resp = requests.post(
            f"{self.ollama_url}/api/generate",
            json={"model": self.chat_model, "prompt": prompt, "stream": True},
            stream=True,
            timeout=180,
        )
        resp.raise_for_status()

        for raw_line in resp.iter_lines(decode_unicode=True):
            if not raw_line:
                continue
            chunk = requests.models.complexjson.loads(raw_line)
            text = chunk.get("response", "")
            if text:
                yield {"type": "delta", "content": self._to_plain_text(text)}
            if chunk.get("done"):
                yield {"type": "done"}
