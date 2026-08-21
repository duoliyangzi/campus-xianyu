# 校园版咸鱼 — 三人协作交接说明（给 A / B / C）

仓库：https://github.com/duoliyangzi/campus-xianyu  
云环境：腾讯云 CloudStudio 应用 `campus-xianyu`（由 **C** 维护数据库）  
技术栈（以开发指南为准，不要换）：

| 层 | 技术 |
|----|------|
| 前端 | **Vue 3 + Vite**（移动端 H5） |
| 后端 | **Spring Boot**（REST API） |
| 数据库 | **MySQL 8**（已在 CloudStudio 用 Docker 建好） |
| 请求 | Axios |
| 接口前缀 | `/api/...`（见下文） |

---

## 1. 现在到底完成了什么？

**已完成（C）：**

- GitHub 仓库与初始化 SQL、启动脚本
- CloudStudio 中 MySQL 容器已跑通，库名 `campus_xianyu`
- 公共数据已导入：商品分类（含周边二级）、校区、举报原因、管理员占位账号
- 全组业务表结构已建好（用户/商品/求购/留言/私聊/订单/举报/AI 审核等）

**尚未完成（正常，今天本来就不要求整站）：**

- 还没有 `backend/`（Spring Boot 工程）
- 还没有 `frontend/`（Vue 工程）
- 因此还没有「打开网页就能注册、发商品」——**业务数据自动入库，要等后端接口写好之后才会发生**

一句话：现在是 **「空系统 + 公共字典 + 表结构」**；不是整站已经对外营业。

---

## 2. A / B 要不要注册 CloudStudio？

**不是必须每人一个。** 推荐两种模式，选一种统一即可。

### 方案甲（推荐，省机时）：本地开发 + 共用云库（或本地库）

| 人 | 做什么 |
|----|--------|
| C | 负责 CloudStudio 开着时提供云 MySQL；不用时 **停止** 工作空间省机时 |
| A / B | 本机装 IDEA + VS Code / Node，写 Spring Boot / Vue |
| 数据库 | 开发初期可 **本机也装一份 MySQL**，执行仓库里的 `sql/01_schema.sql` + `sql/02_seed_data.sql`，和云端结构一致；联调时再连 C 的云库 |

**优点：** A/B 不必一直开 CloudStudio，不抢 C 的机时。  
**注意：** 若连 C 的云库，需要 C 开启工作空间，且解决「本机如何连到 CloudStudio 里的 3306」（端口转发/仅同空间后端可连）。**最省事是：各人本机 MySQL 开发，字段以 GitHub 的 SQL 为准；定期 `git pull`，联调前再统一。**

### 方案乙：三人都用 CloudStudio

- A、B 各自注册 CloudStudio，由 C **邀请进同一个应用**协作，或各自 clone 同一 GitHub 仓库建空间  
- 都要消耗机时，适合短时间集中联调  

**结论给组长：**  
日常开发 → **方案甲（本地写代码）**；需要看云库 / 演示 → C 打开 CloudStudio。  
A/B **可以不注册** CloudStudio，但 **必须有 GitHub 账号**，并被加进本仓库协作者。

---

## 3. GitHub 怎么交接（C 先做）

1. 打开 https://github.com/duoliyangzi/campus-xianyu  
2. **Settings → Collaborators → Add people**  
3. 添加 A、B 的 GitHub 用户名，权限选 **Write**  
4. A/B 邮箱接受邀请  

之后代码都推到这个仓库，不要各建各的互不相通的库（除非另建组织仓库并迁移）。

### 每人日常命令

```bash
git clone https://github.com/duoliyangzi/campus-xianyu.git
cd campus-xianyu
git pull                    # 开工前
# ... 改自己的模块 ...
git add .
git commit -m "feat: 说明你做了什么"
git push
```

建议每人建自己的分支（如 `feature/a-auth`），合并前用 PR；课程时间紧也可以约定直接推 `main`，但 **每天至少合并一次**（开发指南要求）。

---

## 4. 分工（对照开发指南）

### 成员 A：商品发布管理 + 用户与认证

- 页面：登录、注册、实名认证、发布/编辑商品、我的商品、商品详情基础信息  
- 接口：`/api/auth`、`/api/users`、商品 CRUD（可挂在 `/api/products`）  
- 负责：在仓库创建并维护 **`backend/` 骨架（Spring Boot）** 亦可三人共建，但登录注册与商品主线由 A 优先打通  

### 成员 B：搜索筛选 + 求购

- 页面：首页列表、搜索、筛选、求购列表/发布/详情  
- 接口：商品列表/搜索/筛选/分页、`/api/wanted`  

### 成员 C：互动交易 + 平台管理 + AI + 部署

- 页面：留言、私聊、订单状态、管理后台、审核、举报、分类管理、AI 审核结果  
- 接口：`/api/comments`、`/api/messages`、`/api/orders`、`/api/admin`、`/api/categories`、`/api/reports`、`/api/ai-audit`  
- 部署：CloudStudio / 后续云主机、SQL 维护  

统一约定见：`docs/enums.md`。

---

## 5. 「后端自动写入数据库」到底是什么意思？

**不是现在已经有魔法。** 流程是：

```text
手机浏览器
  → 打开 Vue 前端页面（以后才有）
  → 前端用 Axios 调用 Spring Boot 接口，例如 POST /api/products
  → Spring Boot 用 JDBC/MyBatis/JPA 执行 INSERT/UPDATE
  → 数据进入 MySQL（campus_xianyu）
```

所以：

1. **现在没有 backend/frontend** → 网页还不能往库里写业务数据（只能 SQL 里已有的分类等公共数据）。  
2. **A 先搭 Spring Boot**，写出例如「注册用户」接口并测通 → 调用接口后，`user` 表才会出现新行。  
3. **再搭 Vue**，页面点「注册」按钮去调这个接口 → 这才是「用系统填数据，而不是三人直接改数据库」。  

### 后端用什么？

**Spring Boot（Java）**，不要换成 Django/Express 等，除非老师同意改技术栈。

建议版本方向：

- JDK 17 或 21  
- Spring Boot 3.x  
- 访问 MySQL：`mysql-connector-j` + Spring Data JPA 或 MyBatis（组里选一种统一）  
- 密码：BCrypt  

工程建议目录（建好后放进本仓库）：

```text
campus-xianyu/
  backend/          ← Spring Boot（A 可先 init）
  frontend/         ← Vue3 + Vite（可 A/B 共建或 B 偏前端）
  sql/              ← 已有
  docker-compose.yml
  start.sh
  docs/
```

`start.sh` 已预留：检测到 `backend/`、`frontend/` 会尝试启动。

### 本地连数据库示例（application.yml）

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/campus_xianyu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root_change_me   # 改成你们自己的，勿提交真实密码到公开仓库
    driver-class-name: com.mysql.cj.jdbc.Driver
```

本机开发：先本机 Docker/`docker compose up -d` 或本机 MySQL 导入 `sql/01`+`02`。  
连 CloudStudio 云库：仅当 C 工作空间运行中，且已做好端口访问方案时再用。

---

## 6. 数据库还要不要「再填」？

| 数据类型 | 怎么来 | 还要不要再导 SQL |
|----------|--------|------------------|
| 分类/校区/举报原因 | `02_seed_data.sql` 已导入 | 一般不用 |
| 用户/商品/求购/留言/订单 | **接口 + 页面** 产生 | 不要日常手写 INSERT |
| 改表结构 | 改 `01_schema.sql` + 三人协商后更新所有环境 | 要同步 GitHub |
| 答辩演示数据 | 用网页点一遍，或临时用 `03_demo_data.sql` | 可选 |

**禁止：** 三人直接进 Navicat 改业务数据当日常开发方式。  
**允许：** 管理员在后台页面审核、启停分类（也是走接口）。

---

## 7. 建议日程（摘自开发指南，便于交接）

| 日期 | 重点 |
|------|------|
| 8/21 | 仓库 + 云库 + 注册登录/商品主线开始（A）；C 部署已完成最小目标 |
| 8/22 | 商品改删、搜索筛选、求购（A/B） |
| 8/23 | 留言私聊订单、管理后台、AI（C）；首次较完整可访问 |
| 8/24–25 | 联调、文档、PPT、答辩 |

---

## 8. C 给 A/B 的「口头交接」清单（复制发群即可）

```text
【校园咸鱼协作】
1. 仓库：https://github.com/duoliyangzi/campus-xianyu
2. 请把 GitHub 用户名发我，我拉你们进协作者
3. 技术栈：Vue3 + Spring Boot + MySQL，表结构以仓库 sql/ 为准，枚举看 docs/enums.md
4. 云库：我在 CloudStudio 已建好；你们日常可本机导入同一套 sql 开发，省得抢机时
5. 本机起库：docker compose up -d 或执行 sql/01_schema.sql + sql/02_seed_data.sql
6. 还没有 backend/frontend，需要 A 先建 Spring Boot 工程推进册登录和商品；B 做列表搜索求购；我做留言订单后台和部署
7. 业务数据等接口好了用网页写入，不要直接改库
8. 文档详情：仓库里 docs/team-handoff.md
```

---

## 9. 统一接口路径（开发前对齐）

```
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

角色：`STUDENT` / `ADMIN`  
订单状态：`PENDING_CHAT`（待沟通）/ `PENDING_TRADE`（待交易）/ `COMPLETED`（已完成）  
更多见 `docs/enums.md`。

---

## 10. 常见疑问

**Q：整站什么时候算真正上线？**  
A：前端能通过 CloudStudio 预览链接或服务器 IP 打开，并能完成注册→发商品→搜索→留言→订单→后台审核，才算完整上线。现在只是数据库上线。

**Q：A/B 必须装 Docker 吗？**  
A：不必须；能导入 SQL 的 MySQL 即可。有 Docker 最省事：`docker compose up -d`。

**Q：管理员密码是什么？**  
A：seed 中用户名为 `admin`，开发用明文曾约定为 `password`（BCrypt 需与 Spring Security 一致；对不上就 A 用后端重新生成哈希 UPDATE）。**务必改密，勿用于正式环境。**

**Q：机时不够怎么办？**  
A：不用时停止 CloudStudio；答辩前可迁腾讯云轻量机，同一套 `sql/` 导入即可。
