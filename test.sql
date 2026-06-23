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
