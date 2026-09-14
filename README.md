# 校园咸鱼（Campus Xianyu）

校园二手交易 Web：完整交易业务 + 独立 AI 服务。用 **LangGraph Multi-Agent** 做客服 RAG、找货、挂牌/求购文案、风控预审；Java 鉴权网关 + **SSE 流式**；已公网部署并可演示。

**仓库：** [github.com/duoliyangzi/campus-xianyu](https://github.com/duoliyangzi/campus-xianyu)

> 定位：业务侧 AI 应用落地（Multi-Agent + RAG + Tool）

---

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 · Vite · Axios |
| 业务后端 | Spring Boot · JPA · JWT |
| 数据 | MySQL 8 · Redis（限流 / 近期 AI 上下文） |
| AI 服务 | FastAPI · LangGraph · Function Calling · RAG（Chroma + Embedding，失败回退 TF-IDF） |
| 模型 | 通义千问（DashScope OpenAI 兼容，当前 `qwen-turbo`） |
| 网关与部署 | Nginx（`/api/ai/` 关缓冲保 SSE）· systemd · Docker 起 MySQL/Redis · 腾讯云轻量 |

补充能力：会话 MySQL 落库、MCP 暴露同套工具、自建 easy/hard 评测集。

---

## 能做什么

**交易业务**  
注册登录、实名、商品挂牌/求购、浏览筛选、私聊、订单双方确认、举报与管理审核。

**AI 助手（「我的」页）**

| 能力 | 说明 |
|------|------|
| 意图路由 | LLM 语义主判 support / search / listing / wanted / risk；失败关键词兜底 |
| 客服 RAG | FAQ 切片检索 + 历史上下文；流式 SSE 回答 |
| 找货 | Function Calling 调 Java 商品接口，返回可点卡片 |
| 挂牌 / 求购 | 专用 Agent 生成结构化 JSON，可填入发布页 |
| 风控预审 | 关键词硬拦 + LLM 语义；管理端可复用建议，上架仍人工 |

架构要点：浏览器只打 Java；Spring Boot 鉴权后转发 FastAPI；搜商品时 AI 再回调业务 API。密钥不出前端。

---

## 架构

```text
浏览器 (Vue3)
    ↓
Nginx :80
    ├─ /            前端静态
    ├─ /api /uploads → Spring Boot :8080  （JWT · 业务 · AI 网关 · 会话落库）
    └─ /api/ai/*       同上（SSE 关缓冲）
                           ↓
                     FastAPI :8000
                           ├─ LangGraph 编排
                           ├─ 通义千问
                           ├─ Chroma RAG / Redis
                           └─ Tool → Java /api/products
                           ↓
                     MySQL · Redis (Docker)
```

请求链路（流式）：前端 → `/api/ai/chat/stream` → Java 鉴权写库 → FastAPI 合并历史 / 限流 / 意图路由 / 专家节点 → SSE（meta → token* → final）→ Java 落助手回复 → 前端拼字。

---

## 评测结果

自建集 `ai-service/eval/cases.json`（2026-09-13）：

| 指标 | easy | hard | all |
|------|------|------|-----|
| 意图准确率 | 100% (15) | 91.7% (12) | **96.3% (27)** |
| RAG Hit@1 | 87.5% | 85.7% | **~87%** |
| RAG Hit@3 | 100% | 100% | **100%** |
| 风控一致率（小样本） | 100% (8) | 100% (8) | 100% (16) |

```bash
cd ai-service && source .venv/bin/activate   # Windows: .\.venv\Scripts\Activate.ps1
python -m eval.run_eval
python -m eval.run_eval --difficulty hard
```

---

## 仓库结构

```text
frontend/          Vue3 H5
backend/           Spring Boot 业务与 AI 网关
ai-service/        FastAPI · LangGraph · RAG · 评测 · MCP
sql/               建表、种子与增量（含 AI 会话表）
deploy/            Nginx · systemd 示例
docs/              部署与实现说明
docker-compose.yml           MySQL
docker-compose.redis.yml     Redis
```

---

## 本地快速启动

**依赖：** JDK 17+、Node 18+、Python 3.11+、Docker（MySQL/Redis）或本机等价服务。

```bash
# 1) 库
docker compose up -d
docker compose -f docker-compose.redis.yml up -d
# 按需执行 sql/01_schema.sql、02_seed_data.sql 及后续增量（含 07_ai_chat.sql）

# 2) 业务后端（默认注意端口与 application 配置）
cd backend && ./mvnw spring-boot:run

# 3) AI 服务
cd ai-service
cp .env.example .env   # 填写 LLM_API_KEY、JAVA_API_BASE、REDIS_URL
python -m venv .venv && source .venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 127.0.0.1 --port 8000

# 4) 前端
cd frontend && npm ci && npm run dev
```

生产部署见 [docs/production-deploy.md](docs/production-deploy.md)。AI 实现细节见 [docs/ai-feature-implementation-2026-09-12.md](docs/ai-feature-implementation-2026-09-12.md)。

---

## 演示账号与安全

- 展示数据脚本：`sql/08_showcase_demo.sql`（演示账号密码以脚本/运维说明为准，上线请改默认口令）。
---

## License

仅供学习与求职展示；商用请自行评估依赖协议与平台合规要求。
