from __future__ import annotations

import time
import uuid
from typing import Any, Literal

from fastapi import FastAPI, Header, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel, Field

from app.config import get_settings
from app.context_store import get_context_store
from app.graph_workflow import run_agents
from app.llm import llm_enabled
from app.logging_config import get_logger, log_event, setup_logging
from app.rag import get_rag
from app.rate_limit import RateLimiter
from app.stream_chat import iter_sse_events

setup_logging()
logger = get_logger("app.api")

app = FastAPI(title="Campus Xianyu AI Service", version="0.2.0")
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)

limiter = RateLimiter()


class HistoryItem(BaseModel):
    role: Literal["user", "assistant"]
    content: str = Field(min_length=1, max_length=4000)


class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=2000)
    mode: Literal["auto", "support", "listing", "wanted", "risk", "search"] = "auto"
    user_id: str | None = None
    history: list[HistoryItem] = Field(default_factory=list)


class ChatResponse(BaseModel):
    answer: str
    intent: str
    citations: list[dict[str, Any]]
    listing: dict[str, Any] | None = None
    wanted: dict[str, Any] | None = None
    risk: dict[str, Any] | None = None
    products: list[dict[str, Any]] = []
    agent_trace: list[str]
    llm_enabled: bool
    rate_limit_backend: str
    rag_backend: str | None = None
    context_backend: str | None = None
    request_id: str | None = None


@app.on_event("startup")
def on_startup() -> None:
    settings = get_settings()
    log_event(
        logger,
        "service_startup",
        model=settings.llm_model,
        llm_enabled=llm_enabled(),
        redis=get_context_store().backend,
        rag=get_rag().backend,
    )


@app.get("/health")
def health() -> dict[str, Any]:
    settings = get_settings()
    rag = get_rag()
    store = get_context_store()
    return {
        "status": "ok",
        "llm_enabled": llm_enabled(),
        "model": settings.llm_model if llm_enabled() else None,
        "rate_limit_backend": limiter.backend,
        "redis": store.backend,
        "context_backend": store.backend,
        "rag": rag.status(),
        "function_calling": True,
        "streaming": True,
        "logging": True,
        "mcp_server": "python -m app.mcp_server",
    }


@app.post("/v1/chat", response_model=ChatResponse)
def chat(
    body: ChatRequest,
    x_user_id: str | None = Header(default=None),
) -> ChatResponse:
    request_id = uuid.uuid4().hex[:12]
    user_key = (body.user_id or x_user_id or "anonymous").strip() or "anonymous"
    t0 = time.perf_counter()
    log_event(
        logger,
        "chat_start",
        request_id=request_id,
        user_id=user_key,
        mode=body.mode,
        stream=False,
        message=body.message,
        history_len=len(body.history or []),
    )
    if not limiter.allow(user_key):
        log_event(logger, "chat_rate_limited", request_id=request_id, user_id=user_key)
        raise HTTPException(status_code=429, detail="AI 调用过于频繁，请稍后再试")
    history = [{"role": h.role, "content": h.content} for h in (body.history or [])][-12:]
    try:
        result = run_agents(body.message.strip(), mode=body.mode, history=history, user_id=user_key)
    except Exception as exc:  # noqa: BLE001
        log_event(
            logger,
            "chat_error",
            request_id=request_id,
            user_id=user_key,
            mode=body.mode,
            error=str(exc),
            latency_ms=int((time.perf_counter() - t0) * 1000),
        )
        logger.exception("chat_exception request_id=%s", request_id)
        raise HTTPException(status_code=500, detail=f"AI 服务异常：{exc}") from exc

    latency_ms = int((time.perf_counter() - t0) * 1000)
    log_event(
        logger,
        "chat_ok",
        request_id=request_id,
        user_id=user_key,
        mode=body.mode,
        stream=False,
        intent=result.get("intent"),
        latency_ms=latency_ms,
        answer_len=len(result.get("answer") or ""),
        citation_count=len(result.get("citations") or []),
        product_count=len(result.get("products") or []),
        agent_trace=result.get("agent_trace") or [],
        rag_backend=result.get("rag_backend"),
        context_backend=result.get("context_backend"),
        risk_suggestion=(result.get("risk") or {}).get("suggestion") if result.get("risk") else None,
    )
    return ChatResponse(
        answer=result["answer"],
        intent=result["intent"],
        citations=result["citations"],
        listing=result.get("listing"),
        wanted=result.get("wanted"),
        risk=result.get("risk"),
        products=result.get("products") or [],
        agent_trace=result["agent_trace"],
        llm_enabled=result["llm_enabled"],
        rate_limit_backend=limiter.backend,
        rag_backend=result.get("rag_backend"),
        context_backend=result.get("context_backend"),
        request_id=request_id,
    )


@app.post("/v1/chat/stream")
def chat_stream(
    body: ChatRequest,
    x_user_id: str | None = Header(default=None),
):
    request_id = uuid.uuid4().hex[:12]
    user_key = (body.user_id or x_user_id or "anonymous").strip() or "anonymous"
    t0 = time.perf_counter()
    log_event(
        logger,
        "chat_start",
        request_id=request_id,
        user_id=user_key,
        mode=body.mode,
        stream=True,
        message=body.message,
        history_len=len(body.history or []),
    )
    if not limiter.allow(user_key):
        log_event(logger, "chat_rate_limited", request_id=request_id, user_id=user_key, stream=True)
        raise HTTPException(status_code=429, detail="AI 调用过于频繁，请稍后再试")
    history = [{"role": h.role, "content": h.content} for h in (body.history or [])][-12:]

    def event_gen():
        import json

        final_meta: dict[str, Any] = {}
        try:
            for chunk in iter_sse_events(
                body.message.strip(),
                mode=body.mode,
                history=history,
                user_id=user_key,
            ):
                if chunk.startswith("event: final"):
                    for line in chunk.splitlines():
                        if line.startswith("data:"):
                            try:
                                data = json.loads(line[5:].strip())
                                final_meta = {
                                    "intent": data.get("intent"),
                                    "answer_len": len(data.get("answer") or ""),
                                    "agent_trace": data.get("agent_trace") or [],
                                    "rag_backend": data.get("rag_backend"),
                                    "risk_suggestion": (data.get("risk") or {}).get("suggestion")
                                    if data.get("risk")
                                    else None,
                                    "product_count": len(data.get("products") or []),
                                }
                            except Exception:
                                pass
                            break
                yield chunk
            log_event(
                logger,
                "chat_ok",
                request_id=request_id,
                user_id=user_key,
                mode=body.mode,
                stream=True,
                latency_ms=int((time.perf_counter() - t0) * 1000),
                **final_meta,
            )
        except Exception as exc:  # noqa: BLE001
            log_event(
                logger,
                "chat_error",
                request_id=request_id,
                user_id=user_key,
                mode=body.mode,
                stream=True,
                error=str(exc),
                latency_ms=int((time.perf_counter() - t0) * 1000),
            )
            logger.exception("chat_stream_exception request_id=%s", request_id)
            payload = json_dumps_error(str(exc))
            yield f"event: error\ndata: {payload}\n\n"

    return StreamingResponse(
        event_gen(),
        media_type="text/event-stream",
        headers={
            "Cache-Control": "no-cache",
            "Connection": "keep-alive",
            "X-Accel-Buffering": "no",
            "X-Request-Id": request_id,
        },
    )


def json_dumps_error(message: str) -> str:
    import json

    return json.dumps({"message": message}, ensure_ascii=False)
