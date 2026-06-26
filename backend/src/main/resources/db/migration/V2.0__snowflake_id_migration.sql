-- ===================================================
-- V2.0: 雪花算法 ID 迁移
-- 描述: 对暴露在前端 URL / API 响应中的表，
--       从 AUTO_INCREMENT 改为雪花算法（应用层生成 ID）
-- 日期: 2026-06-26
-- ===================================================

-- 去掉 AUTO_INCREMENT，保留 BIGINT UNSIGNED 类型
ALTER TABLE user_observation     MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE marine_species       MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE marine_ecosystem     MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE content_comment      MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE content_like         MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE user_bookmark        MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE media_asset          MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE conversation_message MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE competition_record   MODIFY id BIGINT UNSIGNED NOT NULL;
ALTER TABLE system_notification  MODIFY id BIGINT UNSIGNED NOT NULL;
