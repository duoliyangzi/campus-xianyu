from __future__ import annotations

import json
import time
from collections.abc import Iterator
from typing import Any

from app.context_store import get_context_store
from app.graph_workflow import (
    _fallback_listing,
    _fallback_wanted,
    classify_user_intent,
    resolve_search_keyword,
)
from app.llm import chat_text, extract_json, llm_enabled, stream_text
from app.prompting import build_rag_system_prompt, merge_history
from app.rag import get_rag
from app.tools import search_products


def _sse(event: str, data: Any) -> str:
    payload = json.dumps(data, ensure_ascii=False)
    return f"event: {event}\ndata: {payload}\n\n"


def _clean_text(text: str) -> str:
    """去掉异常替换符，避免历史/模型输出里的 � 继续污染回答。"""
    if not text:
        return ""
    return text.replace("\ufffd", "").replace("�", "")


def _resolve_intent(
    message: str,
    mode: str,
    history: list[dict[str, Any]] | None = None,
) -> tuple[str, str, str]:
    """返回 (intent, search_keyword, source)。手动模式强制本能力；auto 走语义调度。"""
    mode = (mode or "auto").strip() or "auto"
    message = (message or "").strip()

    # 手动模式：各司其职，不再用关键词改道
    if mode in ("support", "listing", "wanted", "risk", "search"):
        return mode, "", "forced"

    classified = classify_user_intent(message, history)
    return (
        str(classified["intent"]),
        str(classified.get("keyword") or ""),
        str(classified.get("source") or "llm"),
    )


def _slow_chunks(text: str, size: int = 1, delay: float = 0.025) -> Iterator[str]:
    """按字延迟推送；首包后节奏约 40 字/秒，兼顾流畅与可见打字效果。"""
    text = text or ""
    for i in range(0, len(text), size):
        yield text[i : i + size]
        time.sleep(delay)


def _emit_llm_stream(system: str, user: str, history: list[dict[str, Any]] | None) -> Iterator[str]:
    """LLM 边生成边推；大块拆成单字，首字几乎不 sleep，后续轻微节流。"""
    first = True
    for token in stream_text(system, user, history=history):
        if not token:
            continue
        for piece in token:
            yield piece
            if first:
                first = False
                continue
            time.sleep(0.02)



def iter_sse_events(
    message: str,
    mode: str = "auto",
    history: list[dict[str, Any]] | None = None,
    user_id: str | None = None,
) -> Iterator[str]:
    """真正边生成边推：meta 先发，token 随模型/打字流出，最后 final。"""
    uid = (user_id or "anonymous").strip() or "anonymous"
    merged = merge_history(history, uid)
    yield _sse("status", {"text": "正在理解意图…", "phase": "routing"})
    intent, llm_keyword, route_source = _resolve_intent(message, mode, merged)

    yield _sse(
        "meta",
        {
            "intent": intent,
            "agent_trace": [f"orchestrator:{route_source}:{intent}", "stream:live"],
            "llm_enabled": llm_enabled(),
            "rag_backend": get_rag().backend,
            "context_backend": get_context_store().backend,
        },
    )
    # 立刻给前端一个状态事件，缩短「空白思考」体感
    yield _sse("status", {"text": "正在组织回答…", "phase": "preparing"})

    citations: list[dict[str, Any]] = []
    products: list[dict[str, Any]] = []
    listing: dict[str, Any] | None = None
    wanted: dict[str, Any] | None = None
    risk: dict[str, Any] | None = None
    answer_parts: list[str] = []
    trace = [f"orchestrator:{route_source}:{intent}", "stream:live"]

    try:
        if intent == "search":
            keyword = resolve_search_keyword(message, llm_keyword)
            products = search_products(keyword, size=6)
            trace.append(f"search:{keyword}:{len(products)}")
            system = (
                "你是校园咸鱼找货助手。根据工具检索结果用中文流式回答。"
                "不要编造不存在的商品；有检索结果时如实介绍标题与价格，并提示可点击卡片查看详情。"
                "输出必须是正常中文，禁止乱码。"
            )
            user = (
                f"用户问题：{message}\n"
                f"检索关键词：{keyword}\n"
                f"检索结果 JSON：{json.dumps({'count': len(products), 'products': products}, ensure_ascii=False)}"
            )
            if llm_enabled():
                for piece in _emit_llm_stream(system, user, merged):
                    piece = _clean_text(piece)
                    if not piece:
                        continue
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            else:
                text = (
                    f"为你找到 {len(products)} 件与「{keyword}」相关的在售商品。"
                    if products
                    else f"没有找到与「{keyword}」匹配的在售商品，可换关键词或发布求购。"
                )
                for piece in _slow_chunks(text):
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})

        elif intent == "listing":
            yield _sse("status", {"text": "正在生成挂牌文案…", "phase": "listing"})
            if llm_enabled():
                raw = chat_text(
                    "【角色】校园咸鱼·挂牌文案助手（出售闲置专用）。"
                    "【目标】把用户描述整理成可直接填入「发布商品」页的字段。"
                    "【禁止】不要做风控结论、不要生成求购文案、不要假装搜索库存。"
                    "只输出 JSON："
                    '{"title":"","description":"","category_hint":"","condition_hint":"","price_hint":null,"tips":""}',
                    message,
                    history=merged,
                )
                listing = extract_json(raw) or _fallback_listing(message)
            else:
                listing = _fallback_listing(message)
            text = (
                f"【挂牌模式】已生成出售文案建议（将填入「发布商品」页）：\n"
                f"标题：{listing.get('title')}\n"
                f"分类建议：{listing.get('category_hint')}\n"
                f"成色建议：{listing.get('condition_hint')}\n"
                f"描述：{listing.get('description')}\n"
                f"备注：{listing.get('tips') or '可在发布页直接修改后提交审核。'}"
            )
            trace.append("listing")
            for piece in _slow_chunks(text):
                answer_parts.append(piece)
                yield _sse("token", {"text": piece})

        elif intent == "wanted":
            yield _sse("status", {"text": "正在生成求购文案…", "phase": "wanted"})
            if llm_enabled():
                raw = chat_text(
                    "【角色】校园咸鱼·求购文案助手（想买/求购专用）。"
                    "【目标】把用户想买的东西整理成可填入「求购」页的字段。"
                    "【禁止】不要写出售挂牌文案、不要做风控 PASS/REJECT、不要编造在售库存。"
                    "只输出 JSON："
                    '{"item_name":"","budget_hint":null,"condition_hint":"","description":"","tips":""}',
                    message,
                    history=merged,
                )
                wanted = extract_json(raw) or _fallback_wanted(message)
            else:
                wanted = _fallback_wanted(message)
            text = (
                f"【求购模式】已生成求购文案建议（将填入「求购」页）：\n"
                f"物品名称：{wanted.get('item_name')}\n"
                f"预算建议：{wanted.get('budget_hint') if wanted.get('budget_hint') is not None else '请自行填写'}\n"
                f"期望成色：{wanted.get('condition_hint')}\n"
                f"补充描述：{wanted.get('description')}\n"
                f"备注：{wanted.get('tips') or '可在求购页修改后发布。'}"
            )
            trace.append("wanted")
            for piece in _slow_chunks(text):
                answer_parts.append(piece)
                yield _sse("token", {"text": piece})

        elif intent == "risk":
            from app.risk_assess import assess_content_risk, format_risk_answer

            yield _sse("status", {"text": "正在进行语义风控审核…", "phase": "risk"})
            risk = assess_content_risk(message, history=merged)
            text = format_risk_answer(risk)
            # REJECT（含辱骂）直接打字机输出固定结论，避免把原文再送 LLM 触发云厂商内容拦截
            if risk.get("suggestion") == "REJECT" or "abuse" in (risk.get("categories") or []):
                for piece in _slow_chunks(text):
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            elif llm_enabled():
                for piece in _emit_llm_stream(
                    "【角色】校园咸鱼·发布前风控预审员。"
                    "【目标】根据审核 JSON 用自然中文说明 PASS/REVIEW/REJECT 与原因。"
                    "【禁止】不要帮忙写出售/求购文案，不要去搜商品。"
                    "不要编造额外规则；不要改口成无关原因。输出纯中文，禁止乱码或替换符。",
                    f"审核JSON：{json.dumps(risk, ensure_ascii=False)}\n参考答复：{text}",
                    merged,
                ):
                    piece = _clean_text(piece)
                    if not piece:
                        continue
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            else:
                for piece in _slow_chunks(text):
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            trace.append(f"risk:llm+keyword:{risk.get('suggestion')}")

        else:
            # support：先本地 RAG，再真正 stream LLM（首字尽快、后续轻节流）
            intent = "support"
            yield _sse("status", {"text": "正在检索知识库…", "phase": "rag"})
            hits = get_rag().retrieve(message, top_k=3)
            citations = [
                {"title": h.title, "content": h.content, "score": round(h.score, 4)} for h in hits
            ]
            trace.append("rag:pre-retrieve")
            yield _sse("status", {"text": "正在生成回答…", "phase": "llm"})
            system = build_rag_system_prompt(
                base_rules=(
                    "【角色】校园咸鱼·客服助手（认证/订单/退货/举报等流程问答）。"
                    "【目标】根据知识片段与对话历史用中文回答平台规则。"
                    "【禁止】不要输出挂牌/求购 JSON，不要输出风控 PASS/REVIEW/REJECT 格式。"
                    "本系统支持流式输出；不要声称不支持流式。"
                    "只能依据片段与本站数据，禁止编造淘宝/闲鱼等外部平台政策。"
                    "若用户其实在找在售商品，可简短引导切换到「自动」模式或说明可搜首页商品。"
                    "若片段不足，明确说明并引导换问法。"
                    "输出必须是正常中文，禁止出现乱码或 Unicode 替换符。"
                ),
                history=merged,
                citations=citations,
                question=message,
            )
            if llm_enabled():
                for piece in _emit_llm_stream(system, message, merged):
                    piece = _clean_text(piece)
                    if not piece:
                        continue
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            elif citations:
                text = f"{citations[0].get('content')}\n\n（引用：{citations[0].get('title')}）"
                for piece in _slow_chunks(text):
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
            else:
                text = "暂时没有检索到相关资料，请换个问法，例如：如何实名认证。"
                for piece in _slow_chunks(text):
                    answer_parts.append(piece)
                    yield _sse("token", {"text": piece})
    except Exception as exc:  # noqa: BLE001
        err = f"生成失败：{exc}"
        answer_parts.append(err)
        yield _sse("token", {"text": err})

    answer = _clean_text("".join(answer_parts)).strip()
    store = get_context_store()
    store.append(uid, "user", message)
    if answer:
        store.append(uid, "assistant", answer)

    yield _sse(
        "final",
        {
            "answer": answer,
            "intent": intent,
            "citations": citations,
            "listing": listing,
            "wanted": wanted,
            "risk": risk,
            "products": products,
            "agent_trace": trace,
            "llm_enabled": llm_enabled(),
            "rag_backend": get_rag().backend,
            "context_backend": store.backend,
        },
    )
    yield _sse("done", {"ok": True})
