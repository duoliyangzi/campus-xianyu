from __future__ import annotations

import json
import re
from collections.abc import Iterator
from typing import Any

from app.config import get_settings


def llm_enabled() -> bool:
    return bool(get_settings().llm_api_key.strip())


def get_chat_model(*, streaming: bool = False):
    """返回 LangChain ChatOpenAI；无密钥时返回 None。"""
    settings = get_settings()
    if not settings.llm_api_key.strip():
        return None
    from langchain_openai import ChatOpenAI

    return ChatOpenAI(
        api_key=settings.llm_api_key,
        base_url=settings.llm_base_url,
        model=settings.llm_model,
        temperature=0.2,
        timeout=60,
        streaming=streaming,
    )


def chat_text(system: str, user: str, history: list[dict[str, Any]] | None = None) -> str:
    model = get_chat_model()
    if model is None:
        return ""
    from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

    messages: list[Any] = [SystemMessage(content=system)]
    for item in (history or [])[-8:]:
        role = str(item.get("role") or "")
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        if role == "user":
            messages.append(HumanMessage(content=content[:800]))
        elif role == "assistant":
            messages.append(AIMessage(content=content[:800]))
    messages.append(HumanMessage(content=user))
    response = model.invoke(messages)
    return str(response.content or "").strip()


def stream_text(system: str, user: str, history: list[dict[str, Any]] | None = None) -> Iterator[str]:
    model = get_chat_model(streaming=True)
    if model is None:
        return
    from langchain_core.messages import AIMessage, HumanMessage, SystemMessage

    messages: list[Any] = [SystemMessage(content=system)]
    for item in (history or [])[-8:]:
        role = str(item.get("role") or "")
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        if role == "user":
            messages.append(HumanMessage(content=content[:800]))
        elif role == "assistant":
            messages.append(AIMessage(content=content[:800]))
    messages.append(HumanMessage(content=user))
    for chunk in model.stream(messages):
        text = str(getattr(chunk, "content", "") or "")
        if text:
            yield text


def extract_json(text: str) -> dict[str, Any] | None:
    text = text.strip()
    if not text:
        return None
    try:
        return json.loads(text)
    except json.JSONDecodeError:
        match = re.search(r"\{[\s\S]*\}", text)
        if not match:
            return None
        try:
            return json.loads(match.group(0))
        except json.JSONDecodeError:
            return None
