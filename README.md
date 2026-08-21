# 校园版咸鱼 — 数据库与云端初始化（成员 C）

## 现在怎么上云（按课程 CloudStudio 文档）

**今天推荐：走 CloudStudio，不必先买腾讯云。**

详细步骤见：`docs/cloudstudio-deploy.md`

```bash
chmod +x start.sh
./start.sh
```

会启动 MySQL 并导入 `sql/01`、`sql/02`。有 `backend/`、`frontend/` 后会一并启动。

| 用途 | CloudStudio | 说明 |
|------|-------------|------|
| 今天建库、三人联调 | 推荐 | 课程《9-1》同款方案 |
| 答辩长期公网 | 可临时用 | 机时不够再迁腾讯云轻量机 |

---

## 仓库里的 SQL 怎么用

```text
sql/00_create_users.sql   # 双账号（ddl / app），改密码后再执行
sql/01_schema.sql         # 全量表结构
sql/02_seed_data.sql      # 分类 / 校区 / 举报原因 / 管理员占位
sql/03_demo_data.sql      # 演示数据模板，平时不要当公共初始化
```

重建空白系统：

```bash
mysql -uroot -p < sql/01_schema.sql
mysql -uroot -p < sql/02_seed_data.sql
mysql -uroot -p < sql/00_create_users.sql   # 先改文件里的密码
```

Docker 一键（首次空数据目录会自动执行 01/02）：

```bash
docker compose up -d
```

然后用管理账号执行 `00_create_users.sql`，Spring Boot 只配 `xianyu_app`。

---

## 代码枚举（不要做成可随意新增的「分类」）

见 `docs/enums.md`。

---

## 数据规则（三人遵守）

1. 云库建表 + 公共数据只通过 SQL 文件初始化。  
2. 日常只通过网页 / 接口改业务数据，不直接改库。  
3. 改公共数据或表结构必须三人协商，并同步 GitHub SQL 与设计文档。  
4. Spring Boot 使用运行账号，不用 root。

---

## 成员职责提醒

- **A**：`user`、`product`、`product_image`  
- **B**：列表筛选接口、`wanted`  
- **C**：`comment` / `conversation` / `message` / `trade_order` / `report` / `ai_audit_log` / `category` 管理，以及云部署与 SQL 维护
