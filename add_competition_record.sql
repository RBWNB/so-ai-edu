-- ============================================================
-- 海洋学堂 - 竞技模式记录表
-- 直接在你的 MySQL 数据库中执行此文件即可
-- ============================================================

CREATE TABLE IF NOT EXISTS competition_record (
    id              BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    accuracy        DECIMAL(5,2)    NOT NULL COMMENT '正确率百分比',
    total_questions INT             NOT NULL DEFAULT 10 COMMENT '总题数',
    correct_count   INT             NOT NULL DEFAULT 0  COMMENT '答对题数',
    total_time_ms   BIGINT          NOT NULL DEFAULT 0  COMMENT '总耗时(毫秒)',
    avg_time_ms     BIGINT          NOT NULL DEFAULT 0  COMMENT '平均每题耗时(毫秒)',
    tier            VARCHAR(16)     NOT NULL DEFAULT '青铜' COMMENT '段位: 王者/钻石/黄金/白银/青铜',
    rank_score      INT             NOT NULL DEFAULT 0  COMMENT '排名综合分(0-100)',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

    INDEX idx_user    (user_id),
    INDEX idx_accuracy (accuracy),
    INDEX idx_created  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞技模式答题记录';
