# C 模块 · 本地功能完善说明 & B 同学重新部署指南

> 分支：`feature/c-followup`  
> 基于已合并的 `main`（含 A/B 整合与 B 的云部署配置）  
> 编写日期：2026-08-24

---

## 一、C 在最新整合版本上完成的内容

### 订单与交易流程

1. **商品详情页交易约定结构调整**：详情页只保留「联系卖家 / 我要购买 / 举报商品」；点击「我要购买」后弹出独立的「填写交易约定」弹窗，不再在详情页直接展示约定地点、时间、备注。
2. **交易约定弹窗体验优化**：增加右上角 **× 关闭按钮**；「约定时间」拆分为 **日期 + 时间** 两个输入框，避免手机端 `datetime-local` 没有确定按钮的问题。
3. **订单状态改为双方确认机制**：
   - **待沟通 → 待交易**：买家、卖家都需点击「确认交易约定」，单方确认时显示「您已确认，等待对方…」，双方都确认后才进入待交易。
   - **待交易 → 已完成**：买家点「确认收货」、卖家点「确认交易完成」，双方都确认后订单才完成，商品才会下架。
4. **防止重复下单**：同一买家对同一商品若已有进行中订单（待沟通 / 待交易），再次购买会提示「该商品已有进行中的订单，请在「我的订单」查看」，避免同一交易在列表里出现多条记录。

### 分类管理

5. **分类服务行为修正**：去掉启动时自动重排；管理员读取分类列表时 **只查询、不删除** DISABLED 数据；删除仍走明确的删除接口。

### 前端交互与认证

6. **登录令牌使用 sessionStorage**（与 A/B 整合版一致）：同一浏览器不同标签页可分别登录不同账号；令牌失效时自动退出并返回登录页；权限不足时保留当前账号并跳回对应角色页面。
7. **未实名认证限制扩展**：未认证用户点击商品详情中的「联系卖家 / 我要购买 / 举报商品 / 发表留言」，以及进入「消息」页，都会提示先实名认证（风格与求购页一致）。
8. **删除「我的」页开发进度文案**：「留言、私聊、订单与举报功能已接入。」已移除。

### 数据库

9. **新增增量迁移脚本** `sql/06_c_followup.sql`（在已有库上执行，**不要**重跑 `01/02` 初始化脚本）：
   - `trade_order.buyer_confirmed` / `seller_confirmed`

### 接口变更（B/A 联调注意）

| 变更点 | 说明 |
|--------|------|
| `PUT /api/orders/{id}/status` | 不再传 `{ status }`，改为传 `{ action }` |
| action 取值 | `CONFIRM_TRADE`（双方确认交易约定）、`BUYER_CONFIRM_COMPLETE`、`SELLER_CONFIRM_COMPLETE`、`UPDATE_DETAILS` |
| `OrderResponse` | 新增 `buyerConfirmed`、`sellerConfirmed` |
| `POST /api/orders` | 同一买家 + 同一商品存在进行中订单时会返回错误 |

> 求购 `PUT /api/wanted/{id}/match` **保持原样**，无需传 body，仅将状态改为 `MATCHED`。

### 改动文件清单

**新增**

- `sql/06_c_followup.sql`

**后端修改**

- `backend/src/main/java/com/campus/xianyu/dict/CategoryService.java`
- `backend/src/main/java/com/campus/xianyu/order/OrderController.java`
- `backend/src/main/java/com/campus/xianyu/order/OrderRepository.java`
- `backend/src/main/java/com/campus/xianyu/order/OrderStatusRequest.java`
- `backend/src/main/java/com/campus/xianyu/order/OrderResponse.java`
- `backend/src/main/java/com/campus/xianyu/order/TradeOrder.java`

**前端修改**

- `frontend/src/App.vue`
- `frontend/src/style.css`
- `frontend/vite.config.js`（本地临时改 8081，**提交前改回 8080**）

### 本地开发说明（仅 C 本机）

10. 因本机 **8080 被 VMware 占用**，本地临时将 `frontend/vite.config.js` 代理指向 **8081**，后端用 `SERVER_PORT=8081` 启动。**提交到 GitHub 前会改回 8080**，不影响云服务器和其他同学。

### 本次未改动（仍属 A/B 或后续共同确认）

- JWT 实现（A/B 已在 `main` 中完成）
- 商品软删除（A 负责）
- 求购接单卖家关联（`matched_seller_id`，暂不实现）
- 云部署 Nginx / 环境变量配置（B 已在服务器配好）
- 审核历史表、分类高级管理（父分类 / 排序 / 启停）等文档承诺但未实现项

---

## 二、B 同学重新部署步骤

> 前提：C 的代码已 push 并合并到 `main`（或 B 直接拉 `feature/c-followup` 分支测试）。

### 1. 拉取最新代码

```bash
cd /path/to/campus-xianyu
git pull origin main
```

### 2. 执行数据库增量迁移（必做）

在服务器 MySQL 上执行 **一次**，不要重跑 `01_schema.sql` / `02_seed_data.sql`：

```bash
mysql -u root -p campus_xianyu < sql/06_c_followup.sql
```

若列已存在会报错，可忽略对应语句。执行后确认：

```sql
SHOW COLUMNS FROM trade_order LIKE 'buyer_confirmed';
```

### 3. 重新构建前端

```bash
cd frontend
npm install
npm run build
```

构建产物在 `frontend/dist/`，由 Nginx 托管静态文件（与之前一致）。

### 4. 重启后端

确保环境变量仍正确（与之前部署相同，**不要写进 GitHub**）：

- `DB_PASSWORD`
- `JWT_SECRET`
- `UPLOAD_DIR`（图片持久化目录）

```bash
cd backend
./mvnw -q -DskipTests package
# 然后按你们现有的 systemd / 启动脚本重启，例如：
sudo systemctl restart campus-xianyu
```

服务器上后端仍监听 **8080**，Nginx 继续把 `/api` 和 `/uploads` 反代到本机 8080，**无需改 Nginx**（除非 C 提交里误带了 8081 的 vite 配置——合并前 C 会改回 8080）。

### 5. 验证清单

| 检查项 | 预期 |
|--------|------|
| 公网首页 | http://152.136.123.193 可打开 |
| 分类接口 | `/api/categories` 返回正常 |
| 商品详情 | 「我要购买」弹出交易约定弹窗，有关闭按钮 |
| 订单流程 | 买卖双方各确认一次才能进入待交易；完成也需双方确认 |
| 未认证用户 | 联系卖家 / 购买 / 举报 / 留言 / 消息页均提示先认证 |

### 6. 发给 A/C 的联调提醒

- 订单状态接口已改为 `action` 模式，若有 Apifox / 文档请同步更新。
- 本地开发若 8080 被占用，可临时用 8081，**仓库内 vite 代理应保持 8080**。

---

## 三、C 提交前自检清单

- [ ] `frontend/vite.config.js` 代理改回 `http://127.0.0.1:8080`
- [ ] 本地跑通：购买弹窗 → 双方确认交易约定 → 双方确认完成
- [ ] 本地跑通：未认证提示
- [ ] `git push` → 提 PR 合并到 `main` → 通知 B 按第二节步骤部署

---

## 四、本地开发快速启动（C 本机）

```powershell
# 1. 数据库迁移（首次）
mysql -u root -p campus_xianyu < sql/06_c_followup.sql

# 2. 后端（8080 被占用时用 8081）
cd backend
$env:DB_PASSWORD="你的MySQL密码"
$env:SERVER_PORT="8081"
.\mvnw.cmd spring-boot:run

# 3. 前端
cd frontend
npm install
npm run dev
```

浏览器打开：http://127.0.0.1:5173

测试管理员：`admin` / `password`
