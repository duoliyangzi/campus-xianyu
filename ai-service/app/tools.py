from __future__ import annotations

import re
from typing import Any
from urllib.parse import urlparse

import httpx

from app.config import get_settings


def _normalize_media_url(url: Any) -> str | None:
    if not url:
        return None
    text = str(url).strip()
    if not text:
        return None
    if text.startswith("/uploads/"):
        return text
    try:
        parsed = urlparse(text)
        if parsed.path.startswith("/uploads/"):
            return parsed.path
    except Exception:
        return text
    return text


def _fetch_products(keyword: str, size: int) -> list[dict[str, Any]]:
    settings = get_settings()
    base = (settings.java_api_base or "http://127.0.0.1:8081/api").rstrip("/")
    params: dict[str, Any] = {"page": 0, "size": size}
    query = (keyword or "").strip()
    if query:
        params["keyword"] = query
    try:
        with httpx.Client(timeout=8.0) as client:
            response = client.get(f"{base}/products", params=params)
            response.raise_for_status()
            payload = response.json()
    except Exception:
        return []

    data = payload.get("data") if isinstance(payload, dict) else None
    if not isinstance(data, dict):
        return []
    content = data.get("content") or []
    products: list[dict[str, Any]] = []
    for item in content:
        if not isinstance(item, dict):
            continue
        products.append(
            {
                "id": item.get("id"),
                "title": item.get("title") or "",
                "price": item.get("price"),
                "coverUrl": _normalize_media_url(item.get("coverUrl") or item.get("cover_url")),
                "campusId": item.get("campusId") or item.get("campus_id"),
                "conditionLevel": item.get("conditionLevel") or item.get("condition_level"),
                "description": (item.get("description") or "")[:80],
            }
        )
    return [p for p in products if p.get("id")]


def search_products(keyword: str, size: int = 6) -> list[dict[str, Any]]:
    """调用 Java 商品列表；关键词无结果时回退模糊匹配首页在售。"""
    query = (keyword or "").strip()
    if query:
        hit = _fetch_products(query, size)
        if hit:
            return hit
        # 「二手手机」等前缀过窄时再试核心词
        for prefix in ("二手", "闲置", "校园"):
            if query.startswith(prefix) and len(query) > len(prefix):
                narrowed = query[len(prefix) :].strip()
                if narrowed:
                    hit = _fetch_products(narrowed, size)
                    if hit:
                        return hit
                    query = narrowed
                    break
        pool = _fetch_products("", 40)
        fuzzy = [
            p
            for p in pool
            if query in (p.get("title") or "") or query in (p.get("description") or "")
        ]
        if fuzzy:
            return fuzzy[:size]
        return []
    return _fetch_products("", size)[:size]


def extract_search_keyword(message: str) -> str:
    """从口语问句抽出商品关键词，例如「现在有手机卖吗」→「手机」。"""
    raw = (message or "").strip()
    if not raw:
        return "闲置"

    # 末尾锚定，避免 .+? 在可选后缀下只吃一个字
    patterns = (
        r"现在有没有\s*(.+?)(?:卖|在售)?[吗嘛]?$",
        r"现在有\s*(.+?)卖[吗嘛]?$",
        r"有没有人卖\s*(.+)$",
        r"有没有\s*(.+?)(?:卖|在售)?[吗嘛]?$",
        r"有\s*(.+?)卖[吗嘛]?$",
        r"(?:想要买|想买|我想买|我要买|我想要|帮我找|帮我看看|找一下|搜索|推荐)\s*(.+)$",
        r"(.+?)(?:有卖的吗|卖吗|在售吗)$",
    )
    for pat in patterns:
        m = re.search(pat, raw)
        if not m:
            continue
        candidate = (m.group(1) or "").strip(" ：:，,。.?？、的了呢啊呀")
        for noise in ("合适的", "相关的", "二手的", "闲置的"):
            candidate = candidate.replace(noise, "")
        candidate = candidate.strip(" ：:，,。.?？、的了呢啊呀")
        if candidate and len(candidate) <= 40:
            return candidate

    text = raw
    for noise in (
        "想要卖一个",
        "想要卖",
        "现在有卖的吗",
        "有卖的吗",
        "有没有卖",
        "现在有没有",
        "有没有人卖",
        "能买到吗",
        "在售吗",
        "在售",
        "想要买",
        "我想要",
        "我想买",
        "我要买",
        "想买",
        "现在有",
        "卖吗",
        "吗",
        "帮我找",
        "帮我看看",
        "有没有",
        "找一下",
        "搜索",
        "求购",
        "推荐",
        "合适的",
    ):
        text = text.replace(noise, " ")
    text = re.sub(r"\s+", " ", text).strip(" ：:，,。.?？、")
    return (text[:40] if text else raw[:40]) or "闲置"
