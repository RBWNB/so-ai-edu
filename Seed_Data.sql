USE marine_db;

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

INSERT INTO marine_ecosystem (id, name, description, typical_species, cover_media_id, image_url, status) VALUES
    (1, '红树林生态系统', '生长在热带、亚热带海岸潮间带的木本植物群落，被称为"海岸卫士"。', '弹涂鱼、招潮蟹、黑脸琵鹭', 2, NULL, 1);

INSERT INTO species_distribution_point (species_id, latitude, longitude, location_name, sea_area) VALUES
    (1, 22.4833, 113.9167, '深圳湾大桥海域', '珠江口');

-- ==============================================
-- 4. 初始化 RAG 知识库
-- ==============================================
INSERT INTO kb_category (id, name, description) VALUES
    (1, '海洋哺乳动物', '关于海豚、鲸鱼等海洋哺乳动物的科普知识');

INSERT INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
    (1, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual', 1);

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
-- 8. 初始化每日任务
-- ==============================================
INSERT INTO learning_task (id, title, description, task_type, target_value, reward_points, status) VALUES
                                                                                                       (1, '每日签到', '登录即可领取', 'daily_checkin', 1, 5, 1),
                                                                                                       (2, '答对3道题目', '在答题闯关中答对3题', 'daily_quiz', 3, 15, 1),
                                                                                                       (3, '完成一次AI问答', '向AI导师提问并获得回答', 'ask_ai', 1, 10, 1),
                                                                                                       (4, '浏览5个物种详情', '在海洋百科中查看物种详情页', 'read_species', 5, 20, 1),
                                                                                                       (5, '发布1条观察记录', '在社区发布你的自然观察', 'upload_observation', 1, 15, 1),
                                                                                                       (6, '收藏3个物种或知识', '将感兴趣的物种或知识加入收藏', 'bookmark', 3, 10, 1);

-- ==============================================
-- 9. 初始化用户徽章
-- ==============================================
INSERT INTO user_badge (user_id, badge_code, badge_name, description, earned_at) VALUES
                                                                                     (2, 'first_quiz', '初识海洋', '完成首次答题', '2026-05-01 10:30:00'),
                                                                                     (2, 'quiz_master', '答题达人', '累计答对100题', '2026-05-15 14:20:00'),
                                                                                     (2, 'eco_guardian', '生态卫士', '浏览50个物种详情', '2026-06-01 09:15:00'),
                                                                                     (2, 'perfect_streak', '十连答对', '连续答对10题', '2026-06-10 16:45:00'),
                                                                                     (2, 'persistence', '持之以恒', '连续签到7天', '2026-06-15 08:00:00'),
                                                                                     (2, 'ai_explorer', 'AI探索者', '完成20次AI问答', '2026-06-18 11:30:00'),
                                                                                     (2, 'collector', '收藏达人', '收藏30个物种或知识', '2026-06-19 13:00:00'),
                                                                                     (2, 'knowledge_star', '知识之星', '达到Lv.10等级', '2026-06-20 15:30:00');

-- ==============================================
-- 10. 初始化积分流水
-- ==============================================
INSERT INTO point_transaction (user_id, points, biz_type, description, created_at) VALUES
                                                                                       (2, 20, 'quiz', '完成每日答题任务', '2026-06-20 14:30:22'),
                                                                                       (2, 10, 'task', '连续签到第7天额外奖励', '2026-06-20 09:15:08'),
                                                                                       (2, -100, 'shop', '兑换物种图鉴实体卡片', '2026-06-19 18:42:55'),
                                                                                       (2, 30, 'quiz', '连续答对5题额外加分', '2026-06-19 11:20:33'),
                                                                                       (2, 15, 'task', '发布生态观察记录', '2026-06-18 16:05:17'),
                                                                                       (2, 5, 'task', '每日签到', '2026-06-18 08:00:01');

-- 对应更新积分账户余额（模拟累计）
UPDATE user_point_account SET total_earned_points = 80, available_points = 100 WHERE user_id = 2;

-- ==============================================
-- 11. 初始化积分商店商品（含新增自定义称号）
-- ==============================================
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
                                                                                                (10, '火焰边框', 'frame_code:flame', 'avatar_frame', 400, NULL, 1),
                                                                                                (11, '烈焰虚线边框', 'frame_code:dashed', 'avatar_frame', 250, NULL, 1),
                                                                                                (12, '霓虹光效边框', 'frame_code:neon', 'avatar_frame', 350, NULL, 1),
                                                                                                (13, '极光幻彩边框', 'frame_code:aurora', 'avatar_frame', 450, NULL, 1),
                                                                                                (14, '冰晶之辉边框', 'frame_code:crystal', 'avatar_frame', 300, NULL, 1),
                                                                                                (15, '紫金皇冠边框', 'frame_code:royal', 'avatar_frame', 550, 50, 1),
                                                                                                (16, '✏️ 自定义称号', 'title_custom', 'virtual_item', 20000, NULL, 1);

-- ==============================================
-- 12. 初始化用户角色
-- ==============================================
INSERT INTO app_role (id, role_code, role_name, description) VALUES
                                                                 (1, 'ADMIN', '系统管理员', '拥有全部管理权限'),
                                                                 (2, 'MANAGER', '内容管理员', '管理百科、知识库、题库和AI内容'),
                                                                 (3, 'VISITOR', '普通用户', '使用C端学习与互动功能');

SET FOREIGN_KEY_CHECKS = 1;

ALTER TABLE marine_ecosystem ADD COLUMN image_url VARCHAR(500) DEFAULT NULL COMMENT '生态系统图片URL' AFTER cover_media_id;

-- 修复生态系统数据的 status 字段
-- 将所有 status 为 NULL 的生态系统数据设置为 1（正常状态）
UPDATE marine_ecosystem SET status = 1 WHERE status IS NULL;

-- 验证更新结果
SELECT id, name, status FROM marine_ecosystem;

-- 给 kb_document 表添加 species_id 字段
-- 用于关联物种，支持题目收藏知识库功能
-- =============================================

ALTER TABLE kb_document
    ADD COLUMN species_id BIGINT UNSIGNED NULL COMMENT '关联物种ID（当source_type=species时使用）'
        AFTER source_type;

-- 添加索引以加速查询
ALTER TABLE kb_document ADD INDEX idx_kb_species (species_id);

-- 更新现有数据（示例）：将中华白海豚相关的知识库文档关联到物种ID=1
UPDATE kb_document SET species_id = 1 WHERE title LIKE '%白海豚%' OR title LIKE '%海豚%';

INSERT INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
    (6, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual', 1);

-- 给 kb_document 表添加 species_id 字段
-- 用于关联物种，支持题目收藏知识库功能
-- =============================================

ALTER TABLE kb_document
    ADD COLUMN species_id BIGINT UNSIGNED NULL COMMENT '关联物种ID（当source_type=species时使用）'
        AFTER source_type;

-- 添加索引以加速查询
ALTER TABLE kb_document ADD INDEX idx_kb_species (species_id);

-- 更新现有数据（示例）：将中华白海豚相关的知识库文档关联到物种ID=1
UPDATE kb_document SET species_id = 1 WHERE title LIKE '%白海豚%' OR title LIKE '%海豚%';

INSERT INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
    (6, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual', 1);

-- 给 kb_document 表添加 species_id 字段
-- 用于关联物种，支持题目收藏知识库功能
-- =============================================

ALTER TABLE kb_document
    ADD COLUMN species_id BIGINT UNSIGNED NULL COMMENT '关联物种ID（当source_type=species时使用）'
        AFTER source_type;

-- 添加索引以加速查询
ALTER TABLE kb_document ADD INDEX idx_kb_species (species_id);

-- 更新现有数据（示例）：将中华白海豚相关的知识库文档关联到物种ID=1
UPDATE kb_document SET species_id = 1 WHERE title LIKE '%白海豚%' OR title LIKE '%海豚%';

INSERT INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
    (6, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual', 1);

-- 给 kb_document 表添加 species_id 字段
-- 用于关联物种，支持题目收藏知识库功能
-- =============================================

ALTER TABLE kb_document
    ADD COLUMN species_id BIGINT UNSIGNED NULL COMMENT '关联物种ID（当source_type=species时使用）'
        AFTER source_type;

-- 添加索引以加速查询
ALTER TABLE kb_document ADD INDEX idx_kb_species (species_id);

-- 更新现有数据（示例）：将中华白海豚相关的知识库文档关联到物种ID=1
UPDATE kb_document SET species_id = 1 WHERE title LIKE '%白海豚%' OR title LIKE '%海豚%';

INSERT INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
    (6, 1, '中华白海豚的生活习性', '中华白海豚主要分布于西太平洋和印度洋的沿岸水域。它们主要以河口水域的小型鱼类为食...', 'manual', 1);
-- ============================================================
-- 补充题库 species_id + 新增缺失的物种数据
-- 解决"收藏关联知识库显示未关联"的问题
-- ============================================================

-- ═══ 1. 先补全物种表（Seed_Data 中只有中华白海豚）═══
INSERT IGNORE INTO marine_species (id, chinese_name, scientific_name, kingdom, phylum, class_name, order_name, family_name, conservation_status, fun_fact) VALUES
                                                                                                                                                               (2, '绿海龟', 'Chelonia mydas', '动物界', '脊索动物门', '爬行纲', '龟鳖目', '海龟科', 'EN', '绿海龟是唯一植食性海龟，成年后体色变为绿色是因为吃多了藻类！'),
                                                                                                                                                               (3, '玳瑁', 'Eretmochelys imbricata', '动物界', '脊索动物门', '爬行纲', '龟鳖目', '海龟科', 'CR', '玳瑁的背甲像重叠的瓦片，俗称"十三鳞"，是珍贵的工艺品原料'),
                                                                                                                                                               (4, '棱皮龟', 'Dermochelys coriacea', '动物界', '脊索动物门', '爬行纲', '龟鳖目', '棱皮龟科', 'CR', '棱皮龟是最大的海龟，体重可超900公斤，背甲革质而非硬壳'),
                                                                                                                                                               (5, '儒艮', 'Dugong dugon', '动物界', '脊索动物门', '哺乳纲', '海牛目', '儒艮科', 'CR', '儒艮就是传说中的"美人鱼"！它们以海草为主食，在中国已功能性灭绝');

-- ═══ 2. 补全知识库文档（每个物种至少一条关联知识库）═══
INSERT IGNORE INTO kb_document (id, category_id, title, content, source_type, species_id) VALUES
                                                                                              (2, 1, '绿海龟的栖息地与习性', '绿海龟主要分布在热带和亚热带海域，喜欢在珊瑚礁和海草床附近活动。它们是洄游性动物，繁殖时会回到出生地产卵。', 'species', 2),
                                                                                              (3, 1, '玳瑁的特征与保护现状', '玳瑁以其美丽的背甲闻名，主要生活在珊瑚礁区域，以海绵为食。因过度捕捞濒临灭绝。', 'species', 3),
                                                                                              (4, 1, '棱皮龟——海洋巨兽', '棱皮龟是现存最大的爬行动物，能潜水至1200米深，主要以水母为食。', 'species', 4),
                                                                                              (5, 1, '儒艮——传说中的美人鱼', '儒艮是中国国家一级保护动物，主要分布于南海海域，以多种海草为食。', 'species', 5);

-- ═══ 3. 核心修复：给所有题目补全 species_id ═══
-- 通过题目内容中的关键词匹配对应物种

UPDATE quiz_question SET species_id = 1 WHERE species_id IS NULL AND (stem LIKE '%白海豚%' OR stem LIKE '%中华%');
UPDATE quiz_question SET species_id = 2 WHERE species_id IS NULL AND (stem LIKE '%海龟%' OR stem LIKE '%绿海龟%' OR stem LIKE '%绿蠵龟%' OR stem LIKE '%栖息地%' AND id > 1);
UPDATE quiz_question SET species_id = 3 WHERE species_id IS NULL AND (stem LIKE '%玳瑁%' OR stem LIKE '%十三鳞%');
UPDATE quiz_question SET species_id = 4 WHERE species_id IS NULL AND (stem LIKE '%棱皮龟%' OR stem LIKE '%革质%');
UPDATE quiz_question SET species_id = 5 WHERE species_id IS NULL AND (stem LIKE '%儒艮%' OR stem LIKE '%美人鱼%' OR stem LIKE '%海牛%');

-- ═══ 4. 兜底：如果还有没匹配上的题目，全部关联到默认物种(白海豚) ═══
-- 这样确保每道题都能收藏关联知识库
UPDATE quiz_question SET species_id = 1 WHERE species_id IS NULL;

-- 验证结果
SELECT q.id, LEFT(q.stem, 40) AS question_preview, q.species_id, s.chinese_name AS species_name
FROM quiz_question q
         LEFT JOIN marine_species s ON q.species_id = s.id
ORDER BY q.id;
-- ============================================
-- 一次性执行SQL：为 quiz_question 表添加 source_document_id 字段
-- 用于记录AI题目来源于哪个RAG知识库文档
--
-- 执行方式: 在MySQL中直接运行此文件
-- ============================================

ALTER TABLE quiz_question ADD COLUMN source_document_id BIGINT NULL COMMENT '来源知识库文档ID（AI从RAG生成时记录）' AFTER species_id;
-- V1.4: 为 quiz_question 表添加 source_document_id 字段
-- 用于记录AI题目来源于哪个RAG知识库文档（即使没有speciesId也能收藏关联知识库）
-- 日期: 2026-07-08

ALTER TABLE quiz_question ADD COLUMN source_document_id BIGINT NULL COMMENT '来源知识库文档ID（AI从RAG生成时记录）' AFTER species_id;
