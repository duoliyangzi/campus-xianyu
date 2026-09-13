from __future__ import annotations

import json
from typing import Any

from app.agent_tools import parse_tool_payload, risk_precheck
from app.llm import chat_text, extract_json, llm_enabled

_SEVERITY = {"NONE": 0, "LOW": 1, "MEDIUM": 2, "HIGH": 3}
_ACTION = {"PASS": 0, "REVIEW": 1, "REJECT": 2}


def _stricter(a: dict[str, Any] | None, b: dict[str, Any] | None) -> dict[str, Any]:
    left = a or {}
    right = b or {}
    level = left.get("risk_level") or "NONE"
    other = right.get("risk_level") or "NONE"
    if _SEVERITY.get(str(other), 0) > _SEVERITY.get(str(level), 0):
        level = other
    sug = left.get("suggestion") or "PASS"
    sug2 = right.get("suggestion") or "PASS"
    if _ACTION.get(str(sug2), 0) > _ACTION.get(str(sug), 0):
        sug = sug2
    reasons = []
    for item in (left.get("reason"), right.get("reason")):
        t = str(item or "").strip()
        if t and t not in reasons and t != "未发现明显风险":
            reasons.append(t)
    cats: list[str] = []
    for src in (left, right):
        for c in src.get("categories") or []:
            if c not in cats:
                cats.append(str(c))
    return {
        "risk_level": level,
        "suggestion": sug,
        "reason": "；".join(reasons) if reasons else "未发现明显风险",
        "categories": cats,
    }


def _is_content_blocked_error(exc: BaseException) -> bool:
    text = str(exc).lower()
    return any(
        k in text
        for k in (
            "data_inspection_failed",
            "inappropriate content",
            "inappropriate_content",
            "content_filter",
            "responsibleai",
        )
    )


def assess_content_risk(
    message: str,
    *,
    history: list[dict[str, Any]] | None = None,
) -> dict[str, Any]:
    """关键词硬规则 + 大模型语义审核；取更严结果。

    注意：通义等云厂商会对辱骂输入直接拦截（data_inspection_failed），
    此时大模型无法返回结论，必须把拦截本身视为 REJECT。
    """
    text = (message or "").strip()
    raw = risk_precheck.invoke({"title": text[:40], "description": text})
    keyword = parse_tool_payload(raw)

    # 关键词已拒绝：无需再调 LLM（也常会被云厂商内容安检拦下）
    if str(keyword.get("suggestion") or "").upper() == "REJECT":
        keyword["categories"] = list(keyword.get("categories") or []) + ["abuse"]
        from app.logging_config import get_logger, log_event

        log_event(
            get_logger("app.risk"),
            "risk_keyword_reject",
            suggestion=keyword.get("suggestion"),
            risk_level=keyword.get("risk_level"),
            reason=str(keyword.get("reason") or "")[:120],
        )
        return keyword

    if not llm_enabled():
        return keyword

    try:
        llm_raw = chat_text(
            "你是校园咸鱼内容风控审核员。【风控专用】对用户要发布的标题/描述做语义审核。"
            "不要帮忙写出售/求购文案，不要去搜商品。"
            "只输出 JSON，不要其他文字："
            '{"risk_level":"NONE|LOW|MEDIUM|HIGH","suggestion":"PASS|REVIEW|REJECT",'
            '"reason":"","categories":["abuse|fraud|forbidden|sexual|violence|too_short|ok"]}'
            "审核规则（按优先级）："
            "1) 辱骂、脏话、人身攻击、低俗淫秽用语（含谐音/变体/短句）→ "
            'suggestion=REJECT, risk_level=HIGH, categories 含 "abuse"；'
            "2) 诈骗导流、违禁、色情暴力 → REJECT 或 REVIEW；"
            "3) 正常二手交易描述 → PASS，categories 含 ok；"
            "4) 仅信息过短且完全无不当内容 → REVIEW + LOW，categories 含 too_short；"
            "重要：绝不能因为句子短就忽略辱骂；有辱骂就必须 REJECT。",
            text,
            history=history,
        )
    except Exception as exc:  # noqa: BLE001
        if _is_content_blocked_error(exc):
            blocked = {
                "risk_level": "HIGH",
                "suggestion": "REJECT",
                "reason": "内容触发平台安全审核（疑似辱骂/不当用语），禁止发布",
                "categories": ["abuse"],
            }
            from app.logging_config import get_logger, log_event

            log_event(get_logger("app.risk"), "risk_provider_blocked", error=str(exc)[:160])
            return _stricter(keyword, blocked)
        from app.logging_config import get_logger, log_event

        log_event(get_logger("app.risk"), "risk_llm_fallback", error=str(exc)[:160])
        return keyword

    llm_risk = extract_json(llm_raw) or {}
    level = str(llm_risk.get("risk_level") or "REVIEW").upper()
    if level not in _SEVERITY:
        level = "REVIEW"
    sug = str(llm_risk.get("suggestion") or "REVIEW").upper()
    if sug not in _ACTION:
        sug = "REVIEW"
    llm_norm = {
        "risk_level": level,
        "suggestion": sug,
        "reason": str(llm_risk.get("reason") or "模型语义审核完成").strip(),
        "categories": list(llm_risk.get("categories") or []),
    }
    merged = _stricter(keyword, llm_norm)
    from app.logging_config import get_logger, log_event

    log_event(
        get_logger("app.risk"),
        "risk_assessed",
        suggestion=merged.get("suggestion"),
        risk_level=merged.get("risk_level"),
        categories=merged.get("categories") or [],
    )
    return merged


def format_risk_answer(risk: dict[str, Any]) -> str:
    cats = risk.get("categories") or []
    abuse = "abuse" in cats or any(
        k in str(risk.get("reason") or "") for k in ("辱骂", "不当", "安全审核")
    )
    head = f"风控预审结果：{risk.get('suggestion')} / {risk.get('risk_level')}"
    reason = risk.get("reason") or "未发现明显风险"
    if abuse or risk.get("suggestion") == "REJECT":
        return (
            f"{head}\n"
            f"说明：{reason}\n"
            "该内容不宜发布。请删除辱骂、低俗或违规表述后重新提交。"
            "最终上架仍以管理员人工审核为准。"
        )
    return (
        f"{head}\n"
        f"说明：{reason}\n"
        "最终上架仍以管理员人工审核为准。"
    )
