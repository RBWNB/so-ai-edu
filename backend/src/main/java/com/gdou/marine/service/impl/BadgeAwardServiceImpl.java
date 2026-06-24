package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.*;
import com.gdou.marine.mapper.*;
import com.gdou.marine.service.BadgeAwardService;
import com.gdou.marine.service.SpeciesBrowseRecordService;
import com.gdou.marine.service.UserLearningProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description 勋章自动颁发 ServiceImpl
 */
@Service
public class BadgeAwardServiceImpl implements BadgeAwardService {

    private static final Logger log = LoggerFactory.getLogger(BadgeAwardServiceImpl.class);

    private final UserBadgeMapper userBadgeMapper;
    private final QuizAttemptMapper quizAttemptMapper;
    private final UserLearningProfileService profileService;
    private final ConversationMessageMapper conversationMessageMapper;
    private final UserBookmarkMapper userBookmarkMapper;
    private final SpeciesBrowseRecordService browseRecordService;
    private final PointTransactionMapper pointTransactionMapper;

    public BadgeAwardServiceImpl(UserBadgeMapper userBadgeMapper,
                                 QuizAttemptMapper quizAttemptMapper,
                                 UserLearningProfileService profileService,
                                 ConversationMessageMapper conversationMessageMapper,
                                 UserBookmarkMapper userBookmarkMapper,
                                 SpeciesBrowseRecordService browseRecordService,
                                 PointTransactionMapper pointTransactionMapper) {
        this.userBadgeMapper = userBadgeMapper;
        this.quizAttemptMapper = quizAttemptMapper;
        this.profileService = profileService;
        this.conversationMessageMapper = conversationMessageMapper;
        this.userBookmarkMapper = userBookmarkMapper;
        this.browseRecordService = browseRecordService;
        this.pointTransactionMapper = pointTransactionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Map<String, String>> autoCheckAndAward(Long userId) {
        List<Map<String, String>> newlyAwarded = new ArrayList<>();

        if (userId == null || userId <= 0) return newlyAwarded;

        // 已获得徽章 code 集合
        Set<String> earnedCodes = userBadgeMapper.selectList(
                        new LambdaQueryWrapper<UserBadge>()
                                .eq(UserBadge::getUserId, userId))
                .stream()
                .map(UserBadge::getBadgeCode)
                .collect(Collectors.toSet());

        // ---- 逐个条件检测 ----
        if (!earnedCodes.contains("first_quiz") && checkFirstQuiz(userId)) {
            award(userId, "first_quiz", "初识海洋", "完成首次答题", newlyAwarded);
        }
        if (!earnedCodes.contains("quiz_master") && checkQuizMaster(userId)) {
            award(userId, "quiz_master", "答题达人", "累计答对100题", newlyAwarded);
        }
        if (!earnedCodes.contains("ai_explorer") && checkAiExplorer(userId)) {
            award(userId, "ai_explorer", "AI探索者", "完成20次AI问答", newlyAwarded);
        }
        if (!earnedCodes.contains("knowledge_star") && checkKnowledgeStar(userId)) {
            award(userId, "knowledge_star", "知识之星", "达到Lv.10等级", newlyAwarded);
        }
        if (!earnedCodes.contains("collector") && checkCollector(userId)) {
            award(userId, "collector", "收藏达人", "收藏30个物种或知识", newlyAwarded);
        }
        if (!earnedCodes.contains("persistence") && checkPersistence(userId)) {
            award(userId, "persistence", "持之以恒", "连续签到7天", newlyAwarded);
        }
        if (!earnedCodes.contains("perfect_streak") && checkPerfectStreak(userId)) {
            award(userId, "perfect_streak", "十连答对", "连续答对10题", newlyAwarded);
        }
        if (!earnedCodes.contains("eco_guardian") && checkEcoGuardian(userId)) {
            award(userId, "eco_guardian", "生态卫士", "浏览50个物种详情", newlyAwarded);
        }

        if (!newlyAwarded.isEmpty()) {
            log.info("用户 {} 新获得 {} 枚勋章", userId, newlyAwarded.size());
        }
        return newlyAwarded;
    }

    // ==================== 条件检测 ====================

    /** 完成首次答题：quiz_attempt 有任意记录 */
    private boolean checkFirstQuiz(Long userId) {
        return quizAttemptMapper.selectCount(
                new LambdaQueryWrapper<QuizAttempt>()
                        .eq(QuizAttempt::getUserId, userId)) > 0;
    }

    /** 累计答对100题 */
    private boolean checkQuizMaster(Long userId) {
        Long correctCount = quizAttemptMapper.selectCount(
                new LambdaQueryWrapper<QuizAttempt>()
                        .eq(QuizAttempt::getUserId, userId)
                        .eq(QuizAttempt::getIsCorrect, 1));
        return correctCount >= 100;
    }

    /** 完成20次AI问答：conversation_message 去重 session_id */
    private boolean checkAiExplorer(Long userId) {
        List<ConversationMessage> msgs = conversationMessageMapper.selectList(
                new LambdaQueryWrapper<ConversationMessage>()
                        .eq(ConversationMessage::getUserId, userId)
                        .select(ConversationMessage::getSessionId));
        long sessionCount = msgs.stream()
                .map(ConversationMessage::getSessionId)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return sessionCount >= 20;
    }

    /** 达到Lv.10：user_learning_profile.level >= 10 */
    private boolean checkKnowledgeStar(Long userId) {
        UserLearningProfile profile = profileService.getOrCreateProfile(userId);
        return profile.getLevel() >= 10;
    }

    /** 收藏30个：user_bookmark COUNT >= 30 */
    private boolean checkCollector(Long userId) {
        return userBookmarkMapper.selectCount(
                new LambdaQueryWrapper<UserBookmark>()
                        .eq(UserBookmark::getUserId, userId)) >= 30;
    }

    /** 连续签到7天：point_transaction 中找连续 7 天有签到记录 */
    private boolean checkPersistence(Long userId) {
        if (userId == null) return false;

        List<PointTransaction> records = pointTransactionMapper.selectList(
                new LambdaQueryWrapper<PointTransaction>()
                        .eq(PointTransaction::getUserId, userId)
                        .eq(PointTransaction::getBizType, "task")
                        .like(PointTransaction::getDescription, "签到")
                        .orderByAsc(PointTransaction::getCreatedAt));

        List<LocalDate> sortedDates = records.stream()
                .map(r -> r.getCreatedAt().toLocalDate())
                .distinct()
                .sorted()
                .toList();

        if (sortedDates.size() < 7) return false;

        int streak = 1;
        for (int i = 1; i < sortedDates.size(); i++) {
            if (sortedDates.get(i - 1).plusDays(1).equals(sortedDates.get(i))) {
                streak++;
                if (streak >= 7) return true;
            } else {
                streak = 1;
            }
        }
        return false;
    }

    /** 连续答对10题：quiz_attempt 按 attempted_at 排序，连续 is_correct=1 >= 10 */
    private boolean checkPerfectStreak(Long userId) {
        List<QuizAttempt> attempts = quizAttemptMapper.selectList(
                new LambdaQueryWrapper<QuizAttempt>()
                        .eq(QuizAttempt::getUserId, userId)
                        .orderByDesc(QuizAttempt::getAttemptedAt));

        int streak = 0;
        for (QuizAttempt a : attempts) {
            if (a.getIsCorrect() == 1) {
                streak++;
                if (streak >= 10) return true;
            } else {
                streak = 0;
            }
        }
        return false;
    }

    /** 浏览50个物种：基于 species_browse_record 去重统计 */
    private boolean checkEcoGuardian(Long userId) {
        return browseRecordService.countDistinctSpecies(userId) >= 50;
    }

    // ==================== 颁发逻辑 ====================

    private void award(Long userId, String code, String name, String desc, List<Map<String, String>> list) {
        UserBadge badge = new UserBadge();
        badge.setUserId(userId);
        badge.setBadgeCode(code);
        badge.setBadgeName(name);
        badge.setDescription(desc);
        badge.setEarnedAt(LocalDateTime.now());
        userBadgeMapper.insert(badge);

        Map<String, String> item = new HashMap<>();
        item.put("badgeCode", code);
        item.put("badgeName", name);
        list.add(item);
    }
}