# 校园版咸鱼 · 整体架构与文件说明

> 项目：Campus Xianyu（校园二手交易 H5）  
> 仓库：https://github.com/duoliyangzi/campus-xianyu  
> 文档用途：说明技术栈、前后端职责、连接方式、目录与主要文件作用

---

## 1. 项目是什么

面向本校学生的 **二手交易 Web App（移动端 H5 优先）**，支持：

- 学生注册 / 登录 / 实名认证
- 商品发布、搜索、审核、上下架
- 求购发布与筛选
- 商品留言、一对一私聊
- 线下交易订单（双方确认）
- 举报、用户管理、分类管理
- 关键词辅助审核

三人协作分工：

| 成员 | 主要模块 |
|------|----------|
| A | 注册登录、实名认证、商品增删改查 |
| B | 商品/求购搜索筛选分页、拼音搜索 |
| C | 留言、私聊、订单、举报、管理后台、关键词审核、部署 |

---

## 2. 技术栈

| 层级 | 技术 | 说明 |
|------|------|------|
| 前端 | **Vue 3** + **Vite 8** | 单页 H5；业务几乎集中在 `App.vue` |
| 前端辅助 | **pinyin-pro** | 前端拼音/首字母搜索高亮 |
| 后端 | **Spring Boot 4.1** + **JDK 17** | REST API |
| 持久化 | **Spring Data JPA** + **Hibernate** | ORM，表结构由 SQL 脚本维护（`ddl-auto: none`） |
| 数据库 | **MySQL 8** | 库名 `campus_xianyu`，字符集 `utf8mb4` |
| 校验 | Bean Validation（`jakarta.validation`） | 请求体校验 |
| 密码 | Spring Security Crypto（BCrypt） | 只哈希密码，不存明文 |
| 登录凭证 | **JWT（HS256）** | `TokenService` 签发/校验，密钥走环境变量 |
| 搜索辅助（后端） | **pinyin4j** | 后端拼音相关匹配 |
| 构建 | Maven（后端）、npm（前端） | |
| 部署 | Nginx + systemd + 腾讯云轻量服务器 | 前端静态资源 + `/api` 反代 |

> 说明：早期交接文档写过 Axios，当前前端实际使用浏览器原生 `fetch`。

---

## 3. 整体框架（怎么连在一起）

```text
┌─────────────────────────────┐
│  浏览器 / 手机 H5            │
│  Vue 3（frontend）           │
│  sessionStorage 存 JWT      │
└──────────────┬──────────────┘
               │  HTTP
               │  /api/** 、 /uploads/**
               ▼
┌─────────────────────────────┐
│  开发：Vite 代理 → 8080      │
│  生产：Nginx 同源反代 → 8080 │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  Spring Boot（backend）      │
│  Controller → Service/Repo  │
│  JWT 校验 Authorization头    │
└──────────────┬──────────────┘
               ▼
┌─────────────────────────────┐
│  MySQL：campus_xianyu        │
│  + 本地/服务器 uploads 目录  │
└─────────────────────────────┘
```

### 3.1 前端做什么

- 渲染全部页面（登录注册、学生端、管理员端、弹窗）
- 调用后端 REST 接口读写数据
- 用 `sessionStorage` 保存登录令牌，请求时带上：
  ```http
  Authorization: Bearer <token>
  ```
- 登录失效自动退出；权限不足时回到对应角色页面

### 3.2 后端做什么

- 提供 `/api/**` 业务接口
- 校验 JWT、校验角色（学生 / 管理员）与实名状态
- 通过 JPA 访问 MySQL
- 保存商品图片到 `uploads/`（或服务器 `UPLOAD_DIR`）
- 统一响应格式与全局异常处理

### 3.3 数据库做什么

- 存用户、商品、求购、留言、会话、消息、订单、举报、分类、校区、审核日志等
- 表结构以 `sql/01_schema.sql` 为准，业务代码不自动建表

### 3.4 本地开发如何连接

1. MySQL 运行，库已导入 `01` → `02`（及后续增量 SQL）
2. 后端：`8080`（`./mvnw spring-boot:run`，需设 `DB_PASSWORD`）
3. 前端：`5173`（`npm run dev`）
4. Vite 把前端的 `/api`、`/uploads` **代理**到 `http://127.0.0.1:8080`

因此前端代码里写：

```js
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'
```

开发时请求 `http://127.0.0.1:5173/api/...`，由 Vite 转到后端，浏览器不跨域。

### 3.5 生产部署如何连接

1. 前端 `npm run build` → 静态文件放到 Nginx（如 `/var/www/campus-xianyu`）
2. 后端监听本机 `8080`
3. Nginx：
   - `/` → 前端静态页
   - `/api/` → 反代到 `127.0.0.1:8080`
   - `/uploads/` → 反代到后端图片

用户只访问一个公网地址，前后端**同源**，不必写死 `localhost:8080`。

### 3.6 统一接口约定

- 成功：`{ "code": 0, "message": "成功", "data": ... }`
- 失败：`code != 0`，`message` 为错误说明
- 需登录接口：请求头带 `Authorization: Bearer <token>`

---

## 4. 仓库目录总览

```text
campus-xianyu/
├── backend/                 Spring Boot 后端
├── frontend/                Vue 3 前端
├── sql/                     数据库脚本
├── docs/                    协作与接口文档
├── deploy/                  Nginx / systemd 部署配置
├── docker-compose.yml       本地/云端一键起 MySQL
├── start.sh                 CloudStudio / 开发一键启动脚本
└── README.md                项目入口说明
```

---

## 5. 后端包结构与文件职责

根包：`com.campus.xianyu`

### 5.1 启动与配置

| 文件 | 作用 |
|------|------|
| `XianyuApplication.java` | Spring Boot 启动入口 |
| `config/WebConfig.java` | CORS、密码编码器、上传静态资源映射 `/uploads/**` |
| `resources/application.yaml` | 端口、数据源、上传目录、JWT 密钥与有效期 |
| `pom.xml` | Maven 依赖与构建 |

### 5.2 公共层 `common/`

| 文件 | 作用 |
|------|------|
| `ApiResponse.java` | 统一响应包装 |
| `PageResponse.java` | 分页结果包装 |
| `GlobalExceptionHandler.java` | 全局异常 → 统一错误响应 |
| `SearchText.java` | 中文 / 拼音 / 首字母相关度打分（搜索用） |

### 5.3 认证 `auth/`（A + 公共）

| 文件 | 作用 |
|------|------|
| `AuthController.java` | `/api/auth/register`、`/login` |
| `AuthService.java` | 注册登录业务、密码哈希校验 |
| `TokenService.java` | 签发 / 解析 JWT |
| `LoginRequest.java` / `LoginResponse.java` / `RegisterRequest.java` | 请求响应 DTO |

### 5.4 用户 `user/`（A）

| 文件 | 作用 |
|------|------|
| `AppUser.java` | 用户实体（对应表 `user`） |
| `UserRepository.java` | 用户查询、学生搜索等 |
| `UserController.java` | `/api/users/me`、公开主页、提交实名认证 |
| `UserResponse.java` / `PublicUserResponse.java` | 完整用户信息 / 公开信息 |

### 5.5 商品 `product/`（A + B 搜索）

| 文件 | 作用 |
|------|------|
| `Product.java` / `ProductImage.java` | 商品与图片实体 |
| `ProductRepository.java` / `ProductImageRepository.java` | 数据访问 |
| `ProductController.java` | 列表/详情/发布/修改/下架/恢复/我的商品 |
| `ProductRequest.java` / `ProductResponse.java` | 入参与出参 |

### 5.6 字典 `dict/`（公共 + C 管理）

| 文件 | 作用 |
|------|------|
| `Category.java` / `Campus.java` | 分类、校区实体 |
| `CategoryRepository.java` / `CampusRepository.java` | 数据访问 |
| `CategoryService.java` | 分类增删改查与排序维护 |
| `DictController.java` | `/api/categories`、`/api/campuses`（学生端选项） |
| `OptionResponse.java` | `{id, name}` 选项结构 |

### 5.7 求购 `wanted/`（B）

| 文件 | 作用 |
|------|------|
| `Wanted.java` | 求购实体 |
| `WantedRepository.java` | 数据访问 |
| `WantedController.java` | 列表/发布/我的/详情/修改/匹配/关闭 |
| `WantedRequest.java` / `WantedResponse.java` | 入参出参 |

### 5.8 留言 `comment/`（C）

| 文件 | 作用 |
|------|------|
| `Comment.java` | 留言实体 |
| `CommentRepository.java` | 按商品分页查留言 |
| `CommentController.java` | `/api/comments` 发表与列表 |
| `CommentRequest.java` / `CommentResponse.java` | DTO |

### 5.9 私聊 `message/`（C）

| 文件 | 作用 |
|------|------|
| `Conversation.java` | 会话实体（可关联商品或求购） |
| `ChatMessage.java` | 消息实体 |
| `ConversationRepository.java` / `ChatMessageRepository.java` | 数据访问 |
| `MessageController.java` | 创建会话、会话列表、拉消息、发消息 |
| `ConversationCreateRequest.java` 等 | 会话/消息 DTO |

### 5.10 订单 `order/`（C）

| 文件 | 作用 |
|------|------|
| `TradeOrder.java` | 订单实体（含双方确认字段） |
| `OrderRepository.java` | 查询买家/卖家订单、防重复进行中订单 |
| `OrderController.java` | 下单、列表、详情、状态动作（双方确认） |
| `OrderCreateRequest.java` / `OrderStatusRequest.java` / `OrderResponse.java` | DTO |

订单状态流转要点：

- `PENDING_CHAT`（待沟通）→ 双方 `CONFIRM_TRADE` → `PENDING_TRADE`（待交易）
- `PENDING_TRADE` → 买家确认收货 + 卖家确认完成 → `COMPLETED`，商品下架

### 5.11 举报 `report/`（C）

| 文件 | 作用 |
|------|------|
| `Report.java` / `ReportReason.java` | 举报与原因实体 |
| `ReportRepository.java` / `ReportReasonRepository.java` | 数据访问 |
| `ReportController.java` | 原因字典、学生提交举报 |
| `ReportCreateRequest.java` / `ReportHandleRequest.java` / `ReportResponse.java` | DTO |

### 5.12 管理端 `admin/`（A 认证审核 + C 其余）

| 文件 | 作用 |
|------|------|
| `AdminController.java` | 实名认证申请列表与审核 |
| `AdminCatalogController.java` | 商品审核、举报处理、用户封禁、分类 CRUD、查看关键词审核记录 |
| `*Request.java` | 各类管理操作请求体 |

### 5.13 关键词审核 `aiaudit/`（C）

| 文件 | 作用 |
|------|------|
| `AiAuditService.java` | **关键词规则**审核（非大模型） |
| `AiAuditLog.java` / `AiAuditLogRepository.java` | 审核日志表 |
| `AiAuditController.java` | 管理员手动触发 / 查看记录 |
| `AiAuditResponse.java` | 出参 |

### 5.14 上传 `upload/`（A/公共）

| 文件 | 作用 |
|------|------|
| `UploadController.java` | `/api/uploads/images` 上传商品图，返回可访问 URL |

### 5.15 测试

| 文件 | 作用 |
|------|------|
| `auth/TokenServiceTests.java` | JWT 相关单测 |
| `common/SearchTextTests.java` | 搜索打分单测 |
| `XianyuApplicationTests.java` | 启动冒烟测试 |

---

## 6. 前端文件职责

当前前端是 **单文件主应用** 结构：

| 路径 | 作用 |
|------|------|
| `frontend/package.json` | 依赖：Vue、Vite、pinyin-pro |
| `frontend/vite.config.js` | 开发服务器；`/api`、`/uploads` 代理到后端 8080 |
| `frontend/index.html` | HTML 入口 |
| `frontend/src/main.js` | 创建 Vue 应用并挂载 `App.vue` |
| `frontend/src/App.vue` | **几乎全部业务 UI + 接口调用**：登录注册、首页商品、求购、发布、消息、我的、管理端各 Tab、详情弹窗、订单弹窗、举报弹窗等 |
| `frontend/src/style.css` | 全局样式（移动端卡片、弹窗、导航等） |
| `frontend/src/components/HelloWorld.vue` | Vite 模板残留组件，业务未使用 |
| `frontend/src/assets/` | 静态资源 |

`App.vue` 内关键机制：

- `apiRequest()`：统一 `fetch`、带 Token、处理登录失效/权限错误
- `sessionStorage` 键：`campus_xianyu_token`
- 学生端底部导航：首页 / 求购 / 发布 / 消息 / 我的
- 管理端顶部 Tab：认证审核 / 商品审核 / 举报 / 用户 / 分类

---

## 7. 数据库脚本 `sql/`

| 文件 | 作用 |
|------|------|
| `00_create_users.sql` | 可选：创建数据库用户 |
| `01_schema.sql` | **建库建表**（权威表结构） |
| `02_seed_data.sql` | 公共数据：校区、分类、举报原因、管理员账号 |
| `03_demo_data.sql` | 演示数据 |
| `04_expand_categories.sql` | 分类扩展 |
| `05_acceptance_test_data.sql` | 验收联调用统一测试数据 |
| `06_c_followup.sql` | C 后续增量：订单 `buyer_confirmed` / `seller_confirmed` |

主要业务表（见 `01_schema.sql`）：

`user`、`category`、`campus`、`report_reason`、`product`、`product_image`、`wanted`、`comment`、`conversation`、`message`、`trade_order`、`report`、`ai_audit_log`

---

## 8. 部署相关

| 路径 | 作用 |
|------|------|
| `deploy/nginx-campus-xianyu.conf` | Nginx：静态前端 + `/api` `/uploads` 反代 |
| `deploy/campus-xianyu.service` | systemd 开机自启后端 |
| `docker-compose.yml` | 开发用 MySQL 容器 |
| `start.sh` | CloudStudio / 开发环境一键起库（可顺带起前后端） |
| `docs/cloudstudio-deploy.md` | CloudStudio 部署说明 |

---

## 9. 文档 `docs/`

| 文件 | 作用 |
|------|------|
| `handoff-report.md` | 三人协作交接与规范 |
| `api.md` | 接口约定 |
| `enums.md` | 状态枚举（商品/订单/认证等） |
| `cloudstudio-deploy.md` | CloudStudio 起库部署 |
| `c-followup-handoff.md` | C 模块后续改动与 B 重新部署说明（本地交接，未必入库） |
| `architecture-overview.md` | 本文档：整体架构与文件地图 |

---

## 10. 一次请求走读（举例：登录后看商品列表）

1. 用户打开前端 → `main.js` 挂载 `App.vue`
2. `loadMe()` 用 `sessionStorage` 里的 Token 请求 `GET /api/users/me`
3. Vite/Nginx 把 `/api` 转到 Spring Boot
4. `UserController` 经 `TokenService` 解析 JWT，查 `UserRepository`
5. 前端再请求 `GET /api/products?...`
6. `ProductController` 用 JPA 查 `product` 表，返回 `ApiResponse` + 分页
7. `App.vue` 渲染首页商品卡片

---

## 11. 本地快速启动（摘要）

```powershell
# 数据库：先保证 MySQL 已导入 01 + 02（及需要的 06）

# 后端
cd backend
$env:DB_PASSWORD="你的密码"
.\mvnw.cmd spring-boot:run

# 前端
cd frontend
npm install
npm run dev
```

- 页面：http://127.0.0.1:5173  
- 接口：经代理访问后端 http://127.0.0.1:8080/api  
- 默认管理员（种子数据）：`admin` / `password`（上线前务必修改）

---

## 12. 模块与 API 前缀速查

| 前缀 | 模块 |
|------|------|
| `/api/auth` | 注册登录 |
| `/api/users` | 当前用户、公开主页、实名认证 |
| `/api/products` | 商品 |
| `/api/wanted` | 求购 |
| `/api/comments` | 留言 |
| `/api/messages` | 私聊 |
| `/api/orders` | 订单 |
| `/api/reports` | 举报 |
| `/api/categories`、`/api/campuses` | 字典 |
| `/api/uploads` | 图片上传 |
| `/api/admin` | 管理端 |
| `/api/ai-audit` | 关键词审核 |

---

*文档根据当前仓库结构整理，若后续拆分前端多页面或新增后端包，请同步更新本文件。*
