-- ============================================================
-- 校园版咸鱼 - 表结构
-- 执行顺序: 01_schema.sql → 02_seed_data.sql
-- 字符集: utf8mb4
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_xianyu
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE campus_xianyu;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 用户（A：注册登录 / 实名认证）
-- role / auth_status / status 为代码枚举，库内用 VARCHAR 存约定值
-- role: STUDENT | ADMIN
-- auth_status: UNAUTH | PENDING | APPROVED | REJECTED
-- status: ACTIVE | BANNED
-- ----------------------------
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username`      VARCHAR(50)  NOT NULL,
  `password_hash` VARCHAR(100) NOT NULL COMMENT 'BCrypt等哈希，禁止存明文',
  `nickname`      VARCHAR(50)  DEFAULT NULL,
  `avatar_url`    VARCHAR(255) DEFAULT NULL,
  `phone`         VARCHAR(20)  DEFAULT NULL,
  `role`          VARCHAR(20)  NOT NULL DEFAULT 'STUDENT',
  `student_no`    VARCHAR(30)  DEFAULT NULL COMMENT '学号',
  `college`       VARCHAR(100) DEFAULT NULL COMMENT '学院',
  `auth_status`   VARCHAR(20)  NOT NULL DEFAULT 'UNAUTH',
  `auth_remark`   VARCHAR(255) DEFAULT NULL COMMENT '认证审核备注',
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_student_no` (`student_no`),
  KEY `idx_user_role` (`role`),
  KEY `idx_user_auth_status` (`auth_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户';

-- ----------------------------
-- 商品分类（公共可配置；支持二级，如周边→吧唧）
-- status: ENABLED | DISABLED
-- ----------------------------
DROP TABLE IF EXISTS `category`;
CREATE TABLE `category` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `parent_id`  BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0表示一级分类',
  `name`       VARCHAR(50)  NOT NULL,
  `sort_order` INT          NOT NULL DEFAULT 0,
  `status`     VARCHAR(20)  NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_category_parent` (`parent_id`),
  KEY `idx_category_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类';

-- ----------------------------
-- 校区（公共可配置）
-- ----------------------------
DROP TABLE IF EXISTS `campus`;
CREATE TABLE `campus` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(50) NOT NULL,
  `sort_order` INT         NOT NULL DEFAULT 0,
  `status`     VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_campus_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='校区';

-- ----------------------------
-- 举报原因类型（公共可配置）
-- ----------------------------
DROP TABLE IF EXISTS `report_reason`;
CREATE TABLE `report_reason` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name`       VARCHAR(50) NOT NULL,
  `sort_order` INT         NOT NULL DEFAULT 0,
  `status`     VARCHAR(20) NOT NULL DEFAULT 'ENABLED',
  `created_at` DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_report_reason_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='举报原因';

-- ----------------------------
-- 商品（A：发布管理；C：审核）
-- condition_level: NEW | LIKE_NEW | GOOD | FAIR | POOR  （新旧程度，代码枚举）
-- trade_method: FACE | MAIL | BOTH                  （交易方式，代码枚举）
-- status: PENDING | PUBLISHED | OFF_SHELF | REJECTED （商品状态，代码枚举）
-- ----------------------------
DROP TABLE IF EXISTS `product`;
CREATE TABLE `product` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `seller_id`        BIGINT UNSIGNED NOT NULL,
  `title`            VARCHAR(100) NOT NULL,
  `price`            DECIMAL(10,2) NOT NULL,
  `category_id`      BIGINT UNSIGNED NOT NULL,
  `condition_level`  VARCHAR(20)  NOT NULL,
  `campus_id`        BIGINT UNSIGNED NOT NULL,
  `trade_method`     VARCHAR(20)  NOT NULL,
  `description`      TEXT         NOT NULL,
  `cover_url`        VARCHAR(255) DEFAULT NULL,
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  `audit_remark`     VARCHAR(255) DEFAULT NULL,
  `view_count`       INT UNSIGNED NOT NULL DEFAULT 0,
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_seller` (`seller_id`),
  KEY `idx_product_category` (`category_id`),
  KEY `idx_product_campus` (`campus_id`),
  KEY `idx_product_status` (`status`),
  KEY `idx_product_price` (`price`),
  KEY `idx_product_condition` (`condition_level`),
  KEY `idx_product_created` (`created_at`),
  KEY `idx_product_title` (`title`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='二手商品';
-- 关键词搜索建议用 LIKE 或后续再加 ngram FULLTEXT，避免初始化环境差异导致建表失败

-- ----------------------------
-- 商品图片
-- ----------------------------
DROP TABLE IF EXISTS `product_image`;
CREATE TABLE `product_image` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `url`        VARCHAR(255) NOT NULL,
  `sort_order` INT          NOT NULL DEFAULT 0,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product_image_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品图片';

-- ----------------------------
-- 求购（B）
-- expect_condition: 同商品 condition_level 枚举
-- status: OPEN | CLOSED | MATCHED
-- ----------------------------
DROP TABLE IF EXISTS `wanted`;
CREATE TABLE `wanted` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `buyer_id`         BIGINT UNSIGNED NOT NULL,
  `item_name`        VARCHAR(100) NOT NULL,
  `budget`           DECIMAL(10,2) NOT NULL,
  `expect_condition` VARCHAR(20)  NOT NULL,
  `description`      VARCHAR(500) DEFAULT NULL,
  `campus_id`        BIGINT UNSIGNED DEFAULT NULL,
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'OPEN',
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wanted_buyer` (`buyer_id`),
  KEY `idx_wanted_status` (`status`),
  KEY `idx_wanted_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求购';

-- ----------------------------
-- 商品留言（C）
-- ----------------------------
DROP TABLE IF EXISTS `comment`;
CREATE TABLE `comment` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id` BIGINT UNSIGNED NOT NULL,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `content`    VARCHAR(500) NOT NULL,
  `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_comment_product` (`product_id`),
  KEY `idx_comment_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品留言';

-- ----------------------------
-- 私聊会话（C）
-- 可关联商品或求购（至少一个可为空）
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_a_id`     BIGINT UNSIGNED NOT NULL COMMENT '较小用户ID，便于去重',
  `user_b_id`     BIGINT UNSIGNED NOT NULL COMMENT '较大用户ID',
  `product_id`    BIGINT UNSIGNED DEFAULT NULL,
  `wanted_id`     BIGINT UNSIGNED DEFAULT NULL,
  `last_msg_at`   DATETIME        DEFAULT NULL,
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_conv_users` (`user_a_id`, `user_b_id`),
  KEY `idx_conv_product` (`product_id`),
  KEY `idx_conv_wanted` (`wanted_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私聊会话';

-- ----------------------------
-- 私聊消息（C）
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `conversation_id` BIGINT UNSIGNED NOT NULL,
  `sender_id`       BIGINT UNSIGNED NOT NULL,
  `content`         VARCHAR(1000) NOT NULL,
  `is_read`         TINYINT(1)    NOT NULL DEFAULT 0,
  `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_message_conv` (`conversation_id`),
  KEY `idx_message_sender` (`sender_id`),
  KEY `idx_message_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私聊消息';

-- ----------------------------
-- 订单 / 线下交易约定（C）
-- status: PENDING_CHAT | PENDING_TRADE | COMPLETED  （待沟通/待交易/已完成）
-- ----------------------------
DROP TABLE IF EXISTS `trade_order`;
CREATE TABLE `trade_order` (
  `id`               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no`         VARCHAR(32)  NOT NULL,
  `product_id`       BIGINT UNSIGNED NOT NULL,
  `buyer_id`         BIGINT UNSIGNED NOT NULL,
  `seller_id`        BIGINT UNSIGNED NOT NULL,
  `status`           VARCHAR(20)  NOT NULL DEFAULT 'PENDING_CHAT',
  `meet_time`        DATETIME     DEFAULT NULL COMMENT '约定见面时间',
  `meet_location`    VARCHAR(200) DEFAULT NULL COMMENT '约定地点',
  `remark`           VARCHAR(255) DEFAULT NULL,
  `conversation_id`  BIGINT UNSIGNED DEFAULT NULL,
  `created_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_order_product` (`product_id`),
  KEY `idx_order_buyer` (`buyer_id`),
  KEY `idx_order_seller` (`seller_id`),
  KEY `idx_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='交易订单';

-- ----------------------------
-- 举报（C）
-- target_type: PRODUCT | USER | WANTED | COMMENT
-- status: PENDING | RESOLVED | REJECTED
-- ----------------------------
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
  `id`            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `reporter_id`   BIGINT UNSIGNED NOT NULL,
  `reason_id`     BIGINT UNSIGNED NOT NULL,
  `target_type`   VARCHAR(20)  NOT NULL,
  `target_id`     BIGINT UNSIGNED NOT NULL,
  `description`   VARCHAR(500) DEFAULT NULL,
  `status`        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
  `handler_id`    BIGINT UNSIGNED DEFAULT NULL,
  `handle_result` VARCHAR(255) DEFAULT NULL,
  `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_report_status` (`status`),
  KEY `idx_report_target` (`target_type`, `target_id`),
  KEY `idx_report_reporter` (`reporter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='违规举报';

-- ----------------------------
-- AI 智能审核记录（C）
-- risk_level: NONE | LOW | MEDIUM | HIGH
-- suggestion: PASS | REVIEW | REJECT
-- ----------------------------
DROP TABLE IF EXISTS `ai_audit_log`;
CREATE TABLE `ai_audit_log` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `product_id`   BIGINT UNSIGNED NOT NULL,
  `title`        VARCHAR(100) NOT NULL,
  `content_snap` TEXT         NOT NULL COMMENT '送审时的描述快照',
  `risk_level`   VARCHAR(20)  NOT NULL DEFAULT 'NONE',
  `suggestion`   VARCHAR(20)  NOT NULL DEFAULT 'REVIEW',
  `reason`       VARCHAR(500) DEFAULT NULL COMMENT '风险说明',
  `raw_response` TEXT         DEFAULT NULL COMMENT '模型原始返回，可空',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_audit_product` (`product_id`),
  KEY `idx_ai_audit_suggestion` (`suggestion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI审核日志';

SET FOREIGN_KEY_CHECKS = 1;
