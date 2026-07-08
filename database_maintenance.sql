

-- ═══ 1.1 查看当前外键状态 ═══

SELECT '=== 当前外键约束状态 ===' AS info;
SELECT 
    TABLE_NAME,
    CONSTRAINT_NAME,
    COLUMN_NAME,
    REFERENCED_TABLE_NAME,
    DELETE_RULE,
    UPDATE_RULE
FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE REFERENCED_TABLE_NAME = 'marine_species'
AND TABLE_SCHEMA = DATABASE()
ORDER BY TABLE_NAME, CONSTRAINT_NAME;


-- ═══ 1.2 将外键改为 CASCADE（级联删除）═══
-- 说明：ON DELETE CASCADE 表示当删除物种时，MySQL会自动删除所有关联数据
-- 好处：无需代码干预，数据库层面保证数据一致性

SELECT '\n=== 开始优化外键约束 ===' AS info;

-- 1.2.1 species_browse_record 表
ALTER TABLE species_browse_record 
    DROP FOREIGN KEY IF EXISTS fk_browse_species,
    ADD CONSTRAINT fk_browse_species 
        FOREIGN KEY (species_id) REFERENCES marine_species(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE;

SELECT '✅ species_browse_record: 已设置为级联删除' AS result;


-- 1.2.2 user_observation 表
ALTER TABLE user_observation 
    DROP FOREIGN KEY IF EXISTS fk_user_observation_species,
    ADD CONSTRAINT fk_user_observation_species 
        FOREIGN KEY (species_id) REFERENCES marine_species(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE;

SELECT '✅ user_observation: 已设置为级联删除' AS result;


-- 1.2.3 species_distribution_point 表（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables 
                     WHERE table_schema = DATABASE() AND table_name = 'species_distribution_point');

SET @sql = IF(@table_exists > 0,
    'ALTER TABLE species_distribution_point DROP FOREIGN KEY IF EXISTS fk_distribution_species, ADD CONSTRAINT fk_distribution_species FOREIGN KEY (species_id) REFERENCES marine_species(id) ON DELETE CASCADE ON UPDATE CASCADE',
    'SELECT "⏭️ species_distribution_point 表不存在，跳过" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- 1.2.4 species_media 表（如果存在）
SET @table_exists = (SELECT COUNT(*) FROM information_schema.tables 
                     WHERE table_schema = DATABASE() AND table_name = 'species_media');

SET @sql = IF(@table_exists > 0,
    'ALTER TABLE species_media DROP FOREIGN KEY IF EXISTS fk_species_media_species, ADD CONSTRAINT fk_species_media_species FOREIGN KEY (species_id) REFERENCES marine_species(id) ON DELETE CASCADE ON UPDATE CASCADE',
    'SELECT "⏭️ species_media 表不存在，跳过" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ═══ 1.3 移除 kb_document 表的 species_id 字段 ═══
-- 说明：kb_document 不再通过 species_id 关联物种
-- 物种详情通过 quiz_question.species_id → marine_species.id 获取

SELECT '\n=== 开始移除 kb_document.species_id 字段 ===' AS info;

-- 1.3.1 检查字段是否存在
SET @column_exists = (SELECT COUNT(*) FROM information_schema.columns 
                      WHERE table_schema = DATABASE() 
                      AND table_name = 'kb_document' 
                      AND column_name = 'species_id');

-- 1.3.2 如果存在则删除索引和字段
SET @sql = IF(@column_exists > 0,
    CONCAT(
        'ALTER TABLE kb_document DROP INDEX IF EXISTS idx_kb_species; ',
        'ALTER TABLE kb_document DROP COLUMN species_id; ',
        'SELECT "✅ kb_document.species_id 字段已移除" AS result;'
    ),
    'SELECT "ℹ️ kb_document.species_id 字段不存在，跳过" AS result;'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;


-- ═══ 1.4 验证结构优化结果 ═══

SELECT '\n=== 结构优化完成，验证结果 ===' AS info;

-- 验证外键是否已改为CASCADE
SELECT 
    TABLE_NAME,
    CASE DELETE_RULE
        WHEN 'CASCADE' THEN '✅ 级联删除'
        WHEN 'RESTRICT' THEN '❌ 阻止删除'
        WHEN 'NO ACTION' THEN '⚠️ 无操作'
        ELSE DELETE_RULE
    END AS delete_status
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
WHERE TABLE_SCHEMA = DATABASE()
AND CONSTRAINT_TYPE = 'FOREIGN KEY'
AND REFERENCED_TABLE_NAME = 'marine_species'
ORDER BY TABLE_NAME;

-- 验证字段是否已删除
SELECT 
    CASE 
        WHEN COUNT(*) = 0 THEN '✅ kb_document.species_id 已移除'
        ELSE '❌ kb_document.species_id 仍存在'
    END AS field_check
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
AND TABLE_NAME = 'kb_document'
AND COLUMN_NAME = 'species_id';


-- ════════════════════════════════════════════════════════
-- 第二部分：清理历史孤儿数据
-- ════════════════════════════════════════════════════════

SELECT '\n\n=== 开始清理孤儿数据 ===' AS info;

-- ═══ 2.1 统计当前孤儿数据数量 ═══

SELECT '--- 孤儿数据统计 ---' AS section;

-- 2.1.1 题目表中的无效species_id
SELECT 
    'quiz_question' AS table_name,
    COUNT(*) AS orphan_count,
    '引用了不存在的species_id' AS description
FROM quiz_question q
WHERE q.species_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM marine_species s WHERE s.id = q.species_id);


-- ═══ 2.2 执行清理操作 ═══

SELECT '\n--- 执行清理操作 ---' AS section;

-- 2.2.1 清理题目表的孤儿数据（设置species_id为NULL，保留题目）
UPDATE quiz_question SET species_id = NULL
WHERE species_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM marine_species s WHERE s.id = quiz_question.species_id);

SELECT CONCAT('✅ 清理了 ', ROW_COUNT(), ' 条题目的无效species_id') AS result;


-- ═══ 2.3 验证清理结果 ═══

SELECT '\n--- 清理结果验证 ---' AS section;

-- 2.3.1 确认题目表无孤儿数据
SELECT 
    CASE
        WHEN COUNT(*) = 0 THEN '✅ 题目表无孤儿数据'
        ELSE CONCAT('❌ 题目表仍有 ', COUNT(*),' 条孤儿数据')
    END AS quiz_check
FROM quiz_question q
WHERE q.species_id IS NOT NULL 
AND NOT EXISTS (SELECT 1 FROM marine_species s WHERE s.id = q.species_id);


-- ════════════════════════════════════════════════════════
-- 第三部分：数据完整性检查与统计
-- ════════════════════════════════════════════════════════

SELECT '\n\n=== 数据完整性报告 ===' AS info;

-- ═══ 3.1 各表记录数统计 ═══

SELECT '--- 数据量统计 ---' AS section;

SELECT 
    'marine_species' AS table_name, COUNT(*) AS total_count, '物种主表' AS description
FROM marine_species
UNION ALL
SELECT 'quiz_question', COUNT(*), '题目表（含物种关联）'
FROM quiz_question
UNION ALL
SELECT 'kb_document', COUNT(*), '知识库文档表（无物种关联）'
FROM kb_document
UNION ALL
SELECT 'user_bookmark', COUNT(*), '收藏记录表'
FROM user_bookmark
UNION ALL
SELECT 'user_observation', COUNT(*), '用户观察记录表'
FROM user_observation
UNION ALL
SELECT 'species_browse_record', COUNT(*), '浏览记录表'
FROM species_browse_record;


-- ═══ 3.2 每个物种的关联数据统计 ═══

SELECT '\n--- 物种关联数据明细 ---' AS section;

SELECT
    s.id AS species_id,
    s.chinese_name AS species_name,
    (SELECT COUNT(*) FROM quiz_question q WHERE q.species_id = s.id) AS question_count,
    (SELECT COUNT(*) FROM species_browse_record b WHERE b.species_id = s.id) AS browse_count,
    (SELECT COUNT(*) FROM user_observation o WHERE o.species_id = s.id) AS observation_count,
    (SELECT COUNT(*) FROM user_bookmark bk WHERE bk.target_type = 'species' AND bk.target_id = s.id) AS bookmark_count,
    (SELECT COUNT(*) FROM species_distribution_point dp WHERE dp.species_id = s.id) AS distribution_count,
    (SELECT COUNT(*) FROM species_media sm WHERE sm.species_id = s.id) AS media_count
FROM marine_species s
ORDER BY s.id;


-- ═══ 3.3 收藏数据按类型分组统计 ═══

SELECT '\n--- 收藏数据分布 ---' AS section;

SELECT 
    target_type,
    COUNT(*) AS bookmark_count,
    CASE target_type
        WHEN 'quiz_question' THEN '📝 题目收藏'
        WHEN 'species' THEN '🐋 物种收藏'
        WHEN 'kb_document' THEN '📚 RAG文档收藏'
        WHEN 'ecosystem' THEN '🌊 生态系统收藏'
        WHEN 'user_observation' THEN '👁️ 观察帖子收藏'
        ELSE CONCAT('❓ 其他类型:', target_type)
    END AS type_description
FROM user_bookmark
GROUP BY target_type
ORDER BY bookmark_count DESC;


-- ═══ 3.4 题目的来源类型统计 ═══

SELECT '\n--- 题目来源分析 ---' AS section;

SELECT 
    CASE 
        WHEN species_id IS NOT NULL AND source_document_id IS NOT NULL THEN '🔄 双重来源（异常）'
        WHEN species_id IS NOT NULL THEN '🐋 物种出题'
        WHEN source_document_id IS NOT NULL THEN '📚 RAG出题'
        ELSE '❓ 未指定来源'
    END AS source_type,
    COUNT(*) AS question_count,
    ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM quiz_question), 2) AS percentage
FROM quiz_question
GROUP BY 
    CASE 
        WHEN species_id IS NOT NULL AND source_document_id IS NOT NULL THEN '🔄 双重来源（异常）'
        WHEN species_id IS NOT NULL THEN '🐋 物种出题'
        WHEN source_document_id IS NOT NULL THEN '📚 RAG出题'
        ELSE '❓ 未指定来源'
    END
ORDER BY question_count DESC;