from __future__ import annotations

import json
import logging
import sys
from datetime import datetime, timezone
from logging.handlers import RotatingFileHandler
from pathlib import Path
from typing import Any

from app.config import get_settings

_configured = False


class JsonLineFormatter(logging.Formatter):
    """一行一条 JSON，方便检索与后续接入采集。"""

    def format(self, record: logging.LogRecord) -> str:
        payload: dict[str, Any] = {
            "ts": datetime.now(timezone.utc).isoformat(),
            "level": record.levelname,
            "logger": record.name,
            "msg": record.getMessage(),
        }
        extra = getattr(record, "extra_fields", None)
        if isinstance(extra, dict):
            payload.update(extra)
        if record.exc_info:
            payload["exc"] = self.formatException(record.exc_info)
        return json.dumps(payload, ensure_ascii=False)


def setup_logging() -> None:
    global _configured
    if _configured:
        return
    settings = get_settings()
    level_name = (settings.log_level or "INFO").upper()
    level = getattr(logging, level_name, logging.INFO)

    root = logging.getLogger()
    root.handlers.clear()
    root.setLevel(level)

    formatter = JsonLineFormatter()

    console = logging.StreamHandler(sys.stdout)
    console.setLevel(level)
    console.setFormatter(formatter)
    root.addHandler(console)

    log_dir = Path(settings.log_dir)
    if not log_dir.is_absolute():
        log_dir = Path(__file__).resolve().parent.parent / log_dir
    log_dir.mkdir(parents=True, exist_ok=True)
    file_handler = RotatingFileHandler(
        log_dir / "ai-service.log",
        maxBytes=5 * 1024 * 1024,
        backupCount=5,
        encoding="utf-8",
    )
    file_handler.setLevel(level)
    file_handler.setFormatter(formatter)
    root.addHandler(file_handler)

    # 降噪第三方库
    logging.getLogger("httpx").setLevel(logging.WARNING)
    logging.getLogger("httpcore").setLevel(logging.WARNING)
    logging.getLogger("chromadb").setLevel(logging.WARNING)
    logging.getLogger("chromadb.telemetry").setLevel(logging.ERROR)
    logging.getLogger("uvicorn.access").setLevel(logging.INFO)

    _configured = True
    logging.getLogger("app").info(
        "logging_ready",
        extra={"extra_fields": {"log_dir": str(log_dir), "level": level_name}},
    )


def get_logger(name: str = "app") -> logging.Logger:
    if not _configured:
        setup_logging()
    return logging.getLogger(name)


def log_event(logger: logging.Logger, msg: str, **fields: Any) -> None:
    """结构化业务事件。"""
    # 避免把超长用户原文打进日志
    safe = dict(fields)
    if "message" in safe and isinstance(safe["message"], str):
        text = safe["message"]
        safe["message"] = text[:80] + ("…" if len(text) > 80 else "")
        safe["message_len"] = len(text)
    logger.info(msg, extra={"extra_fields": safe})
