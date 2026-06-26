package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.CompetitionRecord;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.mapper.CompetitionRecordMapper;
import com.gdou.marine.mapper.QuizQuestionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 竞技模式 - 抽题 / 提交成绩 / 排行榜
 */
@RestController
@RequestMapping("/competition")
public class CompetitionController {

    private static final Logger log = LoggerFactory.getLogger(CompetitionController.class);

    @Autowired
    private CompetitionRecordMapper competitionRecordMapper;

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.gdou.marine.service.UserPointAccountService userPointAccountService;

    /**
     * 竞技模式抽题：随机抽取指定数量已启用题目（含答案）
     * GET /competition/questions?count=10
     */
    @GetMapping("/questions")
    public Map<String, Object> getQuestions(@RequestParam(defaultValue = "10") int count,
                                            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            LambdaQueryWrapper<QuizQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuizQuestion::getStatus, 1);
            wrapper.orderByDesc(QuizQuestion::getCreatedAt);
            List<QuizQuestion> all = quizQuestionMapper.selectList(wrapper);

            if (all.isEmpty()) {
                result.put("success", false);
                result.put("message", "题库暂无题目");
                return result;
            }

            Collections.shuffle(all, new Random());
            int take = Math.min(all.size(), count);
            List<QuizQuestion> selected = all.subList(0, take);

            List<Map<String, Object>> list = new ArrayList<>();
            for (QuizQuestion q : selected) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", q.getId());
                item.put("questionType", q.getQuestionType());
                item.put("stem", q.getStem());
                item.put("optionsJson", q.getOptionsJson());
                item.put("difficulty", q.getDifficulty());
                item.put("correctAnswer", parseAnswerForFrontend(q.getAnswerJson()));
                item.put("explanation", q.getExplanation());
                list.add(item);
            }

            result.put("success", true);
            result.put("data", list);
        } catch (Exception e) {
            log.error("竞技抽题失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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

            // 计算本场积分：答对 1 题 2 分 + 全对额外 20 分
            int score = correctCount * 2 + (correctCount == totalQuestions ? 20 : 0);

            // 保存记录
            CompetitionRecord record = new CompetitionRecord();
            record.setUserId(userId);
            record.setAccuracy(accuracy);
            record.setTotalQuestions(totalQuestions);
            record.setCorrectCount(correctCount);
            record.setScore(score);
            record.setTotalTimeMs(totalTimeMs);
            record.setAvgTimeMs(avgTimeMs);
            record.setTier("");
            record.setRankScore(0);
            competitionRecordMapper.insert(record);

            // 同步到全局积分系统（个人中心积分明细可见）
            try {
                userPointAccountService.earnPoints(userId, score,
                        "competition", record.getId(),
                        "竞技模式答题（" + correctCount + "/" + totalQuestions + "）");
            } catch (Exception e) {
                log.error("竞技积分同步到全局积分失败", e);
            }

            // 基于累积数据计算排名（含累计积分）
            Map<String, Object> cumulative = calcCumulative(userId);

            result.put("success", true);
            result.put("message", "成绩已保存");
            result.put("data", Map.of(
                    "rank", cumulative.getOrDefault("rank", 0),
                    "tier", cumulative.getOrDefault("tier", "青铜"),
                    "totalMatches", cumulative.getOrDefault("totalMatches", 1),
                    "totalAnswered", cumulative.getOrDefault("totalAnswered", 0),
                    "cumulativeAccuracy", cumulative.getOrDefault("cumulativeAccuracy", accuracy),
                    "totalScore", cumulative.getOrDefault("totalScore", 0),
                    "matchScore", score
            ));
        } catch (Exception e) {
            log.error("提交竞技成绩失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取全局排行榜（Top 50，按积分 > 正确率 > 答题量排序）
     * GET /competition/leaderboard?limit=50
     */
    @GetMapping("/leaderboard")
    public Map<String, Object> leaderboard(@RequestParam(defaultValue = "50") int limit,
                                           Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            // 累积聚合：每位用户汇总所有竞技记录，积分优先
            String sql = """
                SELECT
                    cr.user_id,
                    u.username,
                    u.avatar_url,
                    u.avatar_frame,
                    COUNT(*) AS total_matches,
                    COALESCE(SUM(cr.total_questions), 0) AS total_answered,
                    COALESCE(SUM(cr.correct_count), 0) AS total_correct,
                    COALESCE(SUM(cr.score), 0) AS total_score,
                    ROUND(COALESCE(SUM(cr.correct_count), 0) * 100.0 / NULLIF(SUM(cr.total_questions), 0), 1) AS cumulative_accuracy,
                    COALESCE(SUM(cr.total_time_ms), 0) AS total_time_ms
                FROM competition_record cr
                JOIN app_user u ON u.id = cr.user_id
                WHERE u.status = 1
                GROUP BY cr.user_id, u.username, u.avatar_url, u.avatar_frame
                ORDER BY total_score DESC, cumulative_accuracy DESC, total_answered DESC
                LIMIT ?
                """;

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, limit);

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
                item.put("totalAnswered", row.get("total_answered"));

                int totalScore = ((Number) row.get("total_score")).intValue();
                item.put("totalScore", totalScore);

                double cumulativeAccuracy = ((Number) row.get("cumulative_accuracy")).doubleValue();
                item.put("cumulativeAccuracy", cumulativeAccuracy);

                int totalMatches = ((Number) row.get("total_matches")).intValue();
                int totalAnswered = ((Number) row.get("total_answered")).intValue();
                item.put("tier", calcCumulativeTier(cumulativeAccuracy, totalMatches, totalAnswered));

                item.put("isMe", currentUserId != null &&
                        currentUserId.equals(((Number) row.get("user_id")).longValue()));
                leaderboard.add(item);
            }

            // 当前用户不在 Top 50 → 追加
            if (currentUserId != null && leaderboard.stream().noneMatch(it ->
                    Boolean.TRUE.equals(it.get("isMe")))) {
                Map<String, Object> my = queryUserCumulative(currentUserId);
                if (my != null) leaderboard.add(my);
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
     * 当前用户竞技统计
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
            Map<String, Object> info = queryUserCumulative(userId);
            result.put("success", true);
            result.put("data", info != null ? info : Collections.emptyMap());
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

    /**
     * 解析 answerJson 为前端可直接比较的格式
     * DB 存储格式为 JSON 编码，如 "A"、["A","B"]，需要去引号/解析
     */
    private String parseAnswerForFrontend(String answerJson) {
        if (answerJson == null || answerJson.isEmpty()) return "";
        String trimmed = answerJson.trim();
        try {
            // JSON 字符串格式："A" 或 "正确"
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
            // JSON 数组格式：["A","B","C"]
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                List<String> arr = objectMapper.readValue(trimmed, List.class);
                return String.join(",", arr);
            }
        } catch (Exception ignored) {
        }
        return trimmed;
    }

    /**
     * 累积段位算法：兼顾正确率 + 答题量
     * 单场高分不能直接王者，必须有足够的场次和题量支撑
     */
    private String calcCumulativeTier(double accuracy, int totalMatches, int totalAnswered) {
        double volumeBonus = Math.min(totalAnswered / 50.0, 10);   // 答题量最多加 10 分
        double matchBonus = Math.min(totalMatches * 1.5, 10);      // 参赛次数最多加 10 分
        double score = accuracy * 0.8 + volumeBonus + matchBonus;
        if (score >= 95) return "王者";
        if (score >= 82) return "钻石";
        if (score >= 68) return "黄金";
        if (score >= 50) return "白银";
        return "青铜";
    }

    private Map<String, Object> calcCumulative(Long userId) {
        try {
            Map<String, Object> row = queryUserCumulativeRaw(userId);
            if (row == null) return Map.of("rank", 1, "tier", "青铜", "totalMatches", 0, "totalAnswered", 0, "cumulativeAccuracy", 0, "totalScore", 0);

            double acc = ((Number) row.getOrDefault("cumulative_accuracy", 0)).doubleValue();
            int matches = ((Number) row.getOrDefault("total_matches", 0)).intValue();
            int answered = ((Number) row.getOrDefault("total_answered", 0)).intValue();
            int totalScore = ((Number) row.getOrDefault("total_score", 0)).intValue();
            String tier = calcCumulativeTier(acc, matches, answered);

            int rank = queryCumulativeRank(userId, totalScore, acc, answered, matches);

            return Map.of("rank", rank, "tier", tier, "totalMatches", matches, "totalAnswered", answered, "cumulativeAccuracy", Math.round(acc * 10) / 10.0, "totalScore", totalScore);
        } catch (Exception e) {
            return Map.of("rank", 1, "tier", "青铜", "totalMatches", 1, "totalAnswered", 0, "cumulativeAccuracy", 0, "totalScore", 0);
        }
    }

    private Map<String, Object> queryUserCumulativeRaw(Long userId) {
        String sql = """
            SELECT
                COUNT(*) AS total_matches,
                COALESCE(SUM(total_questions), 0) AS total_answered,
                COALESCE(SUM(correct_count), 0) AS total_correct,
                COALESCE(SUM(score), 0) AS total_score,
                ROUND(COALESCE(SUM(correct_count), 0) * 100.0 / NULLIF(SUM(total_questions), 0), 1) AS cumulative_accuracy
            FROM competition_record WHERE user_id = ?
            """;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private int queryCumulativeRank(Long userId, int totalScore, double accuracy, int totalAnswered, int totalMatches) {
        try {
            String sql = """
                SELECT COUNT(*) + 1 AS rank_num FROM (
                    SELECT cr.user_id,
                        COALESCE(SUM(cr.score), 0) AS ts,
                        ROUND(COALESCE(SUM(cr.correct_count), 0) * 100.0 / NULLIF(SUM(cr.total_questions), 0), 1) AS acc,
                        COALESCE(SUM(cr.total_questions), 0) AS ans,
                        COUNT(*) AS cnt
                    FROM competition_record cr GROUP BY cr.user_id
                ) t
                WHERE t.ts > ?
                   OR (t.ts = ? AND t.acc > ?)
                   OR (t.ts = ? AND t.acc = ? AND t.ans > ?)
                   OR (t.ts = ? AND t.acc = ? AND t.ans = ? AND t.cnt > ?)
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql,
                    totalScore, totalScore, accuracy, totalScore, accuracy, totalAnswered, totalScore, accuracy, totalAnswered, totalMatches);
            return rows.isEmpty() ? 1 : ((Number) rows.get(0).get("rank_num")).intValue();
        } catch (Exception e) {
            return 1;
        }
    }

    private Map<String, Object> queryUserCumulative(Long userId) {
        try {
            Map<String, Object> raw = queryUserCumulativeRaw(userId);
            if (raw == null) return null;

            double acc = ((Number) raw.get("cumulative_accuracy")).doubleValue();
            int matches = ((Number) raw.get("total_matches")).intValue();
            int answered = ((Number) raw.get("total_answered")).intValue();
            int totalScore = ((Number) raw.get("total_score")).intValue();

            String tier = calcCumulativeTier(acc, matches, answered);
            int rank = queryCumulativeRank(userId, totalScore, acc, answered, matches);

            // 用户名等信息
            List<Map<String, Object>> users = jdbcTemplate.queryForList(
                    "SELECT username, avatar_url, avatar_frame FROM app_user WHERE id = ?", userId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", rank);
            item.put("userId", userId);
            item.put("username", users.isEmpty() ? "" : users.get(0).get("username"));
            item.put("avatarUrl", users.isEmpty() ? "" : users.get(0).get("avatar_url"));
            item.put("avatarFrame", users.isEmpty() ? "default" : users.get(0).getOrDefault("avatar_frame", "default"));
            item.put("totalMatches", matches);
            item.put("totalAnswered", answered);
            item.put("totalScore", totalScore);
            item.put("cumulativeAccuracy", Math.round(acc * 10) / 10.0);
            item.put("tier", tier);
            item.put("isMe", true);
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
