-- 观察社区审核功能：增加审核备注字段
-- 使用存储过程实现幂等性，避免重复执行报错
DELIMITER $$
DROP PROCEDURE IF EXISTS add_column_if_not_exists $$
CREATE PROCEDURE add_column_if_not_exists()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'user_observation'
          AND COLUMN_NAME = 'audit_remark'
    ) THEN
        ALTER TABLE user_observation
            ADD COLUMN audit_remark VARCHAR(500) DEFAULT NULL
            COMMENT '审核备注（下架原因等）' AFTER status;
    END IF;
END $$
DELIMITER ;
CALL add_column_if_not_exists();
DROP PROCEDURE add_column_if_not_exists;
