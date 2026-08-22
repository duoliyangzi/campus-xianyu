-- ============================================================
-- 校园版咸鱼 - 扩充商品分类
-- 用于已经初始化过数据库的同学，不想重建库时单独执行本文件即可。
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

UPDATE `category` SET `name` = '教材资料', `parent_id` = 0, `sort_order` = 1, `status` = 'ENABLED' WHERE `id` = 1;
UPDATE `category` SET `name` = '数码电子', `parent_id` = 0, `sort_order` = 2, `status` = 'ENABLED' WHERE `id` = 2;
UPDATE `category` SET `name` = '电脑外设', `parent_id` = 0, `sort_order` = 3, `status` = 'ENABLED' WHERE `id` = 3;
UPDATE `category` SET `name` = '宿舍电器', `parent_id` = 0, `sort_order` = 4, `status` = 'ENABLED' WHERE `id` = 4;
UPDATE `category` SET `name` = '生活日用', `parent_id` = 0, `sort_order` = 5, `status` = 'ENABLED' WHERE `id` = 5;
UPDATE `category` SET `name` = '服饰鞋包', `parent_id` = 0, `sort_order` = 6, `status` = 'ENABLED' WHERE `id` = 6;
UPDATE `category` SET `name` = '美妆护理', `parent_id` = 0, `sort_order` = 7, `status` = 'ENABLED' WHERE `id` = 7;
UPDATE `category` SET `name` = '运动户外', `parent_id` = 0, `sort_order` = 8, `status` = 'ENABLED' WHERE `id` = 8;
UPDATE `category` SET `name` = '文具办公', `parent_id` = 0, `sort_order` = 9, `status` = 'ENABLED' WHERE `id` = 9;
UPDATE `category` SET `name` = '卡券票务', `parent_id` = 0, `sort_order` = 10, `status` = 'ENABLED' WHERE `id` = 10;
UPDATE `category` SET `name` = '乐器文娱', `parent_id` = 0, `sort_order` = 11, `status` = 'ENABLED' WHERE `id` = 11;

INSERT INTO `category` (`id`, `parent_id`, `name`, `sort_order`, `status`) VALUES
(12, 0, '周边收藏', 12, 'ENABLED'),
(13, 0, '其他闲置', 13, 'ENABLED'),
(14, 12, '海报挂件', 1, 'ENABLED'),
(15, 12, '吧唧徽章', 2, 'ENABLED'),
(16, 12, '应援服饰', 3, 'ENABLED'),
(17, 12, '卡片票根', 4, 'ENABLED'),
(18, 12, '手办模型', 5, 'ENABLED')
ON DUPLICATE KEY UPDATE
  `parent_id` = VALUES(`parent_id`),
  `name` = VALUES(`name`),
  `sort_order` = VALUES(`sort_order`),
  `status` = VALUES(`status`);