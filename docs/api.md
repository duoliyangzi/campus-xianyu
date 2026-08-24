# 校园版咸鱼 REST API 初稿

> 本文档用于统一前后端接口约定。当前为小组开发初稿，后续如字段或路径调整，需要同步修改本文档。

## 1. 通用约定

### 1.1 基础规则

- 接口统一前缀：`/api`
- 请求数据格式：JSON
- 返回数据格式：JSON
- 字段命名：小驼峰，例如 `studentNo`、`authStatus`、`categoryId`
- 时间格式：`YYYY-MM-DD HH:mm:ss`
- 分页参数：`page`、`size`
- 枚举取值：以 `docs/enums.md` 为准

### 1.2 统一返回格式

成功：

```json
{
  "code": 0,
  "message": "成功",
  "data": {}
}
```

失败：

```json
{
  "code": 400,
  "message": "错误原因",
  "data": null
}
```

### 1.3 登录身份

登录成功后，后端返回当前用户信息和登录凭证。初期可以先用简单 token，后续再决定是否使用 JWT。

```json
{
  "token": "登录凭证",
  "user": {
    "id": 1,
    "username": "student01",
    "nickname": "小鱼同学",
    "role": "STUDENT",
    "authStatus": "UNAUTH",
    "status": "ACTIVE"
  }
}
```

前端后续请求需要在请求头携带：

```text
Authorization: Bearer 登录凭证
```

---

## 2. A 模块：用户认证与商品发布管理

### 2.1 学生注册

```text
POST /api/auth/register
```

请求：

```json
{
  "username": "student01",
  "password": "123456",
  "nickname": "小鱼同学",
  "phone": "13800000000"
}
```

返回：

```json
{
  "code": 0,
  "message": "注册成功",
  "data": {
    "id": 2,
    "username": "student01",
    "nickname": "小鱼同学",
    "phone": "13800000000",
    "role": "STUDENT",
    "authStatus": "UNAUTH",
    "status": "ACTIVE"
  }
}
```

说明：

- 注册用户默认 `role = STUDENT`
- 注册用户默认 `authStatus = UNAUTH`
- 注册用户默认 `status = ACTIVE`
- 密码后端必须加密存储到 `password_hash`，不能存明文

### 2.2 登录

```text
POST /api/auth/login
```

请求：

```json
{
  "username": "student01",
  "password": "123456"
}
```

返回：

```json
{
  "code": 0,
  "message": "登录成功",
  "data": {
    "token": "mock-token-or-jwt",
    "user": {
      "id": 2,
      "username": "student01",
      "nickname": "小鱼同学",
      "role": "STUDENT",
      "authStatus": "UNAUTH",
      "status": "ACTIVE"
    }
  }
}
```

### 2.3 获取当前登录用户

```text
GET /api/users/me
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 2,
    "username": "student01",
    "nickname": "小鱼同学",
    "avatarUrl": null,
    "phone": "13800000000",
    "role": "STUDENT",
    "studentNo": null,
    "college": null,
    "authStatus": "UNAUTH",
    "authRemark": null,
    "status": "ACTIVE",
    "createdAt": "2026-08-21 19:00:00"
  }
}
```

### 2.4 提交学生实名认证

```text
POST /api/users/auth
```

请求：

```json
{
  "studentNo": "20260001",
  "college": "计算机学院"
}
```

返回：

```json
{
  "code": 0,
  "message": "认证已提交，等待管理员审核",
  "data": {
    "id": 2,
    "studentNo": "20260001",
    "college": "计算机学院",
    "authStatus": "PENDING"
  }
}
```

说明：

- 提交后用户 `authStatus` 变为 `PENDING`
- 被拒绝 `REJECTED` 后允许重新提交

### 2.5 管理员查看认证申请

```text
GET /api/admin/users/auth?page=1&size=10&authStatus=PENDING
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 2,
        "username": "student01",
        "nickname": "小鱼同学",
        "studentNo": "20260001",
        "college": "计算机学院",
        "authStatus": "PENDING",
        "createdAt": "2026-08-21 19:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1
  }
}
```

### 2.6 管理员审核认证

```text
PUT /api/admin/users/{id}/auth
```

请求：

```json
{
  "authStatus": "APPROVED",
  "authRemark": "认证通过"
}
```

返回：

```json
{
  "code": 0,
  "message": "审核成功",
  "data": {
    "id": 2,
    "authStatus": "APPROVED",
    "authRemark": "认证通过"
  }
}
```

说明：

- `authStatus` 只能传 `APPROVED` 或 `REJECTED`
- 仅管理员可操作

### 2.7 发布商品

```text
POST /api/products
```

请求：

```json
{
  "title": "高等数学教材",
  "price": 25.00,
  "categoryId": 1,
  "conditionLevel": "GOOD",
  "campusId": 1,
  "tradeMethod": "FACE",
  "description": "几乎全新，无笔记",
  "coverUrl": "https://example.com/cover.jpg",
  "imageUrls": [
    "https://example.com/1.jpg",
    "https://example.com/2.jpg"
  ]
}
```

返回：

```json
{
  "code": 0,
  "message": "发布成功，等待管理员审核",
  "data": {
    "id": 1,
    "sellerId": 2,
    "title": "高等数学教材",
    "price": 25.00,
    "status": "PENDING"
  }
}
```

说明：

- 商品发布者为当前登录用户
- 建议只有 `authStatus = APPROVED` 的学生可以发布商品
- 新商品默认 `status = PENDING`

### 2.8 修改商品

```text
PUT /api/products/{id}
```

请求：

```json
{
  "title": "高等数学教材最新版",
  "price": 22.00,
  "categoryId": 1,
  "conditionLevel": "GOOD",
  "campusId": 1,
  "tradeMethod": "FACE",
  "description": "轻微笔记，适合复习",
  "coverUrl": "https://example.com/cover.jpg",
  "imageUrls": [
    "https://example.com/1.jpg"
  ]
}
```

返回：

```json
{
  "code": 0,
  "message": "修改成功",
  "data": {
    "id": 1,
    "title": "高等数学教材最新版",
    "price": 22.00,
    "status": "PENDING"
  }
}
```

说明：

- 只能修改自己发布的商品
- 修改后是否重新变为 `PENDING` 待审核，由小组确认；初稿建议重新待审核

### 2.9 下架商品

```text
DELETE /api/products/{id}
```

返回：

```json
{
  "code": 0,
  "message": "下架成功",
  "data": {
    "id": 1,
    "status": "OFF_SHELF"
  }
}
```

说明：

- 删除商品采用逻辑删除，即 `status = OFF_SHELF`
- 不物理删除数据库记录

### 2.10 商品详情

```text
GET /api/products/{id}
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "id": 1,
    "sellerId": 2,
    "sellerNickname": "小鱼同学",
    "title": "高等数学教材",
    "price": 25.00,
    "categoryId": 1,
    "categoryName": "教材",
    "conditionLevel": "GOOD",
    "campusId": 1,
    "campusName": "本部校区",
    "tradeMethod": "FACE",
    "description": "几乎全新，无笔记",
    "coverUrl": "https://example.com/cover.jpg",
    "imageUrls": [
      "https://example.com/1.jpg",
      "https://example.com/2.jpg"
    ],
    "status": "PUBLISHED",
    "viewCount": 12,
    "createdAt": "2026-08-21 19:00:00",
    "updatedAt": "2026-08-21 19:00:00"
  }
}
```

---

## 3. B 模块：搜索筛选与求购管理

### 3.1 商品列表、搜索、筛选、分页

```text
GET /api/products?keyword=教材&categoryId=1&minPrice=0&maxPrice=50&campusId=1&conditionLevel=GOOD&page=1&size=10
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "list": [
      {
        "id": 1,
        "title": "高等数学教材",
        "price": 25.00,
        "coverUrl": "https://example.com/cover.jpg",
        "categoryName": "教材",
        "campusName": "本部校区",
        "conditionLevel": "GOOD",
        "tradeMethod": "FACE",
        "createdAt": "2026-08-21 19:00:00"
      }
    ],
    "page": 1,
    "size": 10,
    "total": 1
  }
}
```

说明：

- 商品列表默认只展示 `status = PUBLISHED` 的商品

### 3.2 发布求购

```text
POST /api/wanted
```

请求：

```json
{
  "itemName": "二手自行车",
  "budget": 200.00,
  "expectCondition": "GOOD",
  "description": "希望车况正常，可以校内交易",
  "campusId": 1
}
```

返回：

```json
{
  "code": 0,
  "message": "发布成功",
  "data": {
    "id": 1,
    "itemName": "二手自行车",
    "status": "OPEN"
  }
}
```

### 3.3 求购列表

```text
GET /api/wanted?keyword=自行车&campusId=1&status=OPEN&page=1&size=10
```

### 3.4 求购详情

```text
GET /api/wanted/{id}
```

### 3.5 修改和管理自己的求购

```text
GET /api/wanted/mine
PUT /api/wanted/{id}
PUT /api/wanted/{id}/match
PUT /api/wanted/{id}/close
```

说明：

- 发布者可以修改状态为 `OPEN` 的求购。
- `PUT /api/wanted/{id}/match` 无需请求体，将状态从 `OPEN` 改为 `MATCHED`。
- `PUT /api/wanted/{id}/close` 无需请求体，将未关闭的求购改为 `CLOSED`。
- 当前 `MATCHED` 只表示发布者已找到卖家，尚未记录具体接单卖家。
- 其他学生可通过私聊接口联系求购发布者。

---

## 4. C 模块：互动交易与平台管理

### 4.1 商品留言

```text
POST /api/comments
GET /api/comments?productId=1&page=1&size=10
```

### 4.2 私聊会话与消息

```text
POST /api/messages/conversations
GET /api/messages/conversations
GET /api/messages/conversations/{id}
POST /api/messages/conversations/{id}/messages
```

### 4.3 订单管理

```text
POST /api/orders
GET /api/orders
GET /api/orders/{id}
PUT /api/orders/{id}/status
```

订单状态变更接口不直接接收目标状态，而是接收操作类型：

```json
{
  "action": "CONFIRM_TRADE",
  "meetTime": "2026-08-25 15:30:00",
  "meetLocation": "本部图书馆门口",
  "remark": "请提前联系"
}
```

`action` 取值：

```text
UPDATE_DETAILS
CONFIRM_TRADE
BUYER_CONFIRM_COMPLETE
SELLER_CONFIRM_COMPLETE
```

说明：

- `UPDATE_DETAILS`：仅买家可在待沟通阶段修改时间、地点和备注。
- `CONFIRM_TRADE`：买家和卖家分别确认交易约定；双方确认后才进入待交易。
- `BUYER_CONFIRM_COMPLETE`：仅买家在待交易阶段确认收货。
- `SELLER_CONFIRM_COMPLETE`：仅卖家在待交易阶段确认交易完成。
- 买卖双方都确认完成后，订单才变为 `COMPLETED`，关联商品随即下架。
- 同一买家对同一商品已有待沟通或待交易订单时，不能重复创建订单。

订单状态：

```text
PENDING_CHAT
PENDING_TRADE
COMPLETED
```

### 4.4 管理员审核商品

```text
GET /api/admin/products?status=PENDING&page=1&size=10
PUT /api/admin/products/{id}/audit
```

审核请求：

```json
{
  "status": "PUBLISHED",
  "auditRemark": "审核通过"
}
```

说明：

- `status` 可传 `PUBLISHED` 或 `REJECTED`

### 4.5 举报管理

```text
POST /api/reports
GET /api/admin/reports?status=PENDING&page=1&size=10
PUT /api/admin/reports/{id}/handle
```

### 4.6 分类管理

```text
GET /api/categories
POST /api/admin/categories
PUT /api/admin/categories/{id}
DELETE /api/admin/categories/{id}
```

### 4.7 用户管理

```text
GET /api/admin/users?page=1&size=10&keyword=student
PUT /api/admin/users/{id}/status
```

---

## 5. 公共字典接口

### 5.1 商品分类

```text
GET /api/categories
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "parentId": 0,
      "name": "教材",
      "children": []
    },
    {
      "id": 6,
      "parentId": 0,
      "name": "周边",
      "children": [
        {
          "id": 7,
          "parentId": 6,
          "name": "海报"
        }
      ]
    }
  ]
}
```

### 5.2 校区列表

```text
GET /api/campuses
```

返回：

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {
      "id": 1,
      "name": "本部校区"
    },
    {
      "id": 2,
      "name": "东校区"
    }
  ]
}
```

---

## 6. 待小组确认的问题

1. 登录凭证使用简单 token 还是 JWT？
2. 商品修改后是否重新进入 `PENDING` 审核？初稿建议重新审核。
3. 管理端接口是否统一放在 `/api/admin` 下？初稿建议统一放这里。
4. 图片上传暂时用 URL 字段，还是后续补充上传接口？
5. AI 智能审核功能目前是否彻底不做？如果不做，应删除或忽略 `/api/ai-audit`。
6. 最终部署是否直接使用腾讯云轻量服务器，不再使用 CloudStudio 联调？
