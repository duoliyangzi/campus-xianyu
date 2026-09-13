from __future__ import annotations

import time
from collections import defaultdict, deque
from threading import Lock

from app.config import get_settings

try:
    import redis
except ImportError:  # pragma: no cover
    redis = None


class RateLimiter:
    """优先 Redis；不可用时退化为进程内滑动窗口限流。"""

    def __init__(self) -> None:
        self.settings = get_settings()
        self._memory: dict[str, deque[float]] = defaultdict(deque)
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

    def allow(self, user_key: str) -> bool:
        limit = max(1, self.settings.rate_limit_per_minute)
        key = f"ai:ratelimit:{user_key}"
        now = time.time()
        if self._redis is not None:
            pipe = self._redis.pipeline()
            pipe.zremrangebyscore(key, 0, now - 60)
            pipe.zadd(key, {str(now): now})
            pipe.zcard(key)
            pipe.expire(key, 70)
            _, _, count, _ = pipe.execute()
            return int(count) <= limit

        with self._lock:
            bucket = self._memory[key]
            while bucket and bucket[0] <= now - 60:
                bucket.popleft()
            if len(bucket) >= limit:
                return False
            bucket.append(now)
            return True
