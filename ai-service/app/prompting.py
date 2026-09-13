from __future__ import annotations

from typing import Any

from app.context_store import get_context_store
from app.rag import format_citations_block


def merge_history(
    request_history: list[dict[str, Any]] | None,
    user_id: str | None,
    limit: int = 12,
) -> list[dict[str, Any]]:
    """合并请求携带的 history 与 Redis/内存近期上下文。"""
    store = get_context_store()
    cached = store.get_history(user_id or "anonymous", limit=limit) if user_id else []
    incoming = []
    for item in request_history or []:
        role = str(item.get("role") or "")
        content = str(item.get("content") or "").strip()
        if role in ("user", "assistant") and content:
            incoming.append({"role": role, "content": content})
    # 请求 history 优先（通常更新）；不足时用缓存补齐前缀
    if incoming:
        merged = incoming[-limit:]
    else:
        merged = [{"role": i["role"], "content": i["content"]} for i in cached][-limit:]
    return merged


def format_history_block(history: list[dict[str, Any]] | None, limit: int = 8) -> str:
    if not history:
        return "（无历史）"
    lines: list[str] = []
    for item in history[-limit:]:
        role = str(item.get("role") or "")
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        label = "用户" if role == "user" else "助手"
        lines.append(f"{label}：{content[:400]}")
    return "\n".join(lines) if lines else "（无历史）"


def build_rag_system_prompt(
    *,
    base_rules: str,
    history: list[dict[str, Any]] | None,
    citations: list[dict[str, Any]] | None,
    question: str,
) -> str:
    return (
        f"{base_rules.strip()}\n\n"
        f"【对话历史】\n{format_history_block(history)}\n\n"
        f"【检索到的知识片段】\n{format_citations_block(citations or [])}\n\n"
        f"【当前用户问题】\n{question.strip()}\n\n"
        "请综合以上历史与知识片段用中文回答；只能依据片段与本站数据，禁止编造外部电商平台政策。"
    )
