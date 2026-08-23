# 固定业务枚举（写在前后端代码，不进 seed 表）

管理员后台不要提供「新增订单状态」之类入口。

## 用户

| 字段 | 值 | 含义 |
|------|-----|------|
| role | STUDENT | 普通学生 |
| role | ADMIN | 管理员 |
| auth_status | UNAUTH | 未提交认证 |
| auth_status | PENDING | 待审核 |
| auth_status | APPROVED | 审核通过 |
| auth_status | REJECTED | 审核拒绝 |
| status | ACTIVE | 正常 |
| status | BANNED | 封禁 |

## 商品

| 字段 | 值 | 含义 |
|------|-----|------|
| condition_level | NEW | 全新 |
| condition_level | LIKE_NEW | 几乎全新 |
| condition_level | GOOD | 轻度使用 |
| condition_level | FAIR | 明显使用痕迹 |
| condition_level | POOR | 成色一般 |
| trade_method | FACE | 当面交易 |
| trade_method | MAIL | 邮寄 |
| trade_method | BOTH | 均可 |
| status | PENDING | 待审核 |
| status | PUBLISHED | 已发布 |
| status | OFF_SHELF | 已下架 |
| status | REJECTED | 审核拒绝 |
| status | DELETED | 已删除（软删除，不再对用户展示） |

文档中的「疑似违规」建议体现在 AI 审核 `suggestion=REVIEW` + `risk_level`，商品仍保持 `PENDING`，由管理员确认。

## 订单（C）

| 值 | 中文 |
|----|------|
| PENDING_CHAT | 待沟通 |
| PENDING_TRADE | 待交易 |
| COMPLETED | 已完成 |

## 求购

| 值 | 含义 |
|----|------|
| OPEN | 求购中 |
| MATCHED | 已有人接单沟通 |
| CLOSED | 已关闭 |

## 举报

| 值 | 含义 |
|----|------|
| PENDING | 待处理 |
| RESOLVED | 已处理 |
| REJECTED | 驳回 |

## AI 审核

| 字段 | 值 |
|------|-----|
| risk_level | NONE / LOW / MEDIUM / HIGH |
| suggestion | PASS / REVIEW / REJECT |

## 接口路径（统一）

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
