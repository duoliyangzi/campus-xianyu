# 校园咸鱼 × AI 应用 · 简历与面试材料（方案 B）

> 架构选型：**Vue3 + Spring Boot（业务中台）+ FastAPI（AI / Multi-Agent）+ Redis（限流与缓存）**  
> 不做多模态；聚焦 RAG 客服、文本智能挂牌、风控 Agent、Multi-Agent 编排。

---

## 1. 简历项目标题（任选其一）

- **校园二手交易平台（Multi-Agent 智能助手与 RAG 客服）**
- **Campus Xianyu：基于 LangGraph 的交易场景 Multi-Agent 系统**

副标题可写：`Vue3 · Spring Boot · FastAPI · LangGraph · RAG · Redis · MySQL`

---

## 2. 技术栈一行词（直接贴简历）

**Vue 3 + Vite，Spring Boot 4（JWT / JPA / MySQL），FastAPI + LangGraph（Multi-Agent / Tool Calling），RAG（Embedding + 向量检索），Redis（LLM 限流与热点缓存），Nginx 部署。**

更短版：

**Java 业务中台 + Python Agent 服务；LangGraph 多角色协作；RAG 知识库问答；Redis 限流。**

---

## 3. 架构图（面试可手画）

```text
                    ┌─────────────┐
                    │  Vue3 H5    │
                    │ AI 助手入口  │
                    └──────┬──────┘
                           │ /api
                           ▼
              ┌────────────────────────┐
              │  Spring Boot（Java）    │
              │  用户/商品/订单/审核    │
              │  JWT 鉴权 · 业务工具API │
              └───────────┬────────────┘
                    │              │
         业务读写 MySQL      调用 AI 服务
                    │              │
                    ▼              ▼
              ┌─────────┐   ┌──────────────────────────┐
              │  MySQL  │   │  FastAPI（Python）         │
              └─────────┘   │  Orchestrator（LangGraph） │
                            │  ├─ SupportAgent（RAG）    │
                            │  ├─ ListingAgent（文案）   │
                            │  └─ RiskAgent（风控）      │
                            │  → LLM API（OpenAI 兼容）  │
                            └───────────┬────────────────┘
                                        │
                              ┌─────────┴─────────┐
                              │ Redis：限流/缓存   │
                              │ 向量库：FAQ/文档   │
                              └───────────────────┘
```

**分工一句话**：Java 管「交易事实」；Python 管「推理与编排」；通过 HTTP + 内部工具 API 协作。

---

## 4. 模块与职责（对应 Multi-Agent）

| Agent | 职责 | 主要 Tools（由 Java 提供或 Python 本地） |
|-------|------|----------------------------------------|
| **Orchestrator** | 意图识别与路由（客服 / 挂牌 / 风控） | 无，或轻量分类 |
| **SupportAgent** | 校园交易 FAQ、流程问答（RAG） | 知识库检索；可选查订单状态 |
| **ListingAgent** | 根据用户文本生成标题/描述/分类建议（JSON） | 查分类字典 |
| **RiskAgent** | 发布前风险判断 PASS/REVIEW/REJECT | 关键词规则、历史举报、写审核日志 |

流程示例（发布）：

`ListingAgent 生成文案 → RiskAgent 审核 → 不通过则修订建议 → 通过则 Java 落库商品`

流程示例（客服）：

`Orchestrator → SupportAgent（RAG）→ 带引用回答；涉及订单则 Tool 调 Java`

---

## 5. 简历项目描述（可复制改数字）

**校园二手交易平台（AI 增强）**｜个人 / 团队项目｜2026.xx – 至今

- 基于 **Spring Boot** 实现校园二手核心业务（认证、商品、求购、私聊、订单双方确认、管理端审核）；前端 **Vue3** H5。
- 独立 **FastAPI + LangGraph** 构建 **Multi-Agent**：调度 Agent 路由意图，客服 / 挂牌文案 / 风控专家 Agent 分工协作；客服与找货通过 **Function Calling（bind_tools / tool_calls）** 调用 FAQ 检索与商品搜索工具，风控同样以工具调用落审计。
- 同一套业务工具另以 **MCP Server** 暴露，便于 Cursor 等客户端调试与复用。
- 实现 **RAG 智能客服**：文档切片与向量/字串检索，回答附来源片段，未命中则拒答以降幻觉。
- 风控链路采用「Function Calling 风控工具 + LLM 说明 + 人工审核兜底」，决策写入审计日志，对接管理端商品审核。
- 使用 **Redis（可选）** 对 LLM 调用按用户限流；未配置时内存限流。密钥环境变量注入，支持 OpenAI Compatible（如通义千问）切换。

（评测 2026-09-13：意图 easy 100% / hard ~92% / all ~96%；RAG Hit@1 ~87%、Hit@3 100%；风控小样本一致率 100%。详见 `docs/ai-feature-implementation-2026-09-12.md`。）

---

## 6. 面试 1 分钟介绍稿

> 我做的是一个校园二手交易平台，业务用 Java Spring Boot，包括实名、商品、私聊和双方确认的订单。为了做 AI 应用，我把推理层拆成 Python FastAPI 服务：用 LangGraph 做 Multi-Agent，一个调度 Agent 判断用户是要咨询、生成挂牌文案还是过风控，再交给对应的专家 Agent。客服用了 RAG，把交易规则和 FAQ 放进向量库，回答会带引用。风控是规则加 LLM，并和原来的审核后台打通。Java 继续负责鉴权和写库，Python 通过内部 API 调工具。另外用 Redis 做了调用限流和缓存。我主要想体现的是：大模型怎么嵌进真实业务，而不是只调一个聊天接口。

背的时候抓住三句：**业务在 Java、智能在 Python Agent、用工具和审计闭环。**

---

## 7. 面试高频追问（提前准备）

| 问题 | 答法要点 |
|------|----------|
| 为什么要 Multi-Agent？ | 提示词、工具、失败策略不同；拆开比一个超级 Prompt 好维护 |
| 为什么不全用 Python？ | 交易一致性、JWT、已有模块在 Java；AI 变更频繁适合独立服务 |
| 怎么防幻觉？ | RAG 强制引用；无检索结果拒答；风控关键操作人工兜底 |
| Agent 失败怎么办？ | 超时、降级规则引擎、返回「转人工」 |
| Redis 干什么？ | LLM 限流、检索缓存，不是为了用 Redis 而用 |
| 和 LangChain 官网 Demo 区别？ | 对接真实订单/审核、鉴权、日志、与 H5 产品流程绑定 |

---

## 8. 建议落地顺序（匹配本材料）

| 阶段 | 产出 |
|------|------|
| 1 | FastAPI 骨架 + 健康检查；Java `RestClient` 调通 |
| 2 | SupportAgent + RAG（10～20 条 FAQ）+ 前端「AI 助手」 |
| 3 | Orchestrator + ListingAgent + RiskAgent 发布前链路 |
| 4 | Redis 限流；审计日志；补评测与简历数字 |
| 5 | （可选）Docker Compose 增加 `ai-service` + `redis` |

---

## 9. 简历「技术栈」栏示例

`Java / Spring Boot / MySQL / JWT / Vue3 / Python / FastAPI / LangGraph / RAG / Redis / Docker / Nginx`

---

*本文档仅作求职材料提纲；实现时以仓库代码与真实评测数据为准。*
