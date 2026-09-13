from __future__ import annotations

import json
from typing import Any

from langchain_core.tools import tool

from app.rag import get_rag
from app.tools import search_products


@tool
def search_campus_products(keyword: str) -> str:
    """在校园咸鱼首页搜索已发布的在售商品。当用户想买、想找、询问是否有某类闲置在售时必须调用。

    Args:
        keyword: 简短中文搜索词，例如「手机」「高数」「台灯」
    """
    products = search_products((keyword or "").strip(), size=6)
    payload = {
        "keyword": keyword,
        "count": len(products),
        "products": products,
    }
    return json.dumps(payload, ensure_ascii=False)


@tool
def retrieve_faq(query: str) -> str:
    """从校园咸鱼官方 FAQ 知识库检索条目。回答认证、发布、订单、退货、私聊、举报、AI能力等平台规则前必须先调用。

    Args:
        query: 用户原问题或检索问句
    """
    hits = get_rag().retrieve((query or "").strip(), top_k=3)
    citations = [
        {"title": h.title, "content": h.content, "score": round(h.score, 4)} for h in hits
    ]
    payload = {
        "hit_count": len(citations),
        "citations": citations,
    }
    return json.dumps(payload, ensure_ascii=False)


@tool
def risk_precheck(title: str, description: str = "") -> str:
    """对校园二手商品标题与描述做风控预审，返回风险等级与建议。

    Args:
        title: 商品标题
        description: 商品描述，可为空
    """
    text = f"{title}\n{description}".strip()
    text_lower = text.lower()
    high = ["诈骗", "假货", "违禁", "枪支", "毒品", "色情"]
    # 辱骂/人身攻击：命中即拒绝，避免当作普通闲置文案
    abuse = [
        "傻逼",
        "傻b",
        "傻B",
        "煞笔",
        "骚逼",
        "骚b",
        "骚B",
        "脑残",
        "智障",
        "白痴",
        "去死",
        "操你",
        "草你",
        "他妈的",
        "他妈",
        "垃圾货",
        "垃圾人",
        "狗东西",
        "混蛋",
        "王八",
        "贱人",
        "婊子",
        "nmsl",
        "cnm",
        "sb",
    ]
    medium = ["虚假", "高仿", "刷单", "加微信", "转账", "代考"]
    risk_level = "NONE"
    suggestion = "PASS"
    reasons: list[str] = []

    for word in abuse:
        needle = word.lower() if word.isascii() else word
        hay = text_lower if word.isascii() else text
        if needle in hay:
            risk_level = "HIGH"
            suggestion = "REJECT"
            reasons.append(f"命中辱骂/不当用语：{word}")
            break

    if risk_level == "NONE":
        for word in high:
            if word in text:
                risk_level = "HIGH"
                suggestion = "REJECT"
                reasons.append(f"命中高风险词：{word}")
                break
    if risk_level == "NONE":
        for word in medium:
            if word in text:
                risk_level = "MEDIUM"
                suggestion = "REVIEW"
                reasons.append(f"命中需复核词：{word}")
                break
    if risk_level == "NONE" and len(title.strip()) < 2:
        risk_level = "LOW"
        suggestion = "REVIEW"
        reasons.append("标题过短")
    elif risk_level == "NONE" and len(description.strip()) < 10:
        risk_level = "LOW"
        suggestion = "REVIEW"
        reasons.append("描述过短")
    payload = {
        "risk_level": risk_level,
        "suggestion": suggestion,
        "reason": "；".join(reasons) if reasons else "未发现明显风险",
    }
    return json.dumps(payload, ensure_ascii=False)


SUPPORT_TOOLS = [retrieve_faq, search_campus_products]
SEARCH_TOOLS = [search_campus_products]
RISK_TOOLS = [risk_precheck]


def parse_tool_payload(raw: str) -> dict[str, Any]:
    try:
        data = json.loads(raw)
        return data if isinstance(data, dict) else {}
    except Exception:
        return {}
