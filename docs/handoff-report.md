# 校园版咸鱼 — 协作交接报告（成员 C）

日期：2026-08-21  
仓库：https://github.com/duoliyangzi/campus-xianyu  
仓库首页说明：见根目录 [README.md](../README.md)

---

## 一、成员 C 已完成的工作

1. **GitHub 仓库**  
   - 创建公开仓库 `duoliyangzi/campus-xianyu`，作为三人唯一代码库。  
   - 已提交 SQL、Docker、启动脚本、规范文档与本交接报告。  

2. **数据库设计与初始化文件**  
   - `sql/01_schema.sql`：全组表结构（用户、分类、校区、商品及图片、求购、留言、私聊会话/消息、订单、举报、AI 审核日志等）。  
   - `sql/02_seed_data.sql`：公共数据——校区、商品一级/二级分类（教材、数码、服饰、生活用品、运动用品、周边及海报/吧唧等）、举报原因、管理员占位账号。  
   - `sql/00_create_users.sql`：可选的数据库双账号模板（管理 / 运行）。  
   - `sql/03_demo_data.sql`：演示数据模板（答辩前按需使用，不作为日常初始化）。  

3. **云端数据库部署与验证**  
   - 使用腾讯云 **CloudStudio** 从本仓库 Git 导入项目。  
   - 通过 `start.sh` / Docker 启动 MySQL，库名 `campus_xianyu`。  
   - 已验证表已创建，分类等中文公共数据查询正常。  

4. **本地与云端起库工具**  
   - `docker-compose.yml`、`start.sh`（检测无前后端时仅起库；有 `backend/`、`frontend/` 后可尝试一并启动）。  

5. **协作规范文档**  
   - 本报告、`docs/enums.md`、仓库 README。  

**说明：** 业务前后端（`backend/` / `frontend/`）尚未开发；留言、订单、管理后台等由 C 后续实现；整站公网访问地址待功能就绪后由 C 部署提供。

---

## 二、用什么开发（统一技术栈）

| 项目 | 技术 | 说明 |
|------|------|------|
| 前端 | Vue 3 + Vite | 移动端 H5，Axios 调接口 |
| 后端 | Spring Boot（JDK 17/21） | REST API |
| 数据库 | MySQL 8 | 结构以仓库 SQL 为准 |
| 版本管理 | Git + 本仓库 | 代码统一推送到上述 GitHub |
| 接口测试 | Apifox / Postman | 后端自测 |
| 前端调试 | Chrome 手机模式 | H5 调试 |

目录建议：

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
   - 有 Docker：项目根目录执行 `docker compose up -d`  
   - 或手动执行：`sql/01_schema.sql` → `sql/02_seed_data.sql`  

日常在自己电脑开发即可。本机库用于个人调试；**表结构与枚举必须以仓库为准**。改表须三人协商，由 C 更新云库并同步仓库 SQL。

### 2. 日常流程

```text
git pull → 改自己的模块 → 本地运行自测 → git add / commit / push
```

业务数据通过接口写入，不要直接改库塞商品/用户。每天至少合并联调一次。

### 3. 分工

| 成员 | 负责 | 主要接口 |
|------|------|----------|
| A | 注册登录、实名认证、商品增删改查 | `/api/auth` `/api/users` `/api/products` |
| B | 商品搜索筛选分页、求购 | `/api/products`（列表/搜索） `/api/wanted` |
| C | 留言私聊订单、管理后台、AI 审核、部署 | `/api/comments` `/api/messages` `/api/orders` `/api/admin` `/api/categories` `/api/reports` `/api/ai-audit` |

建议 A 先创建 `backend/` 并打通登录与商品；B、C 在同一后端按模块加接口；`frontend/` 可共建。

### 4. 部署（后期）

开发阶段前后端本地运行。功能基本完成后由 C 部署到云，提供可访问网址（MySQL + Spring Boot + 前端静态资源）。

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

### 2. 角色与状态（代码枚举）

| 类型 | 取值 |
|------|------|
| 角色 | `STUDENT` / `ADMIN` |
| 商品状态 | `PENDING` / `PUBLISHED` / `OFF_SHELF` / `REJECTED` |
| 订单状态 | `PENDING_CHAT` / `PENDING_TRADE` / `COMPLETED` |
| 认证状态 | `UNAUTH` / `PENDING` / `APPROVED` / `REJECTED` |
| 新旧程度 | `NEW` / `LIKE_NEW` / `GOOD` / `FAIR` / `POOR` |
| 交易方式 | `FACE` / `MAIL` / `BOTH` |

详见 `docs/enums.md`。

### 3. 其他

- 改表同步更新 `sql/01_schema.sql`；公共数据同步 `sql/02_seed_data.sql`。  
- 学生端 H5 底部建议：首页 / 求购 / 发布 / 消息 / 我的；管理端独立后台。  
- Commit 示例：`feat(auth): 学生注册登录`。

---

## 五、发给组员（可复制）

```text
【校园咸鱼】https://github.com/duoliyangzi/campus-xianyu
请发 GitHub 用户名，我拉协作。规范见 README 与 docs/handoff-report.md
技术栈 Vue3 + Spring Boot + MySQL；本机 docker compose up -d 后开发并 push
分工：A 登录+商品；B 搜索+求购；C 留言订单后台+部署（云库已就绪）
```
