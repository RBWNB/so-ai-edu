-- ============================================================
-- 每日任务刷新 — 完整迁移脚本（全环境通用，幂等安全）
-- 给 user_task_record 添加 task_date 字段
-- 唯一约束从 (user_id, task_id) 改为 (user_id, task_id, task_date)
-- 使得每天每个任务有一条独立记录，实现自然"每日刷新"
-- ============================================================
-- 使用方法：直接在数据库客户端或命令行一次性执行即可
-- 适用版本：MySQL 5.7+ / MariaDB 10.2+
-- ============================================================

-- 用以处理已有零日期的兼容模式
SET @OLD_SQL_MODE = @@SQL_MODE;
SET SESSION SQL_MODE = '';

-- ============ 第 1 步：添加 task_date 列（幂等） ============
SET @col_exists = (SELECT COUNT(*) FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE()
                     AND TABLE_NAME = 'user_task_record'
                     AND COLUMN_NAME = 'task_date');

SET @add_col_sql = IF(@col_exists = 0,
                      'ALTER TABLE user_task_record ADD COLUMN task_date DATE COMMENT ''任务所属日期'' AFTER task_id',
                      'SELECT 1');
PREPARE stmt FROM @add_col_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============ 第 2 步：填充 task_date 数据（幂等） ============
-- 优先用完成日期，回退到创建日期，最后兜底用今天
UPDATE user_task_record
SET task_date = COALESCE(
        NULLIF(DATE(completed_at), '0000-00-00'),
        NULLIF(DATE(created_at), '0000-00-00'),
        CURDATE()
                )
WHERE task_date IS NULL OR task_date = '0000-00-00';

-- ============ 第 3 步：加上 NOT NULL 约束（幂等） ============
SET @nullable = (SELECT IS_NULLABLE FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE()
                   AND TABLE_NAME = 'user_task_record'
                   AND COLUMN_NAME = 'task_date');

SET @modify_sql = IF(@nullable = 'YES',
                     'ALTER TABLE user_task_record MODIFY COLUMN task_date DATE NOT NULL COMMENT ''任务所属日期''',
                     'SELECT 1');
PREPARE stmt FROM @modify_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============ 第 4 步：先建新唯一索引（幂等，解决外键依赖） ============
SET @new_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE()
                         AND TABLE_NAME = 'user_task_record'
                         AND INDEX_NAME = 'uk_user_task_date');

SET @add_idx_sql = IF(@new_idx_exists = 0,
                      'ALTER TABLE user_task_record ADD UNIQUE KEY uk_user_task_date (user_id, task_id, task_date)',
                      'SELECT 1');
PREPARE stmt FROM @add_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============ 第 5 步：再删旧唯一索引（幂等） ============
SET @old_idx_exists = (SELECT COUNT(*) FROM information_schema.STATISTICS
                       WHERE TABLE_SCHEMA = DATABASE()
                         AND TABLE_NAME = 'user_task_record'
                         AND INDEX_NAME = 'uk_user_task');

SET @drop_idx_sql = IF(@old_idx_exists > 0,
                       'ALTER TABLE user_task_record DROP INDEX uk_user_task',
                       'SELECT 1');
PREPARE stmt FROM @drop_idx_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- ============ 第 6 步：恢复严格模式 ============
SET SESSION SQL_MODE = @OLD_SQL_MODE;

-- ============ 完成 ============
SELECT '✅ 每日任务刷新迁移完成' AS status;

-- ============================================================
-- 回滚脚本（如需恢复到旧结构）：
--   SET @OLD_SQL_MODE = @@SQL_MODE; SET SESSION SQL_MODE = '';
--   ALTER TABLE user_task_record DROP INDEX uk_user_task_date;
--   ALTER TABLE user_task_record ADD UNIQUE KEY uk_user_task (user_id, task_id);
--   ALTER TABLE user_task_record DROP COLUMN task_date;
--   SET SESSION SQL_MODE = @OLD_SQL_MODE;
-- ============================================================
-- 清空旧商品（如果有外键关联兑换记录，先删关联）
DELETE FROM point_exchange_order;
DELETE FROM point_shop_item;

-- 插入新商品
INSERT INTO point_shop_item (id, name, description, item_type, points_price, stock, status) VALUES
                                                                                                (1, '💎 积分盲盒', '随机获得 50%~200% 的积分返还，稳赚不赔', 'virtual_item', 100, 50, 1),
                                                                                                (2, '🎫 任务快进卡', '一键完成一个进行中的每日任务，直接领取奖励', 'coupon', 200, NULL, 1),
                                                                                                (3, '🏅 隐藏称号·蔚蓝守护者', '永久获得稀有勋章「蔚蓝守护者」，不在常规成就列表中', 'badge', 500, 1, 1),
                                                                                                (4, '📊 深度学习报告', '基于你的答题数据生成一份专属学习分析报告，包含正确率趋势与薄弱知识点', 'virtual_item', 300, 30, 1),
                                                                                                (5, '⚡ 双倍经验卡', '当日答题正确时升级经验翻倍（×2），第二天自动失效', 'coupon', 150, NULL, 1),
                                                                                                (6, '🏅 隐藏称号·深渊征服者', '永久获得稀有勋章「深渊征服者」，深海探索者的终极荣誉', 'badge', 800, 1, 1),
                                                                                                (7, '专属头像框·海洋之蓝', 'frame_code:ocean', 'avatar_frame', 500, 20, 1),
                                                                                                (8, '黄金边框', 'frame_code:gold', 'avatar_frame', 200, NULL, 1),
                                                                                                (9, '彩虹边框', 'frame_code:rainbow', 'avatar_frame', 500, NULL, 1),
                                                                                                (10, '火焰边框', 'frame_code:flame', 'avatar_frame', 400, NULL, 1);
ALTER TABLE app_user
    ADD COLUMN avatar_frame VARCHAR(255) DEFAULT NULL COMMENT '用户头像框资源地址/标识';

-- ============================================================
-- 物种浏览记录表 — 幂等建表脚本
-- 用于支持「生态卫士」勋章（浏览50个物种）和每日任务「浏览物种」
-- ============================================================
SET @table_exists = (SELECT COUNT(*) FROM information_schema.TABLES
                     WHERE TABLE_SCHEMA = DATABASE()
                       AND TABLE_NAME = 'species_browse_record');

SET @create_sql = IF(@table_exists = 0,
    'CREATE TABLE species_browse_record (
        id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
        user_id BIGINT UNSIGNED NOT NULL,
        species_id BIGINT UNSIGNED NOT NULL,
        browse_count INT NOT NULL DEFAULT 1 COMMENT ''累计浏览次数'',
        last_browsed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''最后浏览时间'',
        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT ''首次浏览时间'',
        UNIQUE KEY uk_user_species (user_id, species_id),
        KEY idx_browse_user (user_id),
        CONSTRAINT fk_browse_user FOREIGN KEY (user_id) REFERENCES app_user(id),
        CONSTRAINT fk_browse_species FOREIGN KEY (species_id) REFERENCES marine_species(id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''物种浏览记录''',
    'SELECT 1');
PREPARE stmt FROM @create_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✅ 物种浏览记录表迁移完成' AS status;

ALTER TABLE marine_ecosystem ADD COLUMN image_url VARCHAR(500) DEFAULT NULL COMMENT '生态系统图片URL' AFTER cover_media_id;

-- 修复生态系统数据的 status 字段
-- 将所有 status 为 NULL 的生态系统数据设置为 1（正常状态）
UPDATE marine_ecosystem SET status = 1 WHERE status IS NULL;

-- 验证更新结果
SELECT id, name, status FROM marine_ecosystem;
