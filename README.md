# 校园版咸鱼（Campus Xianyu）

高校二手交易 H5：Vue 3 + Spring Boot + MySQL。  
仓库：https://github.com/duoliyangzi/campus-xianyu  

**协作交接（A/B 请先看）：** [docs/handoff-report.md](docs/handoff-report.md)  
**状态枚举：** [docs/enums.md](docs/enums.md)

---

## 成员 C 已完成（2026-08-21）

1. **创建并维护本 GitHub 仓库**，作为三人统一代码库。  
2. **设计并提交全组数据库**  
   - `sql/01_schema.sql`：用户、分类、校区、商品、求购、留言、私聊、订单、举报、AI 审核等表  
   - `sql/02_seed_data.sql`：商品分类（含周边二级）、校区、举报原因、管理员占位账号  
3. **提供本地/云端起库方式**：`docker-compose.yml`、`start.sh`。  
4. **在腾讯云 CloudStudio 部署并验证 MySQL**  
   - 库名：`campus_xianyu`  
   - 已确认分类等公共数据可正常查询（中文正常）  
5. **编写协作交接报告与统一规范**（本页 + `docs/handoff-report.md`）。  

尚未完成（按计划后续做）：前后端业务代码、留言/订单/管理后台、整站公网访问地址。

---

## 技术栈（统一，勿擅自更换）

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite（移动端 H5） |
| 后端 | Spring Boot（JDK 17/21） |
| 数据库 | MySQL 8 |
| 通信 | REST + Axios |

---

## 分工

| 成员 | 模块 |
|------|------|
| A | 注册登录、实名认证、商品增删改查 |
| B | 商品搜索筛选分页、求购 |
| C | 留言私聊订单、管理后台、AI 审核、云部署 |

---

## A/B 如何开始开发

1. 把 GitHub 用户名发给 C，加入本仓库协作者。  
2. `git clone https://github.com/duoliyangzi/campus-xianyu.git`  
3. 本机起库：`docker compose up -d`（或执行 `sql/01` → `sql/02`）。  
4. 建议 A 先建 `backend/`（Spring Boot），再共建 `frontend/`（Vue3）。  
5. 按分工开发，每日 `git pull` / `git push`；表结构以 `sql/` 为准，状态以 `docs/enums.md` 为准。  

详细规范与接口路径见 **[docs/handoff-report.md](docs/handoff-report.md)**。

---

## 仓库结构

```text
sql/                 表结构与公共数据
docs/
  handoff-report.md  协作交接报告
  enums.md           状态枚举
  cloudstudio-deploy.md  CloudStudio 起库说明（部署用）
docker-compose.yml   本地/云端 MySQL
start.sh             一键启动数据库（有前后端后可顺带启动）
```

```bash
# 本机 / CloudStudio 启动数据库
chmod +x start.sh
./start.sh
# 或
docker compose up -d
```

---

## 数据与部署约定

- 公共数据用 SQL 初始化；用户/商品/订单等业务数据由后端接口写入。  
- 改表须三人协商，并同步更新仓库中的 SQL。  
- 开发阶段前后端在本地运行；功能就绪后由 C 统一部署，提供可访问网址。
