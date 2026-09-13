from __future__ import annotations

import json
import time
from threading import Lock
from typing import Any

from app.config import get_settings

try:
    import redis
except ImportError:  # pragma: no cover
    redis = None


class ContextStore:
    """近期对话缓存：优先 Redis，否则进程内存。"""

    def __init__(self) -> None:
        self.settings = get_settings()
        self._memory: dict[str, list[dict[str, Any]]] = {}
        self._lock = Lock()
        self._redis = None
        if self.settings.redis_url and redis is not None:
            try:
                client = redis.Redis.from_url(self.settings.redis_url, decode_responses=True)
                client.ping()
                self._redis = client
            except Exception:
                self._redis = None

    @property
    def backend(self) -> str:
        return "redis" if self._redis is not None else "memory"

    def _key(self, user_id: str) -> str:
        return f"ai:ctx:{user_id or 'anonymous'}"

    def get_history(self, user_id: str, limit: int = 12) -> list[dict[str, Any]]:
        key = self._key(user_id)
        if self._redis is not None:
            try:
                raw = self._redis.get(key)
                if not raw:
                    return []
                data = json.loads(raw)
                if isinstance(data, list):
                    return data[-limit:]
            except Exception:
                return []
            return []
        with self._lock:
            return list(self._memory.get(key, []))[-limit:]

    def append(self, user_id: str, role: str, content: str, limit: int = 12) -> None:
        if not content or not role:
            return
        item = {"role": role, "content": content[:2000], "ts": int(time.time())}
        key = self._key(user_id)
        if self._redis is not None:
            try:
                current = self.get_history(user_id, limit=100)
                current.append(item)
                current = current[-limit:]
                self._redis.setex(key, 24 * 3600, json.dumps(current, ensure_ascii=False))
                return
            except Exception:
                pass
        with self._lock:
            bucket = self._memory.setdefault(key, [])
            bucket.append(item)
            self._memory[key] = bucket[-limit:]

    def replace(self, user_id: str, history: list[dict[str, Any]], limit: int = 12) -> None:
        cleaned: list[dict[str, Any]] = []
        for item in history or []:
            role = str(item.get("role") or "")
            content = str(item.get("content") or "").strip()
            if role in ("user", "assistant") and content:
                cleaned.append({"role": role, "content": content[:2000], "ts": int(time.time())})
        cleaned = cleaned[-limit:]
        key = self._key(user_id)
        if self._redis is not None:
            try:
                if cleaned:
                    self._redis.setex(key, 24 * 3600, json.dumps(cleaned, ensure_ascii=False))
                else:
                    self._redis.delete(key)
                return
            except Exception:
                pass
        with self._lock:
            if cleaned:
                self._memory[key] = cleaned
            else:
                self._memory.pop(key, None)

    def clear(self, user_id: str) -> None:
        key = self._key(user_id)
        if self._redis is not None:
            try:
                self._redis.delete(key)
            except Exception:
                pass
        with self._lock:
            self._memory.pop(key, None)


_store: ContextStore | None = None


def get_context_store() -> ContextStore:
    global _store
    if _store is None:
        _store = ContextStore()
    return _store
