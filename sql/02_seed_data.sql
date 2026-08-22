-- ============================================================
-- 校园版咸鱼 - 公共基础数据（不含用户商品订单等动态数据）
-- 依赖: 先执行 01_schema.sql
-- 初始管理员密码请导入后立即修改（下方为 BCrypt 示例，明文 ChangeMe@123）
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

-- ----------------------------
-- 校区
-- ----------------------------
INSERT INTO `campus` (`id`, `name`, `sort_order`, `status`) VALUES
(1, '本部校区', 1, 'ENABLED'),
(2, '东校区',   2, 'ENABLED'),
(3, '西校区',   3, 'ENABLED'),
(4, '南校区',   4, 'ENABLED');

-- ----------------------------
-- 商品分类（一级 + 周边二级）
-- ----------------------------
INSERT INTO `category` (`id`, `parent_id`, `name`, `sort_order`, `status`) VALUES
(1,  0, '教材资料', 1,  'ENABLED'),
(2,  0, '数码电子', 2,  'ENABLED'),
(3,  0, '电脑外设', 3,  'ENABLED'),
(4,  0, '宿舍电器', 4,  'ENABLED'),
(5,  0, '生活日用', 5,  'ENABLED'),
(6,  0, '服饰鞋包', 6,  'ENABLED'),
(7,  0, '美妆护理', 7,  'ENABLED'),
(8,  0, '运动户外', 8,  'ENABLED'),
(9,  0, '文具办公', 9,  'ENABLED'),
(10, 0, '卡券票务', 10, 'ENABLED'),
(11, 0, '乐器文娱', 11, 'ENABLED'),
(12, 0, '周边收藏', 12, 'ENABLED'),
(13, 0, '其他闲置', 13, 'ENABLED'),
(14, 12, '海报挂件', 1, 'ENABLED'),
(15, 12, '吧唧徽章', 2, 'ENABLED'),
(16, 12, '应援服饰', 3, 'ENABLED'),
(17, 12, '卡片票根', 4, 'ENABLED'),
(18, 12, '手办模型', 5, 'ENABLED');

-- ----------------------------
-- 举报原因
-- ----------------------------
INSERT INTO `report_reason` (`id`, `name`, `sort_order`, `status`) VALUES
(1, '虚假信息',     1, 'ENABLED'),
(2, '涉嫌诈骗',     2, 'ENABLED'),
(3, '违禁物品',     3, 'ENABLED'),
(4, '人身攻击',     4, 'ENABLED'),
(5, '广告骚扰',     5, 'ENABLED'),
(6, '其他违规',     6, 'ENABLED');

-- ----------------------------
-- 初始管理员（仅开发用，答辩前务必改密）
-- username: admin
-- 明文密码: password
-- 下方为常见 BCrypt(strength=10) 对 "password" 的哈希
-- 若 A 同学后端使用的盐轮数/算法不同，用后端生成新哈希后执行 UPDATE
-- ----------------------------
INSERT INTO `user` (
  `id`, `username`, `password_hash`, `nickname`, `role`,
  `auth_status`, `status`
) VALUES (
  1,
  'admin',
  '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG',
  '系统管理员',
  'ADMIN',
  'APPROVED',
  'ACTIVE'
);
