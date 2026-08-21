# 校园版咸鱼 — 协作交接报告（成员 C）

日期：2026-08-21  
仓库：https://github.com/duoliyangzi/campus-xianyu  

---

## 一、当前进度（C 已完成）

1. GitHub 仓库已建立，初始化 SQL、Docker、启动脚本已提交。  
2. CloudStudio 云端 MySQL 已跑通，库名 `campus_xianyu`，公共分类/校区等已导入。  
3. 全组表结构以仓库 `sql/01_schema.sql` 为准；枚举约定见 `docs/enums.md`。  

业务前后端代码尚未开始，由 A/B/C 按分工继续开发。

---

## 二、用什么开发（统一技术栈）

| 项目 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Vite | 移动端 H5，Axios 调接口 |
| 后端 | Spring Boot（JDK 17/21） | REST API |
| 数据库 | MySQL 8 | 结构以仓库 SQL 为准 |
| 版本管理 | Git + 本仓库 | 代码统一推到上述 GitHub |
| 接口测试 | Apifox / Postman | 后端自测 |
| 前端调试 | Chrome 手机模式 | H5 调试 |

目录建议（建好后放进本仓库）：

```text
backend/     Spring Boot
frontend/    Vue 3 + Vite
sql/         已有，勿随意改
```

---

## 三、之后如何开发

### 1. 准备

1. 把 GitHub 用户名发给 C，加入本仓库协作者（Write）。  
2. 克隆：`git clone https://github.com/duoliyangzi/campus-xianyu.git`  
3. 本机安装：JDK、Node.js、MySQL（或 Docker）。  
4. 本机建库（与云端结构一致）：  
   - 有 Docker：在项目根目录执行 `docker compose up -d`  
   - 或手动执行：`sql/01_schema.sql` → `sql/02_seed_data.sql`  

日常在**自己电脑**开发即可，不必每人都开 CloudStudio。  
本机库只用于个人调试；**表结构、字段名、状态枚举必须以 GitHub 的 SQL/文档为准**，不要私自改表。需要改表时三人商量，由 C 更新云库并同步改仓库 SQL。

### 2. 日常流程

```text
git pull → 改自己的模块 → 本地运行自测 → git add/commit/push
```

每天至少合并联调一次。业务数据通过接口写入，不要直接改库塞商品/用户。

### 3. 分工

| 成员 | 负责 | 主要接口 |
|------|------|----------|
| A | 注册登录、实名认证、商品增删改查 | `/api/auth` `/api/users` `/api/products` |
| B | 商品搜索筛选分页、求购 | `/api/products`（列表/搜索） `/api/wanted` |
| C | 留言私聊订单、管理后台、AI 审核、部署 | `/api/comments` `/api/messages` `/api/orders` `/api/admin` `/api/categories` `/api/reports` `/api/ai-audit` |

建议 A 先创建 `backend/` Spring Boot 工程并打通登录与商品；B、C 在同一后端上按模块加接口；前端 `frontend/` 可共建，页面按分工负责。

### 4. 部署（后期，不是现在）

开发阶段前后端在本地运行。功能基本完成后再由 C 部署到云（CloudStudio 或云服务器），提供可访问网址。  
最终提交需要云端访问地址；**不是**把整个 Git 仓库「上传即网站」，而是：云上跑 MySQL + Spring Boot + 前端静态资源（Nginx）。

---

## 四、统一规范

### 1. 接口路径

```text
/api/auth
/api/users
/api/products
/api/wanted
/api/comments
/api/messages
/api/orders
/api/admin
/api/categories
/api/reports
/api/ai-audit
```

### 2. 角色与状态（写代码枚举，不要做成可乱增的分类）

| 类型 | 取值 |
|------|------|
| 角色 | `STUDENT` / `ADMIN` |
| 商品状态 | `PENDING` 待审核 / `PUBLISHED` 已发布 / `OFF_SHELF` 已下架 / `REJECTED` 拒绝 |
| 订单状态 | `PENDING_CHAT` 待沟通 / `PENDING_TRADE` 待交易 / `COMPLETED` 已完成 |
| 认证状态 | `UNAUTH` / `PENDING` / `APPROVED` / `REJECTED` |
| 新旧程度 | `NEW` / `LIKE_NEW` / `GOOD` / `FAIR` / `POOR` |
| 交易方式 | `FACE` / `MAIL` / `BOTH` |

完整说明见 `docs/enums.md`。

### 3. 数据库

- 改表结构必须三人协商，并更新 `sql/01_schema.sql` 与设计文档。  
- 公共数据改动同步更新 `sql/02_seed_data.sql`。  
- 用户、商品、订单等业务数据只通过后端接口增删改。  

### 4. 页面

- 学生端：移动端 H5；底部建议：首页 / 求购 / 发布 / 消息 / 我的。  
- 管理员：独立后台页面，不与学生底部导航混用。  

### 5. 提交说明

Commit 写清模块，例如：`feat(auth): 学生注册登录`、`feat(wanted): 求购列表与发布`。

---

## 五、发给组员的简短说明（可直接复制）

```text
【校园咸鱼】仓库 https://github.com/duoliyangzi/campus-xianyu
请发我 GitHub 用户名，我拉你们进协作。
技术栈：Vue3 + Spring Boot + MySQL，规范见仓库 docs/handoff-report.md 与 docs/enums.md
本机：clone 后 docker compose up -d（或导 sql/01、02），本地开发，代码 push 到该仓库
分工：A 登录认证+商品；B 搜索+求购；C 留言订单后台+部署
表结构和状态枚举统一，不要私自改表；业务数据走接口
前后端先本地跑，后期由我统一部署出访问地址
```

---

## 六、参考文件

| 文件 | 内容 |
|------|------|
| `sql/01_schema.sql` | 表结构 |
| `sql/02_seed_data.sql` | 公共初始数据 |
| `docs/enums.md` | 状态枚举 |
| `docs/handoff-report.md` | 本交接报告 |
| `docker-compose.yml` / `start.sh` | 本地/CloudStudio 起库 |
