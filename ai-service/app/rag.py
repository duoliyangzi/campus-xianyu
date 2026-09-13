from __future__ import annotations

import json
import re
import time
from dataclasses import dataclass
from pathlib import Path
from typing import Any

import numpy as np
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.metrics.pairwise import cosine_similarity

from app.config import get_settings
from app.embeddings import embed_documents, embed_query, embedding_enabled


@dataclass
class RetrievedChunk:
    title: str
    content: str
    score: float
    chunk_id: str = ""


def _split_long(text: str, size: int, overlap: int) -> list[str]:
    text = (text or "").strip()
    if not text:
        return []
    if len(text) <= size:
        return [text]
    parts: list[str] = []
    start = 0
    step = max(1, size - overlap)
    while start < len(text):
        end = min(len(text), start + size)
        parts.append(text[start:end].strip())
        if end >= len(text):
            break
        start += step
    return [p for p in parts if p]


class FaqRag:
    """FAQ RAG：按 ## 切段 + 定长 overlap；优先 Chroma 向量检索，失败回退 TF-IDF。"""

    def __init__(self, knowledge_file: str | None = None) -> None:
        settings = get_settings()
        path = Path(knowledge_file or settings.knowledge_path)
        if not path.is_absolute():
            path = Path(__file__).resolve().parent.parent / path
        self._path = path
        text = path.read_text(encoding="utf-8") if path.exists() else ""
        self.chunks = self._split(text, settings.chunk_size, settings.chunk_overlap)
        corpus = [f"{c['title']}\n{c['content']}" for c in self.chunks] or ["空知识库"]
        self.vectorizer = TfidfVectorizer(analyzer="char_wb", ngram_range=(2, 4))
        self.matrix = self.vectorizer.fit_transform(corpus)
        self.backend = "tfidf"
        self._chroma = None
        self._collection = None
        self._build_chroma(settings)

    @staticmethod
    def _split(text: str, chunk_size: int, chunk_overlap: int) -> list[dict[str, str]]:
        parts = re.split(r"\n##\s+", text.strip())
        chunks: list[dict[str, str]] = []
        idx = 0
        for part in parts:
            part = part.strip()
            if not part or part.startswith("# "):
                continue
            lines = part.splitlines()
            title = lines[0].strip().lstrip("#").strip()
            body = "\n".join(lines[1:]).strip()
            if not title or not body:
                continue
            for piece in _split_long(body, chunk_size, chunk_overlap):
                chunks.append(
                    {
                        "title": title,
                        "content": piece,
                        "chunk_id": f"{idx}-{abs(hash(title + piece)) % 10_000_000}",
                    }
                )
                idx += 1
        return chunks

    def _build_chroma(self, settings) -> None:
        if not self.chunks or not embedding_enabled():
            return
        try:
            import chromadb
            from chromadb.config import Settings as ChromaSettings

            chroma_dir = Path(settings.chroma_path)
            if not chroma_dir.is_absolute():
                chroma_dir = Path(__file__).resolve().parent.parent / chroma_dir
            chroma_dir.mkdir(parents=True, exist_ok=True)
            client = chromadb.PersistentClient(
                path=str(chroma_dir),
                settings=ChromaSettings(anonymized_telemetry=False),
            )
            collection = client.get_or_create_collection(
                name="campus_faq",
                metadata={"hnsw:space": "cosine"},
            )
            ids = [c["chunk_id"] for c in self.chunks]
            existing = set()
            try:
                got = collection.get(include=[])
                existing = set(got.get("ids") or [])
            except Exception:
                existing = set()
            need_rebuild = set(ids) != existing or collection.count() != len(ids)
            if need_rebuild:
                if existing:
                    collection.delete(ids=list(existing))
                docs = [f"{c['title']}\n{c['content']}" for c in self.chunks]
                vectors = embed_documents(docs)
                if not vectors or len(vectors) != len(docs):
                    return
                collection.add(
                    ids=ids,
                    documents=docs,
                    embeddings=vectors,
                    metadatas=[{"title": c["title"]} for c in self.chunks],
                )
            self._chroma = client
            self._collection = collection
            self.backend = "chroma"
        except Exception:
            self._chroma = None
            self._collection = None
            self.backend = "tfidf"

    def retrieve(self, query: str, top_k: int = 3, min_score: float = 0.05) -> list[RetrievedChunk]:
        if not self.chunks:
            return []
        vector_hits = self._retrieve_chroma(query, top_k=top_k)
        tfidf_hits = self._retrieve_tfidf(query, top_k=top_k, min_score=min_score)
        if not vector_hits:
            return tfidf_hits
        # 融合：同 chunk 取较高分，向量分映射到 0~1 量级
        merged: dict[str, RetrievedChunk] = {}
        for hit in tfidf_hits:
            merged[hit.chunk_id or hit.title + hit.content[:20]] = hit
        for hit in vector_hits:
            key = hit.chunk_id or hit.title + hit.content[:20]
            prev = merged.get(key)
            if prev is None or hit.score > prev.score:
                merged[key] = hit
        ranked = sorted(merged.values(), key=lambda x: x.score, reverse=True)
        return ranked[:top_k]

    def _retrieve_tfidf(self, query: str, top_k: int, min_score: float) -> list[RetrievedChunk]:
        query_vec = self.vectorizer.transform([query])
        scores = cosine_similarity(query_vec, self.matrix)[0]
        ranked = np.argsort(scores)[::-1][:top_k]
        results: list[RetrievedChunk] = []
        for idx in ranked:
            score = float(scores[idx])
            if score < min_score:
                continue
            chunk = self.chunks[int(idx)]
            results.append(
                RetrievedChunk(
                    title=chunk["title"],
                    content=chunk["content"],
                    score=score,
                    chunk_id=chunk.get("chunk_id", ""),
                )
            )
        return results

    def _retrieve_chroma(self, query: str, top_k: int) -> list[RetrievedChunk]:
        if self._collection is None:
            return []
        qvec = embed_query(query)
        if not qvec:
            return []
        try:
            res = self._collection.query(query_embeddings=[qvec], n_results=max(1, top_k))
            docs = (res.get("documents") or [[]])[0]
            metas = (res.get("metadatas") or [[]])[0]
            distances = (res.get("distances") or [[]])[0]
            ids = (res.get("ids") or [[]])[0]
            results: list[RetrievedChunk] = []
            for i, doc in enumerate(docs):
                title = ""
                if i < len(metas) and isinstance(metas[i], dict):
                    title = str(metas[i].get("title") or "")
                # cosine distance -> similarity-ish
                dist = float(distances[i]) if i < len(distances) else 1.0
                score = max(0.0, 1.0 - dist)
                content = doc
                if title and doc.startswith(title):
                    content = doc[len(title) :].lstrip("\n")
                results.append(
                    RetrievedChunk(
                        title=title or "FAQ",
                        content=content,
                        score=score,
                        chunk_id=str(ids[i]) if i < len(ids) else "",
                    )
                )
            return results
        except Exception:
            return []

    def status(self) -> dict[str, Any]:
        return {
            "backend": self.backend,
            "chunk_count": len(self.chunks),
            "embedding_enabled": embedding_enabled(),
            "knowledge_path": str(self._path),
        }


_rag: FaqRag | None = None


def get_rag() -> FaqRag:
    global _rag
    settings = get_settings()
    path = Path(settings.knowledge_path)
    if not path.is_absolute():
        path = Path(__file__).resolve().parent.parent / path
    mtime = path.stat().st_mtime if path.exists() else 0.0
    if _rag is None or getattr(_rag, "_mtime", None) != mtime:
        _rag = FaqRag()
        _rag._mtime = mtime  # type: ignore[attr-defined]
    return _rag


def reload_rag() -> FaqRag:
    global _rag
    _rag = FaqRag()
    return _rag


def format_citations_block(citations: list[dict[str, Any]]) -> str:
    if not citations:
        return "（未检索到相关知识片段）"
    lines: list[str] = []
    for i, c in enumerate(citations, 1):
        lines.append(
            f"[{i}] {c.get('title', '')}（score={c.get('score', 0)}）\n{c.get('content', '')}"
        )
    return "\n\n".join(lines)
