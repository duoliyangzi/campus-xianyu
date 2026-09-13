-- ============================================================
-- AI 客服会话落库（在已有库上执行，勿重跑 01_schema.sql）
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id`         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id`    BIGINT UNSIGNED NOT NULL,
  `title`      VARCHAR(100)  NOT NULL DEFAULT '默认会话',
  `created_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_session_user` (`user_id`),
  CONSTRAINT `fk_ai_session_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服会话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `session_id`   BIGINT UNSIGNED NOT NULL,
  `role`         VARCHAR(20)  NOT NULL COMMENT 'user|assistant',
  `content`      TEXT         NOT NULL,
  `intent`       VARCHAR(40)  DEFAULT NULL,
  `payload_json` MEDIUMTEXT   DEFAULT NULL COMMENT 'listing/wanted/products/citations 等 JSON',
  `created_at`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_ai_msg_session_created` (`session_id`, `created_at`),
  CONSTRAINT `fk_ai_msg_session` FOREIGN KEY (`session_id`) REFERENCES `ai_chat_session` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI客服消息';
