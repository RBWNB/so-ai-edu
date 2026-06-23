package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.ConversationMessage;
import com.gdou.marine.entity.PointExchangeOrder;
import com.gdou.marine.entity.QuizAttempt;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.entity.UserLearningProfile;
import com.gdou.marine.mapper.ConversationMessageMapper;
import com.gdou.marine.mapper.PointExchangeOrderMapper;
import com.gdou.marine.mapper.QuizAttemptMapper;
import com.gdou.marine.mapper.QuizQuestionMapper;
import com.gdou.marine.service.UserLearningProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * C 端学习 Controller
 * 提供学习画像、错题本、答题记录查询
 * @author FlnyXx
 */
@RestController
@RequestMapping("/learning")
public class LearningController {

    private static final Logger log = LoggerFactory.getLogger(LearningController.class);

    @Autowired
    private UserLearningProfileService userLearningProfileService;

    @Autowired
    private QuizAttemptMapper quizAttemptMapper;

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Autowired
    private ConversationMessageMapper conversationMessageMapper;

    @Autowired
    private PointExchangeOrderMapper pointExchangeOrderMapper;

    /**
     * 获取学习画像概览（含错题统计）
     * 对应前端 Tab3 统计卡片 + 左侧等级 + 错题本数量
     */
    @GetMapping("/profile")
    public Map<String, Object> getProfile(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 从 quiz_attempt 实时统计（保证数据永远准确，不依赖同步）
            List<QuizAttempt> allAttempts = quizAttemptMapper.selectList(
                    new LambdaQueryWrapper<QuizAttempt>()
                            .eq(QuizAttempt::getUserId, userId)
            );

            long totalQuestions = allAttempts.size();
            long correctCount = allAttempts.stream()
                    .filter(a -> a.getIsCorrect() == 1)
                    .count();
            long wrongQuestionCount = allAttempts.stream()
                    .filter(a -> a.getIsCorrect() == 0)
                    .count();

            // 正确率：保留两位小数（避免除零）
            BigDecimal correctRate = totalQuestions > 0
                    ? BigDecimal.valueOf(correctCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(totalQuestions), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;

            // 双倍经验卡生效检查
            boolean doubleXp = hasDoubleXpToday(userId);
            long effectiveCorrect = doubleXp ? correctCount * 2 : correctCount;

            // 等级算法：每答对 20 题升 1 级，最低 1 级
            // 双倍卡：正确数×2 计算等级
            int level = (int) (effectiveCorrect / 20) + 1;

            // 同步回 user_learning_profile（存原始数据，等级用双倍后的）
            syncProfile(userId, level, totalQuestions, correctCount, correctRate);

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("level", level);
            data.put("totalQuestions", totalQuestions);
            data.put("correctCount", correctCount);
            data.put("correctRate", correctRate);
            data.put("doubleXp", doubleXp);
            data.put("xpExpireAt", doubleXp ? getXpExpireAt(userId) : null);
            data.put("wrongQuestionCount", wrongQuestionCount);
            // weakPoints / preferredCategories 暂从画像表读取，后续可扩展
            UserLearningProfile profile = userLearningProfileService.getOrCreateProfile(userId);
            data.put("weakPoints", profile.getWeakPoints());
            data.put("preferredCategories", profile.getPreferredCategories());

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取学习画像失败", e);
            result.put("success", false);
            result.put("message", "获取学习画像失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 最近答题记录（分页）
     * 对应前端 Tab3 答题记录表格
     * 返回字段对齐 quiz_attempt JOIN quiz_question
     */
    @GetMapping("/answer-history")
    public Map<String, Object> getAnswerHistory(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "6") Integer pageSize,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查询最近答题记录
            List<QuizAttempt> attempts = quizAttemptMapper.selectList(
                    new LambdaQueryWrapper<QuizAttempt>()
                            .eq(QuizAttempt::getUserId, userId)
                            .orderByDesc(QuizAttempt::getAttemptedAt)
                            .last("LIMIT " + ((pageNum - 1) * pageSize) + ", " + pageSize)
            );

            // 查询总数
            Long total = quizAttemptMapper.selectCount(
                    new LambdaQueryWrapper<QuizAttempt>()
                            .eq(QuizAttempt::getUserId, userId)
            );

            // 批量获取题目信息
            List<Long> questionIds = attempts.stream()
                    .map(QuizAttempt::getQuestionId)
                    .distinct()
                    .collect(Collectors.toList());
            Map<Long, QuizQuestion> questionMap = new HashMap<>();
            if (!questionIds.isEmpty()) {
                List<QuizQuestion> questions = quizQuestionMapper.selectBatchIds(questionIds);
                questionMap = questions.stream()
                        .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
            }

            // 组装返回数据
            List<Map<String, Object>> records = new ArrayList<>();
            for (QuizAttempt att : attempts) {
                QuizQuestion question = questionMap.get(att.getQuestionId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", att.getId());
                item.put("stem", question != null ? question.getStem() : "题目已删除");
                item.put("questionType", question != null
                        ? mapQuestionType(question.getQuestionType()) : "未知");
                item.put("isCorrect", att.getIsCorrect() == 1);
                item.put("timeSpent", formatTimeSpent(att.getTimeSpentSeconds()));
                item.put("attemptedAt", att.getAttemptedAt() != null
                        ? att.getAttemptedAt().toString().replace("T", " ") : "");
                records.add(item);
            }

            Map<String, Object> page = new LinkedHashMap<>();
            page.put("pageNum", pageNum);
            page.put("pageSize", pageSize);
            page.put("total", total);
            page.put("records", records);

            result.put("success", true);
            result.put("data", page);
        } catch (Exception e) {
            log.error("获取答题记录失败", e);
            result.put("success", false);
            result.put("message", "获取答题记录失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * AI 问答会话数统计
     * 对应前端 Tab3 AI 问答历史快捷入口
     */
    @GetMapping("/ai-session-count")
    public Map<String, Object> getAiSessionCount(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 从 conversation_message 表按 session_id 去重统计
            Long sessionCount = countDistinctSessions(userId);

            result.put("success", true);
            result.put("data", Map.of("sessionCount", sessionCount));
        } catch (Exception e) {
            log.error("获取AI会话统计失败", e);
            result.put("success", false);
            result.put("message", "获取AI会话统计失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 错题本列表（分页）
     * 只展示"最近一次做错"的题目——如果用户后来做对了同一道题，自动移出错题本
     * 返回：题目信息 + 错误次数 + 最后错误时间
     */
    @GetMapping("/wrong-book")
    public Map<String, Object> getWrongBook(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查该用户所有答题记录，按时间倒序
            List<QuizAttempt> allAttempts = quizAttemptMapper.selectList(
                    new LambdaQueryWrapper<QuizAttempt>()
                            .eq(QuizAttempt::getUserId, userId)
                            .orderByDesc(QuizAttempt::getAttemptedAt)
            );

            // 按 question_id 分组，取每组最新一条
            Map<Long, List<QuizAttempt>> grouped = allAttempts.stream()
                    .collect(Collectors.groupingBy(
                            QuizAttempt::getQuestionId,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            // 筛选：最新一次是做错的 → 还在错题本里
            List<Map<String, Object>> wrongList = new ArrayList<>();
            for (Map.Entry<Long, List<QuizAttempt>> entry : grouped.entrySet()) {
                Long questionId = entry.getKey();
                List<QuizAttempt> atts = entry.getValue();

                // 最新一条记录
                QuizAttempt latest = atts.get(0);
                if (latest.getIsCorrect() == 1) {
                    // 用户后来做对了 → 不算错题
                    continue;
                }

                // 统计错误次数 + 最后错误时间
                long wrongCount = atts.stream()
                        .filter(a -> a.getIsCorrect() == 0)
                        .count();
                LocalDateTime lastWrongTime = atts.stream()
                        .filter(a -> a.getIsCorrect() == 0)
                        .findFirst()
                        .map(QuizAttempt::getAttemptedAt)
                        .orElse(null);

                QuizQuestion question = quizQuestionMapper.selectById(questionId);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("questionId", questionId);
                item.put("stem", question != null ? question.getStem() : "题目已删除");
                item.put("questionType", question != null
                        ? mapQuestionType(question.getQuestionType()) : "未知");
                item.put("explanation", question != null ? question.getExplanation() : "");
                item.put("wrongCount", wrongCount);
                item.put("lastWrongTime", lastWrongTime != null
                        ? lastWrongTime.toString().replace("T", " ") : "");
                wrongList.add(item);
            }

            // 手动分页
            int total = wrongList.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            List<Map<String, Object>> pageRecords = fromIndex < total
                    ? wrongList.subList(fromIndex, toIndex)
                    : Collections.emptyList();

            Map<String, Object> page = new LinkedHashMap<>();
            page.put("pageNum", pageNum);
            page.put("pageSize", pageSize);
            page.put("total", total);
            page.put("records", pageRecords);

            result.put("success", true);
            result.put("data", page);
        } catch (Exception e) {
            log.error("获取错题本失败", e);
            result.put("success", false);
            result.put("message", "获取错题本失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 检查双倍经验是否生效（过期时间 > 当前时间）
     */
    private boolean hasDoubleXpToday(Long userId) {
        String expireAt = getXpExpireAt(userId);
        if (expireAt == null) return false;
        try {
            java.time.LocalDateTime expire = java.time.LocalDateTime.parse(
                    expireAt.replace(" ", "T"));
            return expire.isAfter(java.time.LocalDateTime.now());
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 计算双倍经验过期时间（所有卡叠加，每张 +24h，断档则重置）
     */
    private String getXpExpireAt(Long userId) {
        try {
            // 查最近 7 天内所有双倍卡订单，按时间升序
            java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);
            List<PointExchangeOrder> orders = pointExchangeOrderMapper.selectList(
                    new LambdaQueryWrapper<PointExchangeOrder>()
                            .eq(PointExchangeOrder::getUserId, userId)
                            .eq(PointExchangeOrder::getItemId, 5L)
                            .ge(PointExchangeOrder::getCreatedAt, since)
                            .orderByAsc(PointExchangeOrder::getCreatedAt));

            if (orders.isEmpty()) return null;

            // 叠加计算：每张卡 +24h，连续购买则累加，断档（>24h 间隔）则重置
            java.time.LocalDateTime expireAt = null;
            for (PointExchangeOrder o : orders) {
                if (o.getCreatedAt() == null) continue;
                if (expireAt == null || o.getCreatedAt().isAfter(expireAt)) {
                    // 断档：从这张开始重新算
                    expireAt = o.getCreatedAt().plusHours(24);
                } else {
                    // 连续：在现有基础上再 +24h
                    expireAt = expireAt.plusHours(24);
                }
            }
            return expireAt != null ? expireAt.toString().replace("T", " ") : null;
        } catch (Exception e) {
            return null;
        }
    }

    private PointExchangeOrder getLatestXpOrder(Long userId) {
        try {
            java.time.LocalDateTime since = java.time.LocalDateTime.now().minusDays(7);
            List<PointExchangeOrder> orders = pointExchangeOrderMapper.selectList(
                    new LambdaQueryWrapper<PointExchangeOrder>()
                            .eq(PointExchangeOrder::getUserId, userId)
                            .eq(PointExchangeOrder::getItemId, 5L)
                            .ge(PointExchangeOrder::getCreatedAt, since)
                            .orderByDesc(PointExchangeOrder::getCreatedAt)
                            .last("LIMIT 1"));
            return orders.isEmpty() ? null : orders.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 将实时统计结果同步到 user_learning_profile 表
     * （已有记录则更新，无记录则创建）
     */
    private void syncProfile(Long userId, int level, long totalQuestions,
                             long correctCount, BigDecimal correctRate) {
        try {
            userLearningProfileService.getOrCreateProfile(userId);
            userLearningProfileService.lambdaUpdate()
                    .eq(UserLearningProfile::getUserId, userId)
                    .set(UserLearningProfile::getLevel, level)
                    .set(UserLearningProfile::getTotalQuestions, (int) totalQuestions)
                    .set(UserLearningProfile::getCorrectCount, (int) correctCount)
                    .set(UserLearningProfile::getCorrectRate, correctRate)
                    .set(UserLearningProfile::getUpdatedAt, LocalDateTime.now())
                    .update();
        } catch (Exception e) {
            log.warn("同步学习画像失败: userId={}", userId, e);
        }
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long uid) return uid;
        if (principal instanceof Integer uid) return uid.longValue();
        if (principal instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String mapQuestionType(String dbType) {
        if (dbType == null) return "未知";
        return switch (dbType) {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "judge" -> "判断题";
            case "short_answer" -> "简答题";
            default -> dbType;
        };
    }

    private String formatTimeSpent(Integer seconds) {
        if (seconds == null || seconds == 0) return "--";
        if (seconds < 60) return seconds + "s";
        int min = seconds / 60;
        int sec = seconds % 60;
        return sec > 0 ? min + "m" + sec + "s" : min + "m";
    }

    /**
     * 统计 AI 会话数：查询用户所有消息的 session_id 并去重
     * DB: conversation_message (user_id, session_id)
     */
    private Long countDistinctSessions(Long userId) {
        List<ConversationMessage> messages = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getUserId, userId)
                        .select(ConversationMessage::getSessionId)
        );
        return messages.stream()
                .map(ConversationMessage::getSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }
}
