-- ============================================================
-- 校园版咸鱼 - 本地验收测试数据
-- 依赖：现有 campus_xianyu 已完成 01_schema.sql + 02_seed_data.sql；旧库先执行 04_expand_categories.sql
-- 用途：覆盖学生认证、商品审核/筛选、求购、留言、私聊、订单、举报
-- 安全：仅重置 username 以 qa_ 开头的专用测试账号及其关联数据
-- 所有测试账号明文密码统一为：password
-- ============================================================

USE campus_xianyu;
SET NAMES utf8mb4;

-- ---------- 清理上一轮 QA 数据（不影响真实账号） ----------
SET @qa_seller_1 = (SELECT id FROM `user` WHERE username = 'qa_seller_1' LIMIT 1);
SET @qa_seller_2 = (SELECT id FROM `user` WHERE username = 'qa_seller_2' LIMIT 1);
SET @qa_buyer_1  = (SELECT id FROM `user` WHERE username = 'qa_buyer_1' LIMIT 1);
SET @qa_pending  = (SELECT id FROM `user` WHERE username = 'qa_pending' LIMIT 1);
SET @qa_rejected = (SELECT id FROM `user` WHERE username = 'qa_rejected' LIMIT 1);
SET @qa_banned   = (SELECT id FROM `user` WHERE username = 'qa_banned' LIMIT 1);

DELETE m FROM `message` m
JOIN `conversation` c ON c.id = m.conversation_id
WHERE c.user_a_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned)
   OR c.user_b_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `trade_order`
WHERE buyer_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned)
   OR seller_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `report`
WHERE reporter_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `comment`
WHERE user_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `conversation`
WHERE user_a_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned)
   OR user_b_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE l FROM `ai_audit_log` l
JOIN `product` p ON p.id = l.product_id
WHERE p.seller_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE i FROM `product_image` i
JOIN `product` p ON p.id = i.product_id
WHERE p.seller_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `product`
WHERE seller_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `wanted`
WHERE buyer_id IN (@qa_seller_1,@qa_seller_2,@qa_buyer_1,@qa_pending,@qa_rejected,@qa_banned);
DELETE FROM `user` WHERE username LIKE 'qa\_%';

-- ---------- 用户 ----------
-- BCrypt(strength=10) for plaintext "password"
SET @qa_password_hash = '$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG';
SET @qa_admin = (SELECT id FROM `user` WHERE username = 'admin' LIMIT 1);

INSERT INTO `user`
(`username`,`password_hash`,`nickname`,`avatar_url`,`phone`,`role`,`student_no`,`college`,`auth_status`,`auth_remark`,`status`) VALUES
('qa_seller_1', @qa_password_hash, '测试卖家小林', 'https://api.dicebear.com/9.x/initials/svg?seed=Lin',  '13800001001', 'STUDENT', 'QA2026001', '计算机学院', 'APPROVED', '验收数据：认证通过', 'ACTIVE'),
('qa_seller_2', @qa_password_hash, '测试卖家小周', 'https://api.dicebear.com/9.x/initials/svg?seed=Zhou', '13800001002', 'STUDENT', 'QA2026002', '经济管理学院', 'APPROVED', '验收数据：认证通过', 'ACTIVE'),
('qa_buyer_1',  @qa_password_hash, '测试买家小陈', 'https://api.dicebear.com/9.x/initials/svg?seed=Chen', '13800001003', 'STUDENT', 'QA2026003', '外国语学院', 'APPROVED', '验收数据：认证通过', 'ACTIVE'),
('qa_pending',  @qa_password_hash, '待审核学生',   'https://api.dicebear.com/9.x/initials/svg?seed=Pending', '13800001004', 'STUDENT', 'QA2026004', '设计学院', 'PENDING', '验收数据：等待管理员审核', 'ACTIVE'),
('qa_rejected', @qa_password_hash, '未通过学生',   'https://api.dicebear.com/9.x/initials/svg?seed=Rejected', '13800001005', 'STUDENT', 'QA2026005', '材料学院', 'REJECTED', '验收数据：材料不清晰', 'ACTIVE'),
('qa_banned',   @qa_password_hash, '封禁测试账号', 'https://api.dicebear.com/9.x/initials/svg?seed=Banned', '13800001006', 'STUDENT', 'QA2026006', '法学院', 'APPROVED', '验收数据：认证通过', 'BANNED');

SET @qa_seller_1 = (SELECT id FROM `user` WHERE username = 'qa_seller_1');
SET @qa_seller_2 = (SELECT id FROM `user` WHERE username = 'qa_seller_2');
SET @qa_buyer_1  = (SELECT id FROM `user` WHERE username = 'qa_buyer_1');
SET @qa_pending  = (SELECT id FROM `user` WHERE username = 'qa_pending');

-- ---------- 商品：公开、待审核、拒绝、下架及多筛选组合 ----------
INSERT INTO `product`
(`seller_id`,`title`,`price`,`category_id`,`condition_level`,`campus_id`,`trade_method`,`description`,`cover_url`,`status`,`audit_remark`,`view_count`) VALUES
(@qa_seller_1, '高等数学教材第八版', 25.00, 1, 'GOOD',     1, 'FACE', '少量笔记，页面完整，适合课程学习。', NULL, 'PUBLISHED', '验收数据：审核通过', 18),
(@qa_seller_1, '九成新机械键盘',    168.00, 3, 'LIKE_NEW', 2, 'BOTH', '青轴键盘，功能正常，可当面试用。', NULL, 'PUBLISHED', '验收数据：审核通过', 32),
(@qa_seller_2, '宿舍小功率台灯',     35.00, 4, 'GOOD',     3, 'FACE', '三档亮度，USB供电，宿舍自提。', NULL, 'PUBLISHED', '验收数据：审核通过', 9),
(@qa_seller_2, '校园音乐节票根',     12.00,17, 'LIKE_NEW', 1, 'MAIL', '收藏保存良好，可邮寄。', NULL, 'PUBLISHED', '验收数据：审核通过', 6),
(@qa_seller_1, '待审核蓝牙耳机',     89.00, 2, 'GOOD',     2, 'BOTH', '用于测试管理员商品审核流程。', NULL, 'PENDING', '等待管理员审核', 0),
(@qa_seller_2, '审核拒绝测试商品',  999.00,13, 'NEW',      4, 'MAIL', '用于检查拒绝状态和原因显示。', NULL, 'REJECTED', '描述信息不完整，请修改后重提', 0),
(@qa_seller_1, '已下架测试商品',      8.00, 5, 'FAIR',     1, 'FACE', '用于检查公开列表隐藏和我的商品显示。', NULL, 'OFF_SHELF', '验收数据：卖家主动下架', 3);

SET @p_book = (SELECT id FROM `product` WHERE seller_id=@qa_seller_1 AND title='高等数学教材第八版' LIMIT 1);
SET @p_keyboard = (SELECT id FROM `product` WHERE seller_id=@qa_seller_1 AND title='九成新机械键盘' LIMIT 1);
SET @p_lamp = (SELECT id FROM `product` WHERE seller_id=@qa_seller_2 AND title='宿舍小功率台灯' LIMIT 1);
SET @p_pending = (SELECT id FROM `product` WHERE seller_id=@qa_seller_1 AND title='待审核蓝牙耳机' LIMIT 1);

INSERT INTO `product_image` (`product_id`,`url`,`sort_order`) VALUES
(@p_book,     'https://picsum.photos/seed/qa-book/800/600', 1),
(@p_keyboard, 'https://picsum.photos/seed/qa-keyboard-1/800/600', 1),
(@p_keyboard, 'https://picsum.photos/seed/qa-keyboard-2/800/600', 2),
(@p_lamp,     'https://picsum.photos/seed/qa-lamp/800/600', 1);

INSERT INTO `ai_audit_log`
(`product_id`,`title`,`content_snap`,`risk_level`,`suggestion`,`reason`) VALUES
(@p_book, '高等数学教材第八版', '少量笔记，页面完整，适合课程学习。', 'NONE', 'PASS', '未发现风险关键词'),
(@p_pending, '待审核蓝牙耳机', '用于测试管理员商品审核流程。', 'LOW', 'REVIEW', '建议管理员人工确认商品描述');

-- ---------- 求购：开放、已匹配、已关闭 ----------
INSERT INTO `wanted`
(`buyer_id`,`item_name`,`budget`,`expect_condition`,`description`,`campus_id`,`status`) VALUES
(@qa_buyer_1, '求购二手自行车', 350.00, 'GOOD', '车况正常即可，优先本部校区。', 1, 'OPEN'),
(@qa_buyer_1, '求购英语六级资料', 40.00, 'FAIR', '真题和词汇书均可。', 2, 'OPEN'),
(@qa_seller_2, '求购显示器支架', 80.00, 'GOOD', '已经联系到卖家，用于测试匹配状态。', 3, 'MATCHED'),
(@qa_seller_2, '求购宿舍收纳箱', 30.00, 'GOOD', '求购已结束，用于测试关闭状态。', 4, 'CLOSED');

SET @w_bike = (SELECT id FROM `wanted` WHERE buyer_id=@qa_buyer_1 AND item_name='求购二手自行车' LIMIT 1);

-- ---------- 留言、私聊、订单、举报 ----------
INSERT INTO `comment` (`product_id`,`user_id`,`content`) VALUES
(@p_book, @qa_buyer_1, '请问教材还有配套习题答案吗？'),
(@p_book, @qa_seller_2, '同问，方便在本部校区交易吗？');

SET @user_a = LEAST(@qa_seller_1, @qa_buyer_1);
SET @user_b = GREATEST(@qa_seller_1, @qa_buyer_1);
INSERT INTO `conversation`
(`user_a_id`,`user_b_id`,`product_id`,`wanted_id`,`last_msg_at`) VALUES
(@user_a,@user_b,@p_keyboard,NULL,NOW()),
(@user_a,@user_b,NULL,@w_bike,DATE_SUB(NOW(), INTERVAL 1 HOUR));

SET @conv_product = (SELECT id FROM `conversation` WHERE user_a_id=@user_a AND user_b_id=@user_b AND product_id=@p_keyboard LIMIT 1);
SET @conv_wanted  = (SELECT id FROM `conversation` WHERE user_a_id=@user_a AND user_b_id=@user_b AND wanted_id=@w_bike LIMIT 1);
INSERT INTO `message` (`conversation_id`,`sender_id`,`content`,`is_read`,`created_at`) VALUES
(@conv_product,@qa_buyer_1,'你好，键盘还在吗？',1,DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(@conv_product,@qa_seller_1,'还在，可以今天下午东校区交易。',0,DATE_SUB(NOW(), INTERVAL 25 MINUTE)),
(@conv_wanted,@qa_seller_1,'我有一辆符合要求的自行车，可以先看看照片。',0,DATE_SUB(NOW(), INTERVAL 1 HOUR));

INSERT INTO `trade_order`
(`order_no`,`product_id`,`buyer_id`,`seller_id`,`status`,`meet_time`,`meet_location`,`remark`,`conversation_id`) VALUES
('QA202608230001',@p_keyboard,@qa_buyer_1,@qa_seller_1,'PENDING_CHAT',DATE_ADD(NOW(), INTERVAL 1 DAY),'东校区图书馆门口','验收数据：待沟通订单',@conv_product),
('QA202608230002',@p_book,@qa_buyer_1,@qa_seller_1,'PENDING_TRADE',DATE_ADD(NOW(), INTERVAL 2 DAY),'本部校区食堂门口','验收数据：待交易订单',NULL),
('QA202608230003',@p_lamp,@qa_buyer_1,@qa_seller_2,'COMPLETED',DATE_SUB(NOW(), INTERVAL 1 DAY),'西校区宿舍区','验收数据：已完成订单',NULL);

INSERT INTO `report`
(`reporter_id`,`reason_id`,`target_type`,`target_id`,`description`,`status`,`handler_id`,`handle_result`) VALUES
(@qa_buyer_1,1,'PRODUCT',@p_lamp,'验收数据：商品信息可能不准确，请管理员核查。','PENDING',NULL,NULL),
(@qa_seller_1,5,'USER',@qa_buyer_1,'验收数据：用于查看已驳回举报。','REJECTED',@qa_admin,'测试举报不成立');

SELECT '验收测试数据已就绪，所有 qa_ 账号密码均为 password' AS result;
SELECT username,nickname,role,auth_status,status FROM `user` WHERE username LIKE 'qa\_%' ORDER BY id;
SELECT status,COUNT(*) AS product_count FROM `product` WHERE seller_id IN (@qa_seller_1,@qa_seller_2) GROUP BY status;
SELECT status,COUNT(*) AS wanted_count FROM `wanted` WHERE buyer_id IN (@qa_buyer_1,@qa_seller_2) GROUP BY status;
