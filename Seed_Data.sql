USE marine_db;

-- 关闭外键检查，方便按顺序插入
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================
-- 1. 初始化账号体系
-- ==============================================
# 密码123123
INSERT INTO app_user (id, username, password_hash, real_name, status) VALUES
                                                                          (1, 'admin', '$2a$10$YMKGyYJCjqk2nEjzXZ.81euWN2U/pt4vXAKQ3vKsqb43l4FWa8sgK', '超级管理员', 1),
                                                                          (2, 'student_test', '$2a$10$YMKGyYJCjqk2nEjzXZ.81euWN2U/pt4vXAKQ3vKsqb43l4FWa8sgK', '测试学员', 1);

INSERT INTO app_user_role (user_id, role_id) VALUES
                                                 (1, 1), -- admin -> ADMIN
                                                 (2, 3); -- student_test -> VISITOR

-- ==============================================
-- 2. 初始化媒体资源
-- ==============================================
INSERT INTO media_asset (id, provider, object_key, url, mime_type, original_name) VALUES
                                                                                      (1, 'qiniu', 'species/dolphin_cover.jpg', 'https://example.com/dolphin.jpg', 'image/jpeg', 'dolphin.jpg'),
                                                                                      (2, 'qiniu', 'ecosystem/mangrove_cover.jpg', 'https://example.com/mangrove.jpg', 'image/jpeg', 'mangrove.jpg');

-- ==============================================
-- 3. 初始化物种与生态系统 (带有深圳海域特色)
-- ==============================================
INSERT INTO marine_species (id, chinese_name, scientific_name, kingdom, phylum, class_name, family_name, conservation_status, fun_fact, cover_media_id) VALUES
    (1, '中华白海豚', 'Sousa chinensis', '动物界', '脊索动物门', '哺乳纲', '海豚科', 'VU', '中华白海豚刚出生时是深灰色的，长大后才会变成粉白色哦！', 1);

INSERT INTO marine_ecosystem (id, name, description, typical_species, cover_media_id) VALUES
    (1, '红树林生态系统', '生长在热带、亚热带海岸潮间带的木本植物群落，被称为“海岸卫士”。', '弹涂鱼、招潮蟹、黑脸琵鹭', 2);

INSERT INTO species_distribution_point (species_id, latitude, longitude, location_name, sea_area) VALUES
    (1, 22.4833, 113.9167, '深圳湾大桥海域', '珠江口');

-- ==============================================
-- 4. 初始化 RAG 知识库
-- ==============================================
INSERT INTO kb_category (id, name, description) VALUES
    (1, '海洋哺乳动物', '关于海豚、鲸鱼等海洋哺乳动物的科普知识');

INSERT INTO kb_document (id, category_id, title, content, source_type) VALUES
    (1, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual');

INSERT INTO kb_document_chunk (document_id, chunk_index, content, embedding_key, token_count) VALUES
                                                                                                  (1, 0, '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。', 'doc:chunk:1_0', 25),
                                                                                                  (1, 1, '它们主要以河口水域的小型鱼类为食，具有高度的社会性。', 'doc:chunk:1_1', 30);

-- ==============================================
-- 5. 初始化题库与学习数据
-- ==============================================
INSERT INTO quiz_question (id, question_type, stem, options_json, answer_json, explanation, species_id) VALUES
    (1, 'single', '中华白海豚刚出生时是什么颜色的？', '["A. 粉白色", "B. 深灰色", "C. 纯黑色", "D. 斑点色"]', '["B"]', '中华白海豚体色会随年龄发生变化，幼体为深灰色，成体因皮下血管丰富而呈现粉白色。', 1);

-- 给学生账户初始化积分账户与画像
INSERT INTO user_point_account (user_id, available_points) VALUES (2, 100);

INSERT INTO user_learning_profile (user_id, level, total_questions, correct_count, correct_rate) VALUES
    (2, 1, 10, 8, 80.00);

-- ==============================================
-- 6. 初始化知识图谱 (节点与关系)
-- ==============================================
INSERT INTO kg_node (id, node_type, ref_id, name) VALUES
                                                      (1, 'species', 1, '中华白海豚'),
                                                      (2, 'concept', NULL, '哺乳纲'),
                                                      (3, 'threat', NULL, '海洋塑料污染');

INSERT INTO kg_relation (source_node_id, target_node_id, relation_type) VALUES
                                                                            (1, 2, 'belongs_to'),
                                                                            (1, 3, 'threatened_by');

-- ==============================================
-- 7. 初始化 C端社区数据
-- ==============================================
INSERT INTO user_observation (id, user_id, species_id, title, description, location_name, ai_identified, ai_confidence) VALUES
    (1, 2, 1, '深圳湾偶遇粉红小精灵！', '今天在深圳湾绿道骑行，幸运地拍到了跃出水面的白海豚！', '深圳湾公园', 1, 98.50);


-- ==============================================
-- 8.初始化 用户角色
-- ==============================================
INSERT INTO app_role (id, role_code, role_name, description) VALUES
                                                                 (1, 'ADMIN', '系统管理员', '拥有全部管理权限'),
                                                                 (2, 'MANAGER', '内容管理员', '管理百科、知识库、题库和AI内容'),
                                                                 (3, 'VISITOR', '普通用户', '使用C端学习与互动功能');

SET FOREIGN_KEY_CHECKS = 1;
