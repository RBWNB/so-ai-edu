package com.gdou.marine.controller;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.CompetitionRecord;
import com.gdou.marine.mapper.CompetitionRecordMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 竞技模式 - 提交成绩 & 排行榜
 */
@RestController
@RequestMapping("/competition")
public class CompetitionController {

    private static final Logger log = LoggerFactory.getLogger(CompetitionController.class);

    @Autowired
    private CompetitionRecordMapper competitionRecordMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 提交竞技结果
     * POST /competition/submit
     */
    @Log(module = "竞技模式", description = "提交竞技成绩")
    @PostMapping("/submit")
    public Map<String, Object> submitResult(@RequestBody Map<String, Object> body,
                                            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            BigDecimal accuracy = new BigDecimal(body.get("accuracy").toString());
            int totalQuestions = Integer.parseInt(body.getOrDefault("totalQuestions", "10").toString());
            int correctCount = Integer.parseInt(body.getOrDefault("correctCount", "0").toString());
            long totalTimeMs = Long.parseLong(body.getOrDefault("totalTimeMs", "0").toString());
            long avgTimeMs = Long.parseLong(body.getOrDefault("avgTimeMs", "0").toString());
            String tier = (String) body.getOrDefault("tier", "青铜");

            // 计算综合排名分（与前端 getUserRanking 算法一致）
            int rankScore = calcRankScore(userId, accuracy, totalTimeMs > 0 ? avgTimeMs : 10000);

            // 保存记录
            CompetitionRecord record = new CompetitionRecord();
            record.setUserId(userId);
            record.setAccuracy(accuracy);
            record.setTotalQuestions(totalQuestions);
            record.setCorrectCount(correctCount);
            record.setTotalTimeMs(totalTimeMs);
            record.setAvgTimeMs(avgTimeMs);
            record.setTier(tier);
            record.setRankScore(rankScore);
            competitionRecordMapper.insert(record);

            // 查询用户当前排名
            Map<String, Object> rankInfo = queryUserRank(userId);

            result.put("success", true);
            result.put("message", "成绩已保存");
            result.put("data", Map.of(
                    "rank", rankInfo.getOrDefault("rank", 0),
                    "tier", tier,
                    "rankScore", rankScore
            ));
        } catch (Exception e) {
            log.error("提交竞技成绩失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取全局排行榜（Top 50）
     * GET /competition/leaderboard
     */
    @GetMapping("/leaderboard")
    public Map<String, Object> leaderboard(@RequestParam(defaultValue = "50") int limit,
                                           Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            // 聚合查询：按 user_id 分组，计算参赛次数、平均正确率、最佳段位
            String sql = """
                SELECT
                    cr.user_id,
                    u.username,
                    u.avatar_url,
                    u.avatar_frame,
                    COUNT(*) AS total_matches,
                    ROUND(AVG(cr.accuracy), 1) AS avg_accuracy,
                    MAX(cr.accuracy) AS best_accuracy,
                    MAX(cr.rank_score) AS best_rank_score,
                    (SELECT cr2.tier FROM competition_record cr2
                     WHERE cr2.user_id = cr.user_id
                     ORDER BY cr2.accuracy DESC, cr2.rank_score DESC LIMIT 1
                    ) AS best_tier,
                    SUM(cr.total_questions) AS total_answered
                FROM competition_record cr
                JOIN app_user u ON u.id = cr.user_id
                WHERE u.status = 1
                GROUP BY cr.user_id, u.username, u.avatar_url, u.avatar_frame
                ORDER BY avg_accuracy DESC, total_matches DESC, best_accuracy DESC
                LIMIT ?
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit);

            // 计算排名
            List<Map<String, Object>> leaderboard = new ArrayList<>();
            for (int i = 0; i < rows.size(); i++) {
                Map<String, Object> row = rows.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("rank", i + 1);
                item.put("userId", row.get("user_id"));
                item.put("username", row.get("username"));
                item.put("avatarUrl", row.get("avatar_url"));
                item.put("avatarFrame", row.getOrDefault("avatar_frame", "default"));
                item.put("totalMatches", row.get("total_matches"));
                item.put("averageAccuracy", row.get("avg_accuracy"));
                item.put("bestTier", row.get("best_tier"));
                item.put("totalAnswered", row.get("total_answered"));
                item.put("isMe", currentUserId != null &&
                        currentUserId.equals(((Number) row.get("user_id")).longValue()));
                leaderboard.add(item);
            }

            // 如果当前用户不在 Top 50 中，追加当前用户信息
            if (currentUserId != null && leaderboard.stream().noneMatch(item ->
                    Boolean.TRUE.equals(item.get("isMe")))) {
                Map<String, Object> myRank = queryUserRankWithDetails(currentUserId);
                if (myRank != null) {
                    leaderboard.add(myRank);
                }
            }

            result.put("success", true);
            result.put("data", leaderboard);
        } catch (Exception e) {
            log.error("获取排行榜失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取当前用户的竞技统计
     * GET /competition/my-stats
     */
    @GetMapping("/my-stats")
    public Map<String, Object> myStats(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            Map<String, Object> rankInfo = queryUserRankWithDetails(userId);
            result.put("success", true);
            result.put("data", rankInfo != null ? rankInfo : Collections.emptyMap());
        } catch (Exception e) {
            log.error("获取竞技统计失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ═══════════════════════════════════════
    // 内部方法
    // ═══════════════════════════════════════

    private int calcRankScore(Long userId, BigDecimal accuracy, long avgTimeMs) {
        double acc = accuracy.doubleValue();
        double avgSeconds = avgTimeMs / 1000.0;
        double speedScore = Math.max(0, Math.min(100, (10 - avgSeconds) / 8 * 100));

        // 查历史数据
        int totalAnswered = 0;
        int participationCount = 0;
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT COUNT(*) AS cnt, COALESCE(SUM(total_questions), 0) AS total_ans FROM competition_record WHERE user_id = ?",
                userId);
            if (!rows.isEmpty()) {
                Map<String, Object> row = rows.get(0);
                participationCount = ((Number) row.get("cnt")).intValue();
                totalAnswered = ((Number) row.get("total_ans")).intValue();
            }
        } catch (Exception ignored) {}

        double volumeScore = Math.min(100, totalAnswered);
        double expScore = Math.min(100, (participationCount + 1) * 10);
        double totalScore = acc * 0.45 + speedScore * 0.25 + volumeScore * 0.15 + expScore * 0.15;
        return (int) Math.round(Math.max(0, Math.min(100, totalScore)));
    }

    private Map<String, Object> queryUserRank(Long userId) {
        try {
            String sql = """
                SELECT COUNT(*) + 1 AS rank_num FROM (
                    SELECT cr.user_id, ROUND(AVG(cr.accuracy), 1) AS avg_acc, COUNT(*) AS cnt
                    FROM competition_record cr
                    GROUP BY cr.user_id
                ) t
                WHERE t.avg_acc > (
                    SELECT COALESCE(ROUND(AVG(cr2.accuracy), 1), 0)
                    FROM competition_record cr2 WHERE cr2.user_id = ?
                )
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            int rank = rows.isEmpty() ? 1 : ((Number) rows.get(0).get("rank_num")).intValue();
            return Map.of("rank", rank);
        } catch (Exception e) {
            return Map.of("rank", 1);
        }
    }

    private Map<String, Object> queryUserRankWithDetails(Long userId) {
        try {
            String sql = """
                SELECT
                    u.username,
                    u.avatar_url,
                    u.avatar_frame,
                    COUNT(*) AS total_matches,
                    ROUND(AVG(cr.accuracy), 1) AS avg_accuracy,
                    COALESCE(SUM(cr.total_questions), 0) AS total_answered,
                    (SELECT cr2.tier FROM competition_record cr2
                     WHERE cr2.user_id = cr.user_id
                     ORDER BY cr2.accuracy DESC LIMIT 1
                    ) AS best_tier
                FROM competition_record cr
                JOIN app_user u ON u.id = cr.user_id
                WHERE cr.user_id = ?
                GROUP BY cr.user_id, u.username, u.avatar_url, u.avatar_frame
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
            if (rows.isEmpty()) return null;

            Map<String, Object> row = rows.get(0);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", userId);
            item.put("username", row.get("username"));
            item.put("avatarUrl", row.get("avatar_url"));
            item.put("avatarFrame", row.getOrDefault("avatar_frame", "default"));
            item.put("totalMatches", row.get("total_matches"));
            item.put("averageAccuracy", row.get("avg_accuracy"));
            item.put("bestTier", row.get("best_tier"));
            item.put("totalAnswered", row.get("total_answered"));
            item.put("isMe", true);

            Map<String, Object> rankInfo = queryUserRank(userId);
            item.put("rank", rankInfo.get("rank"));

            return item;
        } catch (Exception e) {
            return null;
        }
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long uid) return uid;
        if (principal instanceof Integer uid) return uid.longValue();
        if (principal instanceof String text) {
            try { return Long.parseLong(text); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }
}
