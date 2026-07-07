package com.gdou.marine.service;

import com.gdou.marine.service.impl.ZhipuAiServiceImpl;
import com.gdou.marine.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description UGC 高光提炼服务
 */
@Service
public class HighlightBroadcastService {
    private static final Logger log = LoggerFactory.getLogger(HighlightBroadcastService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ZhipuAiServiceImpl zhipuAiService;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 生成并发送高光时刻广播
     */
    public void generateAndSendHighlight() {
        try {
            // 1. 找出 48 小时内有过互动（收到点赞或评论）且已过审的帖子，按总点赞数排序
            String sql = """
                SELECT o.id, o.title, o.description, o.location_name,
                       u.username, s.chinese_name, s.conservation_status,
                       COUNT(l.id) as like_count
                FROM user_observation o
                JOIN app_user u ON o.user_id = u.id
                LEFT JOIN marine_species s ON o.species_id = s.id
                LEFT JOIN content_like l ON l.target_type = 'user_observation' AND l.target_id = o.id
                WHERE o.status = 1
                  AND o.id IN (
                      SELECT target_id FROM content_like
                      WHERE target_type = 'user_observation' AND created_at >= NOW() - INTERVAL 48 HOUR
                      UNION
                      SELECT target_id FROM content_comment
                      WHERE target_type = 'user_observation' AND status = 1 AND created_at >= NOW() - INTERVAL 48 HOUR
                  )
                GROUP BY o.id, o.title, o.description, o.location_name, o.created_at, u.username, s.chinese_name, s.conservation_status
                ORDER BY like_count DESC, o.created_at DESC
                LIMIT 1
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            if (rows.isEmpty()) {
                log.info("今日无高光素材，跳过广播");
                return;
            }

            Map<String, Object> topPost = rows.get(0);
            Long postId = ((Number) topPost.get("id")).longValue();
            String username = (String) topPost.get("username");
            String title = (String) topPost.get("title");
            String description = (String) topPost.get("description");
            String locationName = (String) topPost.getOrDefault("location_name", "未知海域");
            String speciesName = (String) topPost.getOrDefault("chinese_name", "神秘海洋生物");

            // 2. 构造 AI Prompt：这就是 AI 发挥价值的地方！
            String systemPrompt = "你是一个海洋社区的爆款文案运营官。你的任务是将用户的观察记录包装成一条极具吸引力的【系统广播】（40-60字左右）。" +
                    "规则：" +
                    "1. 必须带有悬念或震惊感，吸引人去点击查看。" +
                    "2. 如果物种名已知，利用你的知识简单提一句该物种的奇特之处或珍稀程度（如“粉色精灵”）。" +
                    "3. 语气要像真人在发掘宝藏，活泼自然，不要出现Markdown符号。" +
                    "4. 格式固定以【社区高光】开头。";

            String userInput = String.format("发现者：@%s。地点：%s。识别物种：%s。用户原贴标题：%s。用户原贴描述：%s。",
                    username, locationName, speciesName, title, description);

            // 3. 调用大模型
            String aiContent = zhipuAiService.callAI(userInput, systemPrompt);
            log.info("AI生成的高光广播: {}", aiContent);

            // 4. 将 AI 生成的推荐语作为全站广播逐条发送（雪花 ID 每行唯一）
            List<Long> activeUserIds = jdbcTemplate.queryForList(
                    "SELECT id FROM app_user WHERE status = 1", Long.class);

            int count = 0;
            for (Long receiverId : activeUserIds) {
                jdbcTemplate.update(
                        "INSERT INTO system_notification (id, receiver_id, sender_id, type, target_id, post_id, content) VALUES (?, ?, 0, 'broadcast_link', 0, ?, ?)",
                        snowflakeIdGenerator.nextId(), receiverId, postId, aiContent);
                count++;
            }
            log.info("高光时刻广播发送完毕，覆盖用户数：{}", count);

        } catch (Exception e) {
            log.error("生成高光时刻广播失败", e);
        }
    }
}
