CREATE DATABASE IF NOT EXISTS marine_db
    CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE marine_db;

SET FOREIGN_KEY_CHECKS = 0;

-- 1.用户表
CREATE TABLE app_user (
                          id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          username VARCHAR(64) NOT NULL UNIQUE,
                          password_hash VARCHAR(255) NOT NULL,
                          real_name VARCHAR(100),
                          email VARCHAR(128) UNIQUE,
                          phone VARCHAR(20) UNIQUE,
                          avatar_url VARCHAR(500),
                          avatar_frame VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '当前头像框编码',
                          user_title VARCHAR(32) NOT NULL DEFAULT '' COMMENT '用户自定义称号',
                          status TINYINT NOT NULL DEFAULT 1 COMMENT '1 enabled, 0 disabled',
                          last_login_time DATETIME,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2.角色表
CREATE TABLE app_role (
                          id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                          role_code VARCHAR(32) NOT NULL UNIQUE COMMENT 'ADMIN/MANAGER/VISITOR',
                          role_name VARCHAR(64) NOT NULL,
                          description VARCHAR(255),
                          status TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 3.用户角色关联表
CREATE TABLE app_user_role (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT UNSIGNED NOT NULL,
                               role_id BIGINT UNSIGNED NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_user_role (user_id, role_id),
                               CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                               CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES app_role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户角色关联';

-- 4.媒体资源表
CREATE TABLE media_asset (
                             id BIGINT UNSIGNED PRIMARY KEY,
                             provider VARCHAR(32) NOT NULL DEFAULT 'qiniu' COMMENT 'qiniu/local/minio',
                             bucket VARCHAR(128),
                             object_key VARCHAR(500) NOT NULL,
                             url VARCHAR(1000) NOT NULL,
                             mime_type VARCHAR(100),
                             file_size BIGINT UNSIGNED,
                             original_name VARCHAR(255),
                             created_by BIGINT UNSIGNED,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='媒体资源表';

-- 5.海洋物种百科表
CREATE TABLE marine_species (
                                id BIGINT UNSIGNED PRIMARY KEY,
                                chinese_name VARCHAR(128) NOT NULL,
                                scientific_name VARCHAR(191) NOT NULL UNIQUE,
                                alias_names VARCHAR(500),
                                kingdom VARCHAR(64),
                                phylum VARCHAR(64),
                                class_name VARCHAR(64),
                                order_name VARCHAR(64),
                                family_name VARCHAR(64),
                                genus_name VARCHAR(64),
                                species_name VARCHAR(64),
                                conservation_status VARCHAR(32),
                                habitat VARCHAR(255),
                                distribution_area VARCHAR(500),
                                morphology_desc TEXT,
                                habit_desc TEXT,
                                fun_fact TEXT COMMENT '趣味知识',
                                cover_media_id BIGINT UNSIGNED,
                                image_url VARCHAR(500) COMMENT '物种示意图URL（过渡方案，后续迁移到 media_asset）',
                                video_url VARCHAR(500) COMMENT '视频链接（过渡方案）',
                                data_source VARCHAR(255),
                                status TINYINT NOT NULL DEFAULT 1,
                                created_by BIGINT UNSIGNED,
                                updated_by BIGINT UNSIGNED,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                is_deleted TINYINT NOT NULL DEFAULT 0,
                                KEY idx_species_name (chinese_name),
                                KEY idx_species_taxonomy (phylum, class_name, order_name, family_name, genus_name),
                                KEY idx_species_status (conservation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='海洋物种百科';

-- 6.海洋生态系统表
CREATE TABLE marine_ecosystem (
                                  id BIGINT UNSIGNED PRIMARY KEY,
                                  name VARCHAR(128) NOT NULL,
                                  description TEXT,
                                  typical_species VARCHAR(500),
                                  threats VARCHAR(500),
                                  protection_advice TEXT,
                                  cover_media_id BIGINT UNSIGNED,
                                  image_url VARCHAR(500) DEFAULT NULL COMMENT '生态系统图片URL',
                                  status TINYINT NOT NULL DEFAULT 1,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  is_deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='海洋生态系统';

-- 7.知识分类表
CREATE TABLE kb_category (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             parent_id BIGINT UNSIGNED DEFAULT 0,
                             name VARCHAR(128) NOT NULL,
                             description VARCHAR(255),
                             sort_order INT DEFAULT 0,
                             status TINYINT NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识分类';

-- 8.知识库文档表
CREATE TABLE kb_document (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             category_id BIGINT UNSIGNED,
                             title VARCHAR(255) NOT NULL,
                             content LONGTEXT NOT NULL,
                             source VARCHAR(255),
                             source_type VARCHAR(32) DEFAULT 'manual' COMMENT 'manual/species/ecosystem/upload',
                             status TINYINT NOT NULL DEFAULT 1 COMMENT '0 draft, 1 published',
                             created_by BIGINT UNSIGNED,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             KEY idx_kb_category (category_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库文档';

-- 9.RAG 文档分块表
CREATE TABLE kb_document_chunk (
                                   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                   document_id BIGINT UNSIGNED NOT NULL,
                                   chunk_index INT NOT NULL,
                                   content TEXT NOT NULL,
                                   embedding_key VARCHAR(191) COMMENT 'Redis Stack 向量 key',
                                   token_count INT,
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE KEY uk_doc_chunk (document_id, chunk_index),
                                   CONSTRAINT fk_chunk_document FOREIGN KEY (document_id) REFERENCES kb_document(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG 文档分块';

-- 10.题库表
CREATE TABLE quiz_question (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               question_type VARCHAR(32) NOT NULL COMMENT 'single/multiple/judge',
                               stem TEXT NOT NULL,
                               options_json JSON,
                               answer_json JSON NOT NULL,
                               explanation TEXT,
                               difficulty VARCHAR(32) DEFAULT 'normal',
                               knowledge_points JSON,
                               species_id BIGINT UNSIGNED,
                               created_by_ai TINYINT NOT NULL DEFAULT 0,
                               status TINYINT NOT NULL DEFAULT 1,
                               created_by BIGINT UNSIGNED,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库';

-- 11.答题记录表
CREATE TABLE quiz_attempt (
                              id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                              user_id BIGINT UNSIGNED NOT NULL,
                              question_id BIGINT UNSIGNED NOT NULL,
                              user_answer_json JSON,
                              is_correct TINYINT NOT NULL,
                              time_spent_seconds INT,
                              attempted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              KEY idx_attempt_user (user_id),
                              KEY idx_attempt_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='答题记录';

-- 12.错题本表
CREATE TABLE quiz_wrong_bookmark (
                                     id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                     user_id BIGINT UNSIGNED NOT NULL,
                                     question_id BIGINT UNSIGNED NOT NULL,
                                     wrong_count INT NOT NULL DEFAULT 1,
                                     last_wrong_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     mastered TINYINT NOT NULL DEFAULT 0,
                                     UNIQUE KEY uk_wrong_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本';

-- 13.学习画像
CREATE TABLE user_learning_profile (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                       user_id BIGINT UNSIGNED NOT NULL UNIQUE,
                                       level INT NOT NULL DEFAULT 1,
                                       total_questions INT NOT NULL DEFAULT 0,
                                       correct_count INT NOT NULL DEFAULT 0,
                                       correct_rate DECIMAL(5,2) NOT NULL DEFAULT 0,
                                       weak_points JSON,
                                       preferred_categories JSON,
                                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习画像';

-- 14.积分流水
CREATE TABLE point_transaction (
                                   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                   user_id BIGINT UNSIGNED NOT NULL,
                                   points INT NOT NULL,
                                   biz_type VARCHAR(64) NOT NULL COMMENT 'quiz/task/shop/admin',
                                   biz_id BIGINT UNSIGNED,
                                   description VARCHAR(255),
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   KEY idx_point_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分流水';

-- 15.AI 对话历史
CREATE TABLE conversation_message (
                                      id BIGINT UNSIGNED PRIMARY KEY,
                                      user_id BIGINT UNSIGNED,
                                      session_id VARCHAR(64) NOT NULL,
                                      role VARCHAR(32) NOT NULL COMMENT 'user/assistant/system',
                                      content LONGTEXT NOT NULL,
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      KEY idx_conversation_session (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 对话历史';

-- 16.语音缓存
CREATE TABLE voice_audio_cache (
                                   id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                   text_hash VARCHAR(64) NOT NULL,
                                   source_text TEXT NOT NULL,
                                   voice_type VARCHAR(64),
                                   audio_media_id BIGINT UNSIGNED,
                                   tts_engine VARCHAR(64) DEFAULT 'chattts',
                                   created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   UNIQUE KEY uk_voice_hash_type (text_hash, voice_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='语音缓存';

-- 17.AI 调用日志
CREATE TABLE ai_call_log (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             user_id BIGINT UNSIGNED,
                             scene VARCHAR(64) NOT NULL COMMENT 'rag/chat/vision/quiz/tts',
                             provider VARCHAR(64),
                             model_name VARCHAR(128),
                             prompt_tokens INT,
                             completion_tokens INT,
                             latency_ms BIGINT,
                             status VARCHAR(32) DEFAULT 'SUCCESS',
                             error_message VARCHAR(500),
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             KEY idx_ai_scene_time (scene, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 调用日志';

-- 18.B端操作日志
CREATE TABLE operation_log (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               user_id BIGINT UNSIGNED,
                               username VARCHAR(64),
                               module VARCHAR(64),
                               description VARCHAR(255),
                               request_method VARCHAR(16),
                               request_url VARCHAR(500),
                               ip_address VARCHAR(50),
                               execution_time BIGINT,
                               status VARCHAR(32) DEFAULT 'SUCCESS',
                               error_message VARCHAR(500),
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='B端操作日志';

-- 19.用户积分账户表
CREATE TABLE user_point_account (
                                    id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                    user_id BIGINT UNSIGNED NOT NULL UNIQUE,
                                    available_points INT NOT NULL DEFAULT 0,
                                    total_earned_points INT NOT NULL DEFAULT 0,
                                    total_spent_points INT NOT NULL DEFAULT 0,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_point_account_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户积分账户';

-- 20.学习任务表
CREATE TABLE learning_task (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               title VARCHAR(128) NOT NULL,
                               description VARCHAR(500),
                               task_type VARCHAR(64) NOT NULL COMMENT 'daily_quiz/read_species/ask_ai/upload_observation',
                               target_value INT NOT NULL DEFAULT 1,
                               reward_points INT NOT NULL DEFAULT 0,
                               start_time DATETIME,
                               end_time DATETIME,
                               status TINYINT NOT NULL DEFAULT 1,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习任务';

-- 21.用户任务完成记录表
CREATE TABLE user_task_record (
                                  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                  user_id BIGINT UNSIGNED NOT NULL,
                                  task_id BIGINT UNSIGNED NOT NULL,
                                  task_date DATE NOT NULL COMMENT '任务所属日期，用于每日刷新',
                                  progress_value INT NOT NULL DEFAULT 0,
                                  completed TINYINT NOT NULL DEFAULT 0,
                                  completed_at DATETIME,
                                  reward_claimed TINYINT NOT NULL DEFAULT 0,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  UNIQUE KEY uk_user_task_date (user_id, task_id, task_date),
                                  CONSTRAINT fk_task_record_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                                  CONSTRAINT fk_task_record_task FOREIGN KEY (task_id) REFERENCES learning_task(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户任务完成记录';

-- 22.积分商店商品表
CREATE TABLE point_shop_item (
                                 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                 name VARCHAR(128) NOT NULL,
                                 description VARCHAR(500),
                                 item_type VARCHAR(64) NOT NULL COMMENT 'badge/avatar_frame/coupon/virtual_item',
                                 cover_media_id BIGINT UNSIGNED,
                                 points_price INT NOT NULL,
                                 stock INT DEFAULT NULL COMMENT 'NULL means unlimited',
                                 status TINYINT NOT NULL DEFAULT 1,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分商店商品';

-- 23.积分兑换记录表
CREATE TABLE point_exchange_order (
                                      id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                      user_id BIGINT UNSIGNED NOT NULL,
                                      item_id BIGINT UNSIGNED NOT NULL,
                                      points_cost INT NOT NULL,
                                      order_status VARCHAR(32) NOT NULL DEFAULT 'SUCCESS',
                                      created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                      KEY idx_exchange_user (user_id),
                                      CONSTRAINT fk_exchange_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                                      CONSTRAINT fk_exchange_item FOREIGN KEY (item_id) REFERENCES point_shop_item(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='积分兑换记录';

-- 24.用户收藏表
CREATE TABLE user_bookmark (
                               id BIGINT UNSIGNED PRIMARY KEY,
                               user_id BIGINT UNSIGNED NOT NULL,
                               target_type VARCHAR(64) NOT NULL COMMENT 'species/ecosystem/kb_document/quiz_question',
                               target_id BIGINT UNSIGNED NOT NULL,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_bookmark_target (user_id, target_type, target_id),
                               KEY idx_bookmark_user (user_id),
                               CONSTRAINT fk_bookmark_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户收藏';

-- 25.物种媒体图库表
CREATE TABLE species_media (
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                               species_id BIGINT UNSIGNED NOT NULL,
                               media_id BIGINT UNSIGNED NOT NULL,
                               media_role VARCHAR(64) DEFAULT 'gallery' COMMENT 'cover/gallery/video/audio',
                               sort_order INT DEFAULT 0,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               UNIQUE KEY uk_species_media (species_id, media_id),
                               CONSTRAINT fk_species_media_species FOREIGN KEY (species_id) REFERENCES marine_species(id),
                               CONSTRAINT fk_species_media_asset FOREIGN KEY (media_id) REFERENCES media_asset(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种媒体图库';

-- 26.物种地理分布点
CREATE TABLE species_distribution_point (
                                            id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                            species_id BIGINT UNSIGNED NOT NULL,
                                            latitude DECIMAL(10,7) NOT NULL,
                                            longitude DECIMAL(10,7) NOT NULL,
                                            location_name VARCHAR(255),
                                            sea_area VARCHAR(128),
                                            source VARCHAR(255),
                                            confidence DECIMAL(5,2),
                                            created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                            KEY idx_distribution_geo (latitude, longitude),
                                            KEY idx_distribution_species (species_id),
                                            CONSTRAINT fk_distribution_species FOREIGN KEY (species_id) REFERENCES marine_species(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种地理分布点';

-- 27.知识图谱节点
CREATE TABLE kg_node (
                         id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                         node_type VARCHAR(64) NOT NULL COMMENT 'species/ecosystem/concept/threat/protection',
                         ref_id BIGINT UNSIGNED DEFAULT NULL COMMENT '关联业务表ID，可为空',
                         name VARCHAR(128) NOT NULL,
                         description VARCHAR(500),
                         properties JSON,
                         created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         KEY idx_kg_node_type (node_type),
                         KEY idx_kg_node_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱节点';

-- 28.知识图谱关系
CREATE TABLE kg_relation (
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                             source_node_id BIGINT UNSIGNED NOT NULL,
                             target_node_id BIGINT UNSIGNED NOT NULL,
                             relation_type VARCHAR(64) NOT NULL COMMENT 'belongs_to/lives_in/eats/threatened_by/protected_by/related_to',
                             weight DECIMAL(6,3) DEFAULT 1.000,
                             description VARCHAR(500),
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             UNIQUE KEY uk_kg_relation (source_node_id, target_node_id, relation_type),
                             CONSTRAINT fk_kg_relation_source FOREIGN KEY (source_node_id) REFERENCES kg_node(id) ON DELETE CASCADE,
                             CONSTRAINT fk_kg_relation_target FOREIGN KEY (target_node_id) REFERENCES kg_node(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识图谱关系';

-- 29.用户观察分享表
CREATE TABLE user_observation (
                                  id BIGINT UNSIGNED PRIMARY KEY,
                                  user_id BIGINT UNSIGNED NOT NULL,
                                  species_id BIGINT UNSIGNED,
                                  title VARCHAR(128),
                                  description TEXT,
                                  latitude DECIMAL(10,7),
                                  longitude DECIMAL(10,7),
                                  location_name VARCHAR(255),
                                  observed_at DATETIME,
                                  photo_media_id BIGINT UNSIGNED,
                                  ai_identified TINYINT NOT NULL DEFAULT 0,
                                  ai_confidence DECIMAL(5,2),
                                  status TINYINT NOT NULL DEFAULT 1 COMMENT '0 hidden, 1 visible, 2 pending',
                                  audit_remark VARCHAR(500) DEFAULT NULL COMMENT '审核备注（下架原因等）',
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  KEY idx_observation_user (user_id),
                                  KEY idx_observation_species (species_id),
                                  CONSTRAINT fk_user_observation_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                                  CONSTRAINT fk_user_observation_species FOREIGN KEY (species_id) REFERENCES marine_species(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户观察分享';

-- 30.评论
CREATE TABLE content_comment (
                                 id BIGINT UNSIGNED PRIMARY KEY,
                                 user_id BIGINT UNSIGNED NOT NULL,
                                 target_type VARCHAR(64) NOT NULL COMMENT 'species/ecosystem/user_observation/kb_document',
                                 target_id BIGINT UNSIGNED NOT NULL,
                                 parent_id BIGINT UNSIGNED DEFAULT 0,
                                 content VARCHAR(1000) NOT NULL,
                                 status TINYINT NOT NULL DEFAULT 1,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 KEY idx_comment_target (target_type, target_id),
                                 CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

-- 31.点赞
CREATE TABLE content_like (
                              id BIGINT UNSIGNED PRIMARY KEY,
                              user_id BIGINT UNSIGNED NOT NULL,
                              target_type VARCHAR(64) NOT NULL COMMENT 'species/ecosystem/user_observation/comment',
                              target_id BIGINT UNSIGNED NOT NULL,
                              created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE KEY uk_like_target (user_id, target_type, target_id),
                              CONSTRAINT fk_like_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='点赞';

-- 32.用户徽章
CREATE TABLE user_badge (
                            id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                            user_id BIGINT UNSIGNED NOT NULL,
                            badge_code VARCHAR(64) NOT NULL,
                            badge_name VARCHAR(128) NOT NULL,
                            description VARCHAR(500),
                            earned_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            UNIQUE KEY uk_user_badge (user_id, badge_code),
                            CONSTRAINT fk_badge_user FOREIGN KEY (user_id) REFERENCES app_user(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户徽章';

-- 33.物种浏览记录
CREATE TABLE species_browse_record (
                                       id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                       user_id BIGINT UNSIGNED NOT NULL,
                                       species_id BIGINT UNSIGNED NOT NULL,
                                       browse_count INT NOT NULL DEFAULT 1 COMMENT '累计浏览次数',
                                       last_browsed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后浏览时间',
                                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '首次浏览时间',
                                       UNIQUE KEY uk_user_species (user_id, species_id),
                                       KEY idx_browse_user (user_id),
                                       CONSTRAINT fk_browse_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                                       CONSTRAINT fk_browse_species FOREIGN KEY (species_id) REFERENCES marine_species(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物种浏览记录';

-- 34.竞技模式答题记录
CREATE TABLE IF NOT EXISTS competition_record (
                                                  id              BIGINT UNSIGNED PRIMARY KEY,
                                                  user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                                  accuracy        DECIMAL(5,2)    NOT NULL COMMENT '正确率百分比',
                                                  total_questions INT             NOT NULL DEFAULT 10 COMMENT '总题数',
                                                  correct_count   INT             NOT NULL DEFAULT 0  COMMENT '答对题数',
                                                  score           INT             NOT NULL DEFAULT 0  COMMENT '本场积分（答对1题2分 + 全对额外20分）',
                                                  total_time_ms   BIGINT          NOT NULL DEFAULT 0  COMMENT '总耗时(毫秒)',
                                                  avg_time_ms     BIGINT          NOT NULL DEFAULT 0  COMMENT '平均每题耗时(毫秒)',
                                                  tier            VARCHAR(16)     NOT NULL DEFAULT '青铜' COMMENT '段位: 王者/钻石/黄金/白银/青铜',
                                                  rank_score      INT             NOT NULL DEFAULT 0  COMMENT '排名综合分(0-100)',
                                                  created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',

                                                  INDEX idx_user    (user_id),
                                                  INDEX idx_accuracy (accuracy),
                                                  INDEX idx_created  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='竞技模式答题记录';

-- 35.用户消息通知表
CREATE TABLE system_notification (
                                     id BIGINT UNSIGNED PRIMARY KEY,
                                     receiver_id BIGINT UNSIGNED NOT NULL COMMENT '接收通知的用户ID',
                                     sender_id BIGINT UNSIGNED NOT NULL COMMENT '触发通知的用户ID',
                                     type VARCHAR(32) NOT NULL COMMENT '通知类型: like_post, like_comment, reply_post, reply_comment',
                                     target_id BIGINT UNSIGNED NOT NULL COMMENT '点赞或评论的ID，用于溯源',
                                     post_id BIGINT UNSIGNED NOT NULL COMMENT '用于前端直接跳转的顶级帖子(观察记录)ID',
                                     content VARCHAR(255) COMMENT '简略内容，如评论的前50个字，点赞可为空',
                                     is_read TINYINT NOT NULL DEFAULT 0 COMMENT '0:未读, 1:已读',
                                     created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     KEY idx_receiver_unread (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户消息通知';
SET FOREIGN_KEY_CHECKS = 1;


-- ══════════════════════════════════════════════════════════════
-- 数据库维护脚本（集成自 database_maintenance.sql）
-- ══════════════════════════════════════════════════════════════
-- 用途：外键优化、孤儿数据清理、数据完整性检查
-- 执行时机：数据迁移后或需要维护数据一致性时手动执行
-- ══════════════════════════════════════════════════════════════


-- ═══ 1.1 查看当前外键状态 ═══

SELECT '=== 当前外键约束状态 ===' AS info;
SELECT
    rc.CONSTRAINT_NAME,
    rc.TABLE_NAME,
    rc.UNIQUE_CONSTRAINT_NAME,
    rc.DELETE_RULE,
    rc.UPDATE_RULE,
    kcu.COLUMN_NAME,
    kcu.REFERENCED_COLUMN_NAME
FROM INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
    ON  rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
    AND rc.CONSTRAINT_NAME   = kcu.CONSTRAINT_NAME
    AND rc.TABLE_NAME        = kcu.TABLE_NAME
WHERE rc.REFERENCED_TABLE_NAME = 'marine_species'
  AND rc.CONSTRAINT_SCHEMA = DATABASE()
ORDER BY rc.TABLE_NAME, rc.CONSTRAINT_NAME;


-- ═══ 1.2 将外键改为 CASCADE（级联删除）═══
-- 说明：ON DELETE CASCADE 表示当删除物种时，MySQL会自动删除所有关联数据
-- 好处：无需代码干预，数据库层面保证数据一致性

SELECT '\n=== 开始优化外键约束 ===' AS info;

-- 1.2.1 species_browse_record 表
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE table_schema = DATABASE() AND table_name = 'species_browse_record'
                  AND constraint_name = 'fk_browse_species' AND constraint_type = 'FOREIGN KEY');

SET @sql = IF(@fk_exists > 0,
    'ALTER TABLE species_browse_record DROP FOREIGN KEY fk_browse_species, ADD CONSTRAINT fk_browse_species FOREIGN KEY (species_id) REFERENCES marine_species(id) ON DELETE CASCADE ON UPDATE CASCADE',
    'SELECT "⚠️ fk_browse_species 外键不存在，跳过" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT '✅ species_browse_record: 已设置为级联删除' AS result;


-- 1.2.2 user_observation 表
SET @fk_exists = (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS
                  WHERE table_schema = DATABASE() AND table_name = 'user_observation'
                  AND constraint_name = 'fk_user_observation_species' AND constraint_type = 'FOREIGN KEY');

SET @sql = IF(@fk_exists > 0,
    'ALTER TABLE user_observation DROP FOREIGN KEY fk_user_observation_species, ADD CONSTRAINT fk_user_observation_species FOREIGN KEY (species_id) REFERENCES marine_species(id) ON DELETE CASCADE ON UPDATE CASCADE',
    'SELECT "⚠️ fk_user_observation_species 外键不存在，跳过" AS result'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

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
