from __future__ import annotations

from functools import lru_cache
from typing import Sequence

import httpx

from app.config import get_settings


def embedding_enabled() -> bool:
    settings = get_settings()
    return bool(settings.llm_api_key.strip()) and bool(settings.embedding_model.strip())


@lru_cache
def _client() -> httpx.Client | None:
    settings = get_settings()
    if not embedding_enabled():
        return None
    return httpx.Client(
        base_url=settings.llm_base_url.rstrip("/"),
        headers={
            "Authorization": f"Bearer {settings.llm_api_key}",
            "Content-Type": "application/json",
        },
        timeout=60.0,
    )


def _embed(texts: list[str]) -> list[list[float]] | None:
    client = _client()
    if client is None or not texts:
        return None
    settings = get_settings()
    try:
        # 通义兼容接口：input 可为 string 或 string[]
        payload = {
            "model": settings.embedding_model,
            "input": texts if len(texts) > 1 else texts[0],
        }
        resp = client.post("/embeddings", json=payload)
        resp.raise_for_status()
        data = resp.json().get("data") or []
        # 按 index 排序，兼容批量返回
        data = sorted(data, key=lambda x: int(x.get("index", 0)))
        vectors = [list(item.get("embedding") or []) for item in data]
        if len(vectors) != len(texts) or not all(vectors):
            return None
        return vectors
    except Exception:
        return None


def embed_documents(texts: Sequence[str]) -> list[list[float]] | None:
    batch = [str(t) for t in texts]
    # 分批，避免一次过大
    out: list[list[float]] = []
    size = 10
    for i in range(0, len(batch), size):
        part = _embed(batch[i : i + size])
        if not part:
            return None
        out.extend(part)
    return out


def embed_query(text: str) -> list[float] | None:
    vectors = _embed([(text or "").strip()])
    if not vectors:
        return None
    return vectors[0]
