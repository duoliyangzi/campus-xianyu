# Campus Xianyu AI Service（方案 B）

FastAPI + LangGraph Multi-Agent + Function Calling + RAG(Chroma) + SSE 流式 + Redis 上下文 + MCP。

## 启动 API

```powershell
cd ai-service
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
copy .env.example .env
# 编辑 .env，填写 LLM_API_KEY；可选 REDIS_URL
uvicorn app.main:app --host 0.0.0.0 --port 8000
```

健康检查：http://127.0.0.1:8000/health  
可见 `function_calling`、`streaming`、`rag.backend`（`chroma`/`tfidf`）、`redis`、`logging`。

## 日志（工程化）

- 控制台 + 滚动文件：`ai-service/logs/ai-service.log`
- 一行一条 JSON：`chat_start` / `chat_ok` / `chat_error` / `chat_rate_limited` / `risk_*`
- 字段含：`request_id`、`user_id`、`mode`、`intent`、`latency_ms`、`agent_trace` 等（用户原文仅截断前 80 字）
- 环境变量：`LOG_LEVEL=INFO`，`LOG_DIR=logs`

可选 Redis：

```powershell
docker compose -f ../docker-compose.redis.yml up -d
# .env: REDIS_URL=redis://127.0.0.1:6379/0
```

会话落库（Java 侧）：执行 `sql/07_ai_chat.sql`。

## Function Calling

| Tool | 作用 |
|------|------|
| `retrieve_faq` | RAG 检索 FAQ |
| `search_campus_products` | 搜在售商品 |
| `risk_precheck` | 文案风控预审 |

## RAG

- 知识库：`knowledge/faq.md`
- 切片：`##` + 定长 overlap
- 向量：Chroma（`.chroma/`）+ 通义 Embedding；失败回退 TF-IDF

## 接口

`POST /v1/chat` — 非流式 JSON  
`POST /v1/chat/stream` — SSE（`meta` / `token` / `final` / `done`）

```json
{ "message": "如何实名认证？", "mode": "auto", "user_id": "1", "history": [] }
```

`mode`：`auto` | `support` | `listing` | `wanted` | `risk` | `search`

## MCP（可选）

```powershell
.\.venv\Scripts\python.exe -m app.mcp_server
```
