from __future__ import annotations

import json
from typing import Any, Literal, TypedDict

from langgraph.graph import END, StateGraph

from app.agent_tools import SEARCH_TOOLS, SUPPORT_TOOLS, retrieve_faq
from app.context_store import get_context_store
from app.function_calling import invoke_with_function_calls
from app.llm import chat_text, extract_json, llm_enabled
from app.prompting import build_rag_system_prompt, merge_history
from app.rag import get_rag
from app.tools import extract_search_keyword, search_products


Intent = Literal["support", "listing", "wanted", "risk", "search", "unknown"]


class AgentState(TypedDict, total=False):
    message: str
    intent: Intent
    forced_intent: Intent | None
    search_keyword: str
    answer: str
    citations: list[dict[str, Any]]
    listing: dict[str, Any] | None
    wanted: dict[str, Any] | None
    risk: dict[str, Any] | None
    products: list[dict[str, Any]]
    agent_trace: list[str]
    mode: str
    history: list[dict[str, Any]]


def _trace(state: AgentState, name: str) -> list[str]:
    return list(state.get("agent_trace") or []) + [name]


def route_intent(state: AgentState) -> AgentState:
    forced = state.get("forced_intent")
    message = (state.get("message") or "").strip()
    history = state.get("history") or []

    # 手动模式：严格执行各司其职
    if forced in ("support", "listing", "wanted", "risk", "search"):
        intent: Intent = forced  # type: ignore[assignment]
        return {
            **state,
            "intent": intent,
            "search_keyword": "",
            "agent_trace": _trace(state, "orchestrator:forced"),
        }

    classified = classify_user_intent(message, history)
    intent = classified["intent"]
    return {
        **state,
        "intent": intent,
        "search_keyword": classified.get("keyword") or "",
        "agent_trace": _trace(state, f"orchestrator:{classified.get('source')}:{intent}"),
    }


def _looks_like_product_search(message: str) -> bool:
    """关键词启发：仅作 LLM 不可用时的兜底，不作为主路由。"""
    text = (message or "").strip()
    if not text:
        return False
    if any(k in text for k in ["求购", "发布求购", "一条求购", "发个求购"]):
        return False
    search_cues = (
        "有卖的吗",
        "有没有卖",
        "现在有卖",
        "现在有没有",
        "现在有",
        "有没有人卖",
        "能买到吗",
        "卖吗",
        "在售",
        "帮我找",
        "我想要",
        "我想买",
        "我要买",
        "想买",
        "想要买",
        "有没有合适",
        "有合适的",
        "有没有",
        "推荐几件",
        "找一下",
        "搜索",
    )
    return any(k in text for k in search_cues)


def _heuristic_intent(message: str) -> Intent:
    """无 LLM 时的关键词兜底路由。"""
    text = message.lower()
    if any(k in message for k in ["审核", "违规", "风险", "能不能发", "敏感", "预审", "风控"]):
        return "risk"
    if any(k in message for k in ["求购", "发布求购", "一条求购", "发个求购", "想要求购"]):
        return "wanted"
    if _looks_like_product_search(message):
        return "search"
    if any(
        k in message
        for k in [
            "标题",
            "描述",
            "挂牌",
            "帮我写",
            "生成文案",
            "怎么写",
            "出闲置",
            "帮我卖",
            "写个标题",
            "九成新",
            "成色",
            "我想卖",
            "出售",
        ]
    ):
        return "listing"
    if any(k in text for k in ["order", "认证", "订单", "私聊", "举报", "发布", "怎么", "退货", "退款"]):
        return "support"
    return "support"


_ORCHESTRATOR_SYSTEM = (
    "你是校园咸鱼 Multi-Agent 调度器。根据用户语义判断意图，不要死磕字面关键词。"
    "只输出 JSON（不要其他文字）："
    '{"intent":"support|listing|wanted|risk|search","keyword":"","reason":""}'
    "字段说明："
    "intent："
    " support=平台流程咨询（认证/订单/退货/举报/怎么用）；"
    " listing=用户要卖闲置、写出售文案、生成挂牌标题描述；"
    " wanted=用户要发布求购帖（主动挂「求购信息」）；"
    " risk=对某段标题/描述做发布前风控预审；"
    " search=想看看平台现在有没有某类在售商品（浏览/找货/「有没有卖」）。"
    "keyword：仅当 intent=search 时填写短检索词（如「手机」「台灯」），否则为空字符串。"
    "易混辨析（按语义）："
    " 「想买手机 / 有没有手机 / 现在有服装卖吗」→ search（先看现货）；"
    " 「帮我发一条求购跳绳 / 发布求购」→ wanted；"
    " 「帮我写个出售文案 / 出闲置高数」→ listing；"
    " 「这段文案能不能发 / 帮我预审」→ risk；"
    " 不要把普通找货判成 risk；不要把「想买X」默认判成 wanted。"
)


def classify_user_intent(
    message: str,
    history: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    """语义主判意图；关键词规则仅作无 LLM / 解析失败时的辅助兜底。"""
    message = (message or "").strip()
    if not message:
        return {"intent": "support", "keyword": "", "source": "empty", "reason": ""}

    if llm_enabled():
        raw = chat_text(_ORCHESTRATOR_SYSTEM, message, history=history)
        data = extract_json(raw) or {}
        value = str(data.get("intent") or "").strip()
        if value in ("support", "listing", "wanted", "risk", "search"):
            keyword = str(data.get("keyword") or "").strip()[:40]
            if value != "search":
                keyword = ""
            # 极轻量纠偏：模型把「发布求购」说成 listing 时纠正
            if value == "listing" and any(k in message for k in ["发布求购", "一条求购", "发个求购"]):
                value = "wanted"
            return {
                "intent": value,
                "keyword": keyword,
                "source": "llm",
                "reason": str(data.get("reason") or "")[:120],
            }

    intent = _heuristic_intent(message)
    keyword = extract_search_keyword(message) if intent == "search" else ""
    return {"intent": intent, "keyword": keyword, "source": "heuristic", "reason": "llm_unavailable"}


def resolve_search_keyword(message: str, llm_keyword: str | None = None) -> str:
    """检索词：优先用调度器语义抽取，规则抽取仅辅助。"""
    kw = (llm_keyword or "").strip()
    if kw and kw not in ("闲置", "商品", "东西", "物品"):
        return kw[:40]
    return extract_search_keyword(message)


def route_intent_auto(
    message: str,
    history: list[dict[str, Any]] | None = None,
) -> Intent:
    """自动模式：LLM 语义识别为主，关键词启发为辅。"""
    return classify_user_intent(message, history)["intent"]  # type: ignore[return-value]


def support_agent(state: AgentState) -> AgentState:
    """客服 Agent：预检索 RAG + Function Calling，综合历史/片段/当前问题作答。"""
    message = state.get("message") or ""
    history = state.get("history") or []

    # 先检索 FAQ，保证「历史 + 片段 + 原问题」进入模型上下文
    pre_hits = get_rag().retrieve(message, top_k=3)
    pre_citations = [
        {"title": h.title, "content": h.content, "score": round(h.score, 4)} for h in pre_hits
    ]

    if llm_enabled():
        system = build_rag_system_prompt(
            base_rules=(
                "你是校园咸鱼客服。必须通过 Function Calling 使用工具，不要假装调用。"
                "若上方知识片段不足，再调用 retrieve_faq；"
                "用户找在售商品时调用 search_campus_products。"
                "只能依据工具返回与知识片段回答，不要编造淘宝/闲鱼等外部平台政策。"
                "若无结果，明确说明并引导换问法。"
                "你当前是客服，不要输出风控预审 PASS/REVIEW/REJECT 格式。"
            ),
            history=history,
            citations=pre_citations,
            question=message,
        )
        fc = invoke_with_function_calls(
            system=system,
            user=message,
            tools=SUPPORT_TOOLS,
            history=history,
        )
        answer = fc["answer"]
        products = fc["products"] or []
        citations = fc["citations"] or pre_citations
        if not answer:
            if products:
                answer = f"为你找到 {len(products)} 件相关在售商品，可点击卡片查看详情。"
            elif citations:
                answer = citations[0].get("content") or "已检索到相关资料。"
            else:
                answer = "暂时没有得到足够资料，请换个问法，例如：如何实名认证、现在有手机卖吗。"
        trace = _trace(state, "support:function-calling")
        for item in fc.get("tool_trace") or []:
            trace.append(item)
        if pre_citations:
            trace.append("rag:pre-retrieve")
        return {
            **state,
            "intent": "search" if products and not citations else state.get("intent") or "support",
            "answer": answer,
            "citations": citations,
            "products": products,
            "agent_trace": trace,
        }

    # 离线兜底：直接调同一套工具实现（非 FC）
    if _looks_like_product_search(message):
        return search_agent({**state, "agent_trace": _trace(state, "support:offline-search")})
    raw = retrieve_faq.invoke({"query": message})
    from app.agent_tools import parse_tool_payload

    parsed = parse_tool_payload(raw)
    citations = parsed.get("citations") or []
    if citations:
        hits = [
            type("H", (), {"title": c["title"], "content": c["content"], "score": c.get("score", 0)})()
            for c in citations
        ]
        return {
            **state,
            "answer": _fallback_support(hits),
            "citations": citations,
            "products": [],
            "agent_trace": _trace(state, "support:offline-rag"),
        }
    return {
        **state,
        "answer": "知识库未检索到相关内容。请配置 LLM_API_KEY 以启用 Function Calling，或换个问法。",
        "citations": [],
        "products": [],
        "agent_trace": _trace(state, "support:offline-no-hit"),
    }


def _format_history(history: list[dict[str, Any]] | None) -> str:
    if not history:
        return ""
    lines: list[str] = ["【对话上下文】"]
    for item in history[-8:]:
        role = str(item.get("role") or "")
        content = str(item.get("content") or "").strip()
        if not content:
            continue
        label = "用户" if role == "user" else "助手"
        lines.append(f"{label}：{content[:300]}")
    lines.append("")
    return "\n".join(lines) + "\n"


def _fallback_support(hits) -> str:
    top = hits[0]
    titles = "、".join(getattr(h, "title", h.get("title") if isinstance(h, dict) else "") for h in hits)
    content = getattr(top, "content", top.get("content") if isinstance(top, dict) else "")
    return f"{content}\n\n（引用：{titles}）【离线检索模式：未配置 LLM_API_KEY】"


def search_agent(state: AgentState) -> AgentState:
    """搜商品 Agent：Function Calling 调用 search_campus_products。"""
    message = state.get("message") or ""
    history = state.get("history") or []

    if llm_enabled():
        fc = invoke_with_function_calls(
            system=(
                "你是校园咸鱼找货助手。必须调用 search_campus_products 工具检索在售商品，"
                "再根据工具结果用中文简要说明；若无结果请明确告知。"
                "不要编造不存在的商品。"
            ),
            user=message,
            tools=SEARCH_TOOLS,
            history=history,
        )
        products = fc["products"] or []
        answer = fc["answer"]
        if not answer:
            if products:
                lines = [f"为你找到 {len(products)} 件相关在售商品，点击卡片可看详情："]
                for item in products:
                    lines.append(f"- {item.get('title')}　￥{item.get('price')}")
                answer = "\n".join(lines)
            else:
                answer = "没有找到匹配的在售商品，可以换个关键词或去首页筛选。"
        trace = _trace(state, "search:function-calling")
        for item in fc.get("tool_trace") or []:
            trace.append(item)
        return {
            **state,
            "intent": "search",
            "answer": answer,
            "products": products,
            "agent_trace": trace,
        }

    keyword = resolve_search_keyword(message, state.get("search_keyword"))
    products = search_products(keyword, size=6)
    if not products:
        return {
            **state,
            "intent": "search",
            "answer": f"没有在首页找到与「{keyword}」匹配的在售商品。",
            "products": [],
            "agent_trace": _trace(state, "search:offline-empty"),
        }
    lines = [f"为你找到 {len(products)} 件与「{keyword}」相关的在售商品，点击卡片可看详情："]
    for item in products:
        lines.append(f"- {item.get('title')}　￥{item.get('price')}")
    return {
        **state,
        "intent": "search",
        "answer": "\n".join(lines),
        "products": products,
        "agent_trace": _trace(state, f"search:offline:{len(products)}"),
    }


def listing_agent(state: AgentState) -> AgentState:
    message = state.get("message") or ""
    history = state.get("history") or []
    listing: dict[str, Any]
    if llm_enabled():
        raw = chat_text(
            "【角色】校园咸鱼·挂牌文案助手（出售闲置专用）。"
            "根据描述输出 JSON："
            '{"title":"","description":"","category_hint":"","condition_hint":"","price_hint":null,"tips":""}'
            "不要输出其他文字。condition_hint 用：全新/几乎全新/成色较好/有使用痕迹/旧一些。"
            "【禁止】不要做风控结论、不要生成求购文案、不要假装搜索库存。"
            "可参考对话历史理解用户补充信息。",
            message,
            history=history,
        )
        listing = extract_json(raw) or _fallback_listing(message)
    else:
        listing = _fallback_listing(message)
    answer = (
        f"已生成出售挂牌建议（将填入「发布商品」页）：\n"
        f"标题：{listing.get('title')}\n"
        f"分类建议：{listing.get('category_hint')}\n"
        f"成色建议：{listing.get('condition_hint')}\n"
        f"描述：{listing.get('description')}\n"
        f"备注：{listing.get('tips') or '可在发布页直接修改后提交审核。'}"
    )
    return {
        **state,
        "listing": listing,
        "wanted": None,
        "answer": answer,
        "products": [],
        "agent_trace": _trace(state, "listing"),
    }


def _fallback_listing(message: str) -> dict[str, Any]:
    title = message.strip().replace("\n", " ")[:40] or "校园闲置物品"
    return {
        "title": title,
        "description": f"{message.strip()}\n\n支持校内面交，细节可私聊。",
        "category_hint": "其他闲置",
        "condition_hint": "成色较好",
        "price_hint": None,
        "tips": "离线模式：配置 LLM_API_KEY 后可获得更优文案。",
    }


def wanted_agent(state: AgentState) -> AgentState:
    """求购 Agent：生成求购表单字段，前端填入「求购」页而非出售发布页。"""
    message = state.get("message") or ""
    history = state.get("history") or []
    wanted: dict[str, Any]
    if llm_enabled():
        raw = chat_text(
            "【角色】校园咸鱼·求购文案助手（想买/求购专用，不是出售）。"
            "只输出 JSON："
            '{"item_name":"","budget_hint":null,"condition_hint":"","description":"","tips":""}'
            "condition_hint 用：全新/几乎全新/成色较好/有使用痕迹/旧一些。"
            "item_name 要短；description 写清用途与可接受情况；budget_hint 为数字或 null。"
            "【禁止】不要写出售挂牌文案、不要做风控 PASS/REJECT、不要编造在售库存。"
            "可参考对话历史理解用户补充信息。",
            message,
            history=history,
        )
        wanted = extract_json(raw) or _fallback_wanted(message)
    else:
        wanted = _fallback_wanted(message)
    answer = (
        f"已生成求购建议（将填入「求购」页，不是出售发布页）：\n"
        f"物品名称：{wanted.get('item_name')}\n"
        f"预算建议：{wanted.get('budget_hint') if wanted.get('budget_hint') is not None else '请自行填写'}\n"
        f"期望成色：{wanted.get('condition_hint')}\n"
        f"补充描述：{wanted.get('description')}\n"
        f"备注：{wanted.get('tips') or '可在求购页修改后发布。'}"
    )
    return {
        **state,
        "wanted": wanted,
        "listing": None,
        "answer": answer,
        "products": [],
        "agent_trace": _trace(state, "wanted"),
    }


def _fallback_wanted(message: str) -> dict[str, Any]:
    name = message.strip().replace("\n", " ")
    for prefix in ("我想发布一条求购，", "我想发布一条求购", "发布求购，", "求购", "想要", "我想要"):
        name = name.replace(prefix, "")
    name = name.strip(" ，,。")[:40] or "求购物品"
    return {
        "item_name": name,
        "budget_hint": None,
        "condition_hint": "成色较好",
        "description": f"求购：{message.strip()}\n校内面交优先，详情可私聊。",
        "tips": "离线模式：配置 LLM_API_KEY 后可获得更优求购文案。",
    }


def risk_agent(state: AgentState) -> AgentState:
    """风控 Agent：关键词硬规则 + 大模型语义审核。"""
    from app.risk_assess import assess_content_risk, format_risk_answer

    message = state.get("message") or ""
    history = state.get("history") or []
    risk = assess_content_risk(message, history=history)
    answer = format_risk_answer(risk)
    # 有 LLM 且未被云厂商拦截时，可润色说明；REJECT 时优先固定模板，避免再次触发内容安检
    if llm_enabled() and risk.get("suggestion") == "REJECT":
        # 不再把原文回灌大模型润色，防止 data_inspection_failed
        answer = format_risk_answer(risk)
    return {
        **state,
        "risk": risk,
        "answer": answer,
        "products": [],
        "agent_trace": _trace(state, f"risk:llm+keyword:{risk.get('suggestion')}"),
    }


def build_graph():
    graph = StateGraph(AgentState)
    graph.add_node("orchestrator", route_intent)
    graph.add_node("support_node", support_agent)
    graph.add_node("listing_node", listing_agent)
    graph.add_node("wanted_node", wanted_agent)
    graph.add_node("risk_node", risk_agent)
    graph.add_node("search_node", search_agent)

    graph.set_entry_point("orchestrator")

    def select_agent(state: AgentState) -> str:
        intent = state.get("intent") or "support"
        if intent == "listing":
            return "listing_node"
        if intent == "wanted":
            return "wanted_node"
        if intent == "risk":
            return "risk_node"
        if intent == "search":
            return "search_node"
        return "support_node"

    graph.add_conditional_edges(
        "orchestrator",
        select_agent,
        {
            "support_node": "support_node",
            "listing_node": "listing_node",
            "wanted_node": "wanted_node",
            "risk_node": "risk_node",
            "search_node": "search_node",
        },
    )
    graph.add_edge("support_node", END)
    graph.add_edge("listing_node", END)
    graph.add_edge("wanted_node", END)
    graph.add_edge("risk_node", END)
    graph.add_edge("search_node", END)
    return graph.compile()


_graph = None


def get_graph():
    global _graph
    if _graph is None:
        _graph = build_graph()
    return _graph


def run_agents(
    message: str,
    mode: str = "auto",
    history: list[dict[str, Any]] | None = None,
    user_id: str | None = None,
) -> dict[str, Any]:
    forced: Intent | None = None
    if mode in ("support", "listing", "wanted", "risk", "search"):
        forced = mode  # type: ignore[assignment]
    # 客服/挂牌的串线纠正交给 orchestrator 语义判断，不再用关键词强切
    merged = merge_history(history, user_id)
    initial: AgentState = {
        "message": message,
        "forced_intent": forced,
        "mode": mode,
        "history": merged,
        "search_keyword": "",
        "agent_trace": [],
        "citations": [],
        "listing": None,
        "wanted": None,
        "risk": None,
        "products": [],
        "answer": "",
        "intent": "unknown",
    }
    result = get_graph().invoke(initial)
    answer = result.get("answer") or ""
    # 写入 Redis/内存近期上下文，供后续多轮综合
    store = get_context_store()
    uid = (user_id or "anonymous").strip() or "anonymous"
    store.append(uid, "user", message)
    if answer:
        store.append(uid, "assistant", answer)
    return {
        "answer": answer,
        "intent": result.get("intent") or "support",
        "citations": result.get("citations") or [],
        "listing": result.get("listing"),
        "wanted": result.get("wanted"),
        "risk": result.get("risk"),
        "products": result.get("products") or [],
        "agent_trace": result.get("agent_trace") or [],
        "llm_enabled": llm_enabled(),
        "context_backend": store.backend,
        "rag_backend": get_rag().backend,
    }
