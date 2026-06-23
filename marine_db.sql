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
                             id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
                                id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
                                  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
                                  name VARCHAR(128) NOT NULL,
                                  description TEXT,
                                  typical_species VARCHAR(500),
                                  threats VARCHAR(500),
                                  protection_advice TEXT,
                                  cover_media_id BIGINT UNSIGNED,
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
                                      id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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

SET FOREIGN_KEY_CHECKS = 0;

--  19.用户积分账户表
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
-- task_date: 任务所属日期，按天重置进度。唯一索引 (user_id, task_id, task_date)
--             保证每个用户每天每个任务只有一条记录
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
                               id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
                                  id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  KEY idx_observation_user (user_id),
                                  KEY idx_observation_species (species_id),
                                  CONSTRAINT fk_user_observation_user FOREIGN KEY (user_id) REFERENCES app_user(id),
                                  CONSTRAINT fk_user_observation_species FOREIGN KEY (species_id) REFERENCES marine_species(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户观察分享';

-- 30.评论
CREATE TABLE content_comment (
                                 id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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
                              id BIGINT UNSIGNED PRIMARY KEY AUTO_INCREMENT,
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


SET FOREIGN_KEY_CHECKS = 1;
