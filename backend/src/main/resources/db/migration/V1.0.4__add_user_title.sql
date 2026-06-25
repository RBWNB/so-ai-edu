-- ============================================================
-- 用户称号功能：app_user 加字段 user_title
-- ============================================================

ALTER TABLE app_user
    ADD COLUMN user_title VARCHAR(32) NOT NULL DEFAULT '' COMMENT '用户自定义称号' AFTER avatar_frame;

-- 2. 积分商店添加「自定义称号」商品（20000 积分）
INSERT INTO point_shop_item (name, description, item_type, points_price, stock, status, created_at)
VALUES ('✏️ 自定义称号', 'title_custom', 'virtual_item', 20000, NULL, 1, NOW());

-- 回滚：
--   ALTER TABLE app_user DROP COLUMN user_title;
--   DELETE FROM point_shop_item WHERE description = 'title_custom';
