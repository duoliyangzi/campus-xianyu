# 校园咸鱼 · AI 能力落地说明（今日增量）

> 日期：2026-09-12  
> 分支：`feature/ai-multi-agent`  
> 文档目的：汇总 AI 功能、技术栈、实现方式，以及对齐秋招「AI 应用开发」岗位 JD 的程度。  
> 相关材料：`docs/ai-resume-plan-b.md`、`docs/campus-xianyu-ops-manual.md`、`ai-service/knowledge/faq.md`

---

## 1. 一句话总结

在原有 **Vue3 + Spring Boot + MySQL** 校园二手业务之上，按方案 B 新增独立 **FastAPI AI 服务**：用 **LangGraph Multi-Agent** 编排客服 / 挂牌 / 求购 / 风控 / 搜商品；客服通过 **Function Calling + RAG（Chroma 向量库 / TF-IDF 兜底）**；对话 **SSE 流式输出** 并 **落 MySQL**；限流与近期上下文支持 **Redis**（无 Redis 时 memory 兜底）；同一套工具另用 **MCP Server** 暴露。

**不做多模态**（按明确要求）。

---

## 2. 功能清单

### 2.1 产品侧

| 能力 | 说明 |
|------|------|
| AI 客服入口 | 「我的」页固定 AI 区 + 对话面板 |
| 多模式引导 | 自动 / 客服 / 挂牌 / 求购 / 风控；placeholder 随模式变化 |
| 流式回答 | `/api/ai/chat/stream` SSE，气泡逐字追加 |
| 多轮上下文 | 请求 history + Redis/内存近期上下文 + RAG 片段一并注入 LLM |
| 客服 RAG | `##` 切片 + overlap；Chroma + 通义 Embedding；失败回退 TF-IDF |
| 搜商品卡片 | 找货意图 → Function Calling → 可点进详情 |
| 挂牌 / 求购 | 分别填入「发布页」「求购页」 |
| 风控预审 | PASS / REVIEW / REJECT |
| 会话持久化 | MySQL `ai_chat_session` / `ai_chat_message`；打开面板从服务端加载 |

### 2.2 管理侧

| 能力 | 说明 |
|------|------|
| AI 风控审核 | 管理端调用 Risk Agent，写入 `ai_audit_log` |
| 人工兜底 | 最终上架/拒绝仍由管理员决定 |

### 2.3 工程侧

| 能力 | 说明 |
|------|------|
| Java AI 网关 | `/api/ai/chat`、`/chat/stream`、`/chat/history`、`/health` |
| 增量 SQL | `sql/07_ai_chat.sql` |
| Redis | 限流 + `ai:ctx:{userId}` 近期对话；`docker-compose.redis.yml` |
| MCP | `python -m app.mcp_server` |

---

## 3. 技术栈

| 层级 | 技术 | 用途 |
|------|------|------|
| 前端 | Vue 3 + Vite | 流式气泡、模式引导、商品卡片 |
| 业务后端 | Spring Boot + JPA + JWT + MySQL | 鉴权、落库、SSE 透传 |
| AI 服务 | FastAPI + Uvicorn | `/v1/chat`、`/v1/chat/stream` |
| 编排 | LangGraph | support / listing / wanted / risk / search |
| RAG | Chroma + Embedding（通义）/ TF-IDF | 向量检索 + 降级 |
| 缓存/限流 | Redis 或 memory | 限流、会话上下文 |
| LLM | 通义 `qwen-plus`（DashScope 兼容） | 推理与 FC |

简历一行：`Vue3 · Spring Boot · MySQL · FastAPI · LangGraph · Function Calling · RAG(Chroma) · SSE · Redis · MCP · 通义千问`

---

## 4. 架构与关键路径

```text
Vue ──JWT──► Spring /api/ai/chat/stream
               │ 写 user 消息 → MySQL
               │ 透传 SSE
               ▼
            FastAPI /v1/chat/stream
               │ Redis 上下文（可选）
               ▼
            LangGraph Orchestrator
               ├─ Support：预检索 RAG + FC(retrieve_faq/search)
               ├─ Search / Listing / Wanted / Risk
               └─ SSE: meta → token* → final → done
               │
               ▼
            Java 解析 final → 写 assistant 消息 → MySQL
```

### RAG

1. `faq.md` 按 `##` 切段，超长再按 ~400 字 / overlap ~80 切片  
2. Embedding 写入 Chroma（`.chroma/`，gitignore）  
3. 检索：向量 + TF-IDF 融合；无 Embedding/Chroma 失败则纯 TF-IDF  
4. Support：【对话历史】+【知识片段】+【当前问题】拼进 system

### 本地端口

| 服务 | 端口 |
|------|------|
| 前端 | 5173 |
| 后端 | 8081 |
| AI | 8000 |
| MySQL | 3306 |
| Redis（可选） | 6379 |

Redis：`docker compose -f docker-compose.redis.yml up -d`，并在 `ai-service/.env` 配置 `REDIS_URL=redis://127.0.0.1:6379/0`。本机无 Docker 时自动 memory 兜底。

会话表：执行 `sql/07_ai_chat.sql`。

---

## 5. JD 对齐

| JD 要求 | 覆盖 |
|---------|------|
| 大模型应用 / Prompt | ✅ |
| Function Calling | ✅ |
| RAG + 向量库 | ✅ Chroma；TF-IDF 兜底 |
| Multi-Agent | ✅ LangGraph |
| 流式输出 | ✅ SSE |
| 会话落库 | ✅ MySQL |
| Redis 工程化 | ✅ 限流+上下文（可降级） |
| MCP | ✅ |
| 多模态 | ❌ 明确不做 |

---

## 5.1 小评测集结果（2026-09-13，可写简历）

用例：`ai-service/eval/cases.json`；跑分：`python -m eval.run_eval`。

| 指标 | easy | hard | all |
|------|------|------|-----|
| 意图准确率 | 100% (15) | 91.7% (12) | 96.3% (27) |
| 检索词命中 | 100% | 100% | 100% |
| RAG Hit@1 | 87.5% | 85.7% | 86.7% |
| RAG Hit@3 | 100% | 100% | 100% |
| 风控一致率 | 100% (8) | 100% (8) | 100% (16) |

Hard 意图 miss 示例：多意图句「我要卖耳机，顺便看看有没有同款」标 listing、模型判 search。  
简历建议写约数并分档：overall ~96%，hard ~92%；勿只报 100%。

---

## 6. 关键文件

| 路径 | 作用 |
|------|------|
| `ai-service/app/rag.py` / `embeddings.py` | 切片、Chroma、Embedding |
| `ai-service/app/stream_chat.py` / `main.py` | SSE |
| `ai-service/app/context_store.py` / `prompting.py` | Redis 上下文、Prompt 拼装 |
| `sql/07_ai_chat.sql` | 会话表 |
| `backend/.../ai/AiChatHistoryService.java` | 落库 / 拉取 |
| `frontend/src/App.vue` | 流式 UI、引导文案、拉历史 |
| `docker-compose.redis.yml` | Redis |

---

*以仓库当前代码为准。*
