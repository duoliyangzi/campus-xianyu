# 校园咸鱼 · 生产上线指南（轻量云 / 单机）

> 目标架构：Nginx（前端静态 + 反代）→ Spring Boot `:8080` → MySQL；旁路 FastAPI AI `:8000` + Redis。  
> 用户只访问一个公网域名/IP，前后端同源。

---

## 0. 上线前检查清单

- [ ] 改掉默认管理员密码（种子 `admin` / `password`）
- [ ] 生产 `JWT_SECRET`、MySQL 密码、LLM API Key **不进 Git**
- [ ] 执行增量 SQL（含 `sql/07_ai_chat.sql`），**不要**重跑会清空库的 `01_schema.sql`
- [ ] 前端 `vite` 生产构建走相对路径 `/api`（默认即可）
- [ ] AI 的 `JAVA_API_BASE` 指向本机后端 `http://127.0.0.1:8080/api`（生产是 8080，不是本地开发 8081）
- [ ] Nginx 已配置 SSE（见 `deploy/nginx-campus-xianyu.conf`）

---

## 1. 服务器准备

推荐：腾讯云 / 阿里云轻量 2核4G，Ubuntu 22.04。

```bash
sudo apt update
sudo apt install -y openjdk-17-jre-headless nginx mysql-server redis-server python3.12-venv
# 或用 Docker 只起 MySQL + Redis：docker compose up -d
```

Node 用于构建前端（可本机构建后上传 `dist/`，服务器可不装 Node）。

目录约定：

```text
/opt/campus-xianyu/          # 代码
/var/www/campus-xianyu/      # 前端静态
/etc/campus-xianyu.env       # 后端环境变量（权限 600）
/etc/campus-xianyu-ai.env    # AI 环境变量（权限 600）
```

---

## 2. 拉代码与数据库

```bash
sudo mkdir -p /opt/campus-xianyu
sudo chown $USER:$USER /opt/campus-xianyu
cd /opt/campus-xianyu
git clone <你的仓库地址> .
```

建库（首次）：

```bash
sudo mysql -e "CREATE DATABASE IF NOT EXISTS campus_xianyu DEFAULT CHARSET utf8mb4;"
sudo mysql campus_xianyu < sql/01_schema.sql
sudo mysql campus_xianyu < sql/02_seed_data.sql
# 后续增量按顺序执行，例如：
sudo mysql campus_xianyu < sql/06_c_followup.sql
sudo mysql campus_xianyu < sql/07_ai_chat.sql
```

---

## 3. 后端 Spring Boot

`/etc/campus-xianyu.env` 示例：

```bash
DB_URL=jdbc:mysql://127.0.0.1:3306/campus_xianyu?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
DB_USERNAME=root
DB_PASSWORD=换成强密码
JWT_SECRET=换成长随机串
UPLOAD_DIR=/opt/campus-xianyu/backend/uploads
AI_ENABLED=true
AI_BASE_URL=http://127.0.0.1:8000
```

```bash
cd /opt/campus-xianyu/backend
./mvnw -DskipTests package
cp target/*.jar /opt/campus-xianyu/backend/xianyu.jar

sudo cp deploy/campus-xianyu.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now campus-xianyu
sudo systemctl status campus-xianyu
curl -s http://127.0.0.1:8080/api/categories
```

---

## 4. AI 服务（FastAPI）

```bash
cd /opt/campus-xianyu/ai-service
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
cp .env.example .env
# 编辑 .env：填 LLM_API_KEY、REDIS_URL、JAVA_API_BASE=http://127.0.0.1:8080/api
```

安装 systemd：

```bash
sudo cp deploy/campus-xianyu-ai.service /etc/systemd/system/
# 把密钥放到 /etc/campus-xianyu-ai.env（或服务里 EnvironmentFile 指向 ai-service/.env）
sudo systemctl daemon-reload
sudo systemctl enable --now campus-xianyu-ai
curl -s http://127.0.0.1:8000/health
```

可选评测（上线后回归）：

```bash
cd /opt/campus-xianyu/ai-service
source .venv/bin/activate
python -m eval.run_eval --difficulty hard
```

---

## 5. 前端构建与 Nginx

本机或服务器：

```bash
cd /opt/campus-xianyu/frontend
npm ci
npm run build
sudo mkdir -p /var/www/campus-xianyu
sudo rsync -a --delete dist/ /var/www/campus-xianyu/
```

```bash
sudo cp /opt/campus-xianyu/deploy/nginx-campus-xianyu.conf /etc/nginx/sites-available/campus-xianyu
sudo ln -sf /etc/nginx/sites-available/campus-xianyu /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

开放安全组 **80**（有域名再上 443 + Let’s Encrypt）。

---

## 6. 上线验收

| 检查 | 预期 |
|------|------|
| 公网打开站点 | 登录页正常 |
| `/api/categories` | JSON `code=0` |
| 发布/上传图 | `/uploads/...` 可访问 |
| AI 健康 | 后端 `/api/ai/health` 正常 |
| AI 对话 | 流式逐字；「现在有手机卖吗」能出卡片 |
| 改默认管理员密码 | 完成 |

---

## 7. 常见坑

1. **AI 搜不到商品**：`JAVA_API_BASE` 仍指向 `8081` → 改为 `8080`。  
2. **流式一次性蹦出 / 断流**：Nginx 未关 `proxy_buffering`，见 conf 中 `/api/ai/`。  
3. **密钥泄露**：`.env`、`campus-xianyu.env` 永不 commit。  
4. **重跑 schema 清空数据**：生产只用增量 SQL。  
5. **内存不够**：Java `-Xmx640m`；AI 单 worker 即可，别盲目多进程抢内存。

---

## 8. 简历评测数字（与 eval 对齐，2026-09-13）

- 意图：easy 100% / hard ~92% / all ~96%  
- RAG：Hit@1 ~87% / Hit@3 100%  
- 风控一致率：本轮小样本 100%（hard 含谐音与导流）

详见 `docs/ai-feature-implementation-2026-09-12.md` §5.1。
