-- ============================================================
-- 演示数据（可选）- 答辩前再准备，不要在开发初期强制导入
-- 依赖: 01_schema.sql + 02_seed_data.sql
-- 此处仅留结构说明，避免污染公共初始化
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

-- 示例（按需取消注释并改成真实 BCrypt）：
-- INSERT INTO `user` (`username`, `password_hash`, `nickname`, `role`, `student_no`, `college`, `auth_status`, `status`)
-- VALUES ('student01', '<BCrypt>', '测试同学', 'STUDENT', '20260001', '计算机学院', 'APPROVED', 'ACTIVE');

-- INSERT INTO `product` (`seller_id`, `title`, `price`, `category_id`, `condition_level`, `campus_id`, `trade_method`, `description`, `status`)
-- VALUES (2, '高等数学教材', 25.00, 1, 'GOOD', 1, 'FACE', '几乎全新，无笔记', 'PUBLISHED');
