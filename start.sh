#!/usr/bin/env bash
# ==========================================================
# 校园版咸鱼 — CloudStudio 一键启动
# 今天（仅有 sql/）会：启动 MySQL + 导入表结构/公共数据
# 等有 backend/、frontend/ 后：自动再启 Spring Boot + 前端
# ==========================================================
set -e

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

APP_NAME="校园版咸鱼"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${CYAN}========================================${NC}"
echo -e "${CYAN}  ${APP_NAME} — CloudStudio 启动${NC}"
echo -e "${CYAN}========================================${NC}"

# ---------- 1. MySQL（Docker）----------
echo -e "${YELLOW}[1/4] 启动 MySQL...${NC}"
if ! command -v docker >/dev/null 2>&1; then
  echo -e "${RED}未检测到 Docker。请换「带 Docker 的 CloudStudio 模板」，或按 docs/cloudstudio-deploy.md 用 apt 装 MySQL。${NC}"
  exit 1
fi

if command -v docker-compose >/dev/null 2>&1; then
  COMPOSE="docker-compose"
else
  COMPOSE="docker compose"
fi

$COMPOSE up -d

echo -e "${YELLOW}等待 MySQL 就绪...${NC}"
for i in $(seq 1 60); do
  if docker exec campus-xianyu-mysql mysqladmin ping -h 127.0.0.1 -uroot -proot_change_me --silent 2>/dev/null; then
    echo -e "${GREEN}MySQL 已就绪${NC}"
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo -e "${RED}MySQL 启动超时，请执行: docker logs campus-xianyu-mysql${NC}"
    exit 1
  fi
  sleep 2
done

# 首次 docker 会自动跑 initdb；若数据卷已存在则跳过。这里再保险执行一次（可重复）
echo -e "${YELLOW}[2/4] 确认公共数据...${NC}"
TABLE_COUNT=$(docker exec campus-xianyu-mysql mysql -N -uroot -proot_change_me -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='campus_xianyu';" 2>/dev/null || echo 0)
if [ "$TABLE_COUNT" -lt 5 ]; then
  docker exec -i campus-xianyu-mysql mysql -uroot -proot_change_me < sql/01_schema.sql
  docker exec -i campus-xianyu-mysql mysql -uroot -proot_change_me < sql/02_seed_data.sql
  echo -e "${GREEN}已导入 01_schema + 02_seed_data${NC}"
else
  echo -e "${GREEN}库表已存在（${TABLE_COUNT} 张表），跳过导入${NC}"
fi

docker exec campus-xianyu-mysql mysql -uroot -proot_change_me -e "USE campus_xianyu; SELECT id,name FROM category LIMIT 8;"

# ---------- 3. 后端（可选）----------
echo -e "${YELLOW}[3/4] 检查后端...${NC}"
if [ -d "$PROJECT_DIR/backend" ]; then
  if [ -f "$PROJECT_DIR/backend/mvnw" ] || [ -f "$PROJECT_DIR/backend/pom.xml" ]; then
    echo -e "${CYAN}检测到 Spring Boot，尝试启动（需本机已装 JDK 17+）...${NC}"
    cd "$PROJECT_DIR/backend"
    if [ -f ./mvnw ]; then
      ./mvnw -q spring-boot:run &
    else
      mvn -q spring-boot:run &
    fi
    BACKEND_PID=$!
    echo -e "${GREEN}后端 PID: $BACKEND_PID（默认端口以 application.yml 为准，常见 8080）${NC}"
    cd "$PROJECT_DIR"
  else
    echo -e "${YELLOW}backend/ 存在但未识别为 Maven 项目，跳过${NC}"
  fi
else
  echo -e "${YELLOW}尚无 backend/（等 A 搭好 Spring Boot 后再启动）。今天数据库已可用。${NC}"
fi

# ---------- 4. 前端（可选）----------
echo -e "${YELLOW}[4/4] 检查前端...${NC}"
if [ -d "$PROJECT_DIR/frontend" ] && [ -f "$PROJECT_DIR/frontend/package.json" ]; then
  cd "$PROJECT_DIR/frontend"
  if [ ! -d node_modules ]; then
    npm install
  fi
  npm run dev -- --host 0.0.0.0 --port 5173 &
  FRONTEND_PID=$!
  echo -e "${GREEN}前端 PID: $FRONTEND_PID  http://0.0.0.0:5173${NC}"
  cd "$PROJECT_DIR"
else
  echo -e "${YELLOW}尚无 frontend/（等组里搭好 Vue 后再启动）。${NC}"
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}当前可用连接（同一工作空间内）：${NC}"
echo -e "  MySQL host: 127.0.0.1"
echo -e "  MySQL port: 3306"
echo -e "  database:   campus_xianyu"
echo -e "  user:       root"
echo -e "  password:   root_change_me   ${YELLOW}← 上线前务必修改 docker-compose.yml${NC}"
echo -e "${GREEN}========================================${NC}"
echo -e "在 CloudStudio 里把端口 ${CYAN}5173 / 8080${NC} 设为公开预览后，即可用手机浏览器访问。"
echo -e "按 Ctrl+C 不会自动停 Docker；停止库: docker compose down"

# 保持脚本前台，方便看日志（若前后端都没起，则 sleep）
if [ -z "${BACKEND_PID:-}" ] && [ -z "${FRONTEND_PID:-}" ]; then
  echo -e "${CYAN}仅数据库模式：保持运行中。结束请 Ctrl+C（MySQL 容器会继续在后台）。${NC}"
  tail -f /dev/null
fi

wait
