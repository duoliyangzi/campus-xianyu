-- ============================================================
-- C 模块增量迁移（在已有库上执行，勿重跑 01/02 初始化脚本）
-- 订单双方确认字段
-- 若列已存在会报错，可忽略对应语句
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

ALTER TABLE `trade_order`
  ADD COLUMN `buyer_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '买家确认完成' AFTER `conversation_id`,
  ADD COLUMN `seller_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '卖家确认完成' AFTER `buyer_confirmed`;
