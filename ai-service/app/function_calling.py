from __future__ import annotations

import json
from collections.abc import Iterator
from typing import Any, Sequence

from langchain_core.messages import AIMessage, HumanMessage, SystemMessage, ToolMessage
from langchain_core.tools import BaseTool

from app.agent_tools import parse_tool_payload
from app.llm import get_chat_model, llm_enabled


def invoke_with_function_calls(
    *,
    system: str,
    user: str,
    tools: Sequence[BaseTool],
    history: list[dict[str, Any]] | None = None,
    max_rounds: int = 4,
) -> dict[str, Any]:
    """使用模型原生 Function Calling（bind_tools / tool_calls）执行多轮工具调用。"""
    if not llm_enabled():
        return {
            "answer": "",
            "tool_trace": [],
            "products": [],
            "citations": [],
            "risk": None,
            "raw_tool_results": [],
            "used_function_calling": False,
        }

    model = get_chat_model()
    if model is None:
        return {
            "answer": "",
            "tool_trace": [],
            "products": [],
            "citations": [],
            "risk": None,
            "raw_tool_results": [],
            "used_function_calling": False,
        }

    tool_map = {t.name: t for t in tools}
    llm = model.bind_tools(list(tools))

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

    tool_trace: list[str] = []
    products: list[dict[str, Any]] = []
    citations: list[dict[str, Any]] = []
    risk: dict[str, Any] | None = None
    raw_tool_results: list[dict[str, Any]] = []

    final_text = ""
    for _ in range(max_rounds):
        response = llm.invoke(messages)
        messages.append(response)
        tool_calls = getattr(response, "tool_calls", None) or []
        if not tool_calls:
            final_text = str(response.content or "").strip()
            break

        for call in tool_calls:
            name = call.get("name") if isinstance(call, dict) else getattr(call, "name", "")
            args = call.get("args") if isinstance(call, dict) else getattr(call, "args", {})
            call_id = call.get("id") if isinstance(call, dict) else getattr(call, "id", name)
            tool = tool_map.get(name)
            tool_trace.append(f"fc:{name}")
            if tool is None:
                payload = json.dumps({"error": f"unknown tool: {name}"}, ensure_ascii=False)
            else:
                try:
                    payload = tool.invoke(args or {})
                except Exception as exc:  # noqa: BLE001
                    payload = json.dumps({"error": str(exc)}, ensure_ascii=False)
            if not isinstance(payload, str):
                payload = json.dumps(payload, ensure_ascii=False)

            parsed = parse_tool_payload(payload)
            raw_tool_results.append({"name": name, "args": args, "result": parsed})
            if name == "search_campus_products" and isinstance(parsed.get("products"), list):
                products = parsed["products"]
            if name == "retrieve_faq" and isinstance(parsed.get("citations"), list):
                citations = parsed["citations"]
            if name == "risk_precheck":
                risk = parsed

            messages.append(
                ToolMessage(
                    content=payload,
                    tool_call_id=str(call_id or name),
                )
            )
    else:
        final_text = str(getattr(messages[-1], "content", "") or "").strip()

    return {
        "answer": final_text,
        "tool_trace": tool_trace,
        "products": products,
        "citations": citations,
        "risk": risk,
        "raw_tool_results": raw_tool_results,
        "used_function_calling": True,
        "messages": messages,
    }


def stream_final_answer_from_messages(messages: list[Any]) -> Iterator[str]:
    """在工具调用完成后，对已有 messages 再流式生成最终自然语言回答。"""
    model = get_chat_model(streaming=True)
    if model is None:
        return
    # 去掉可能的空 AI 尾，追加一次无工具流式生成
    for chunk in model.stream(messages):
        text = str(getattr(chunk, "content", "") or "")
        if text:
            yield text
