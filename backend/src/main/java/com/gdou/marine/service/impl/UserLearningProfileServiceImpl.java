package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.entity.UserLearningProfile;
import com.gdou.marine.mapper.UserLearningProfileMapper;
import com.gdou.marine.service.UserLearningProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * 学习画像 Service 实现
 * @author FlnyXx
 */
@Service
public class UserLearningProfileServiceImpl
        extends ServiceImpl<UserLearningProfileMapper, UserLearningProfile>
        implements UserLearningProfileService {

    private static final Logger log = LoggerFactory.getLogger(UserLearningProfileServiceImpl.class);

    private static final int DEFAULT_LEVEL = 1;
    private static final int DEFAULT_TOTAL = 0;
    private static final int DEFAULT_CORRECT = 0;
    private static final BigDecimal DEFAULT_RATE = BigDecimal.ZERO;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserLearningProfile getOrCreateProfile(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }

        UserLearningProfile profile = this.getOne(
                new LambdaQueryWrapper<UserLearningProfile>()
                        .eq(UserLearningProfile::getUserId, userId), false);

        if (profile == null) {
            profile = new UserLearningProfile();
            profile.setUserId(userId);
            profile.setLevel(DEFAULT_LEVEL);
            profile.setTotalQuestions(DEFAULT_TOTAL);
            profile.setCorrectCount(DEFAULT_CORRECT);
            profile.setCorrectRate(DEFAULT_RATE);
            profile.setUpdatedAt(LocalDateTime.now());
            this.save(profile);
            log.info("初始化用户 {} 的学习画像", userId);
        }

        return profile;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAfterQuiz(Long userId, boolean isCorrect) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }

        // 确保画像存在
        UserLearningProfile profile = getOrCreateProfile(userId);

        int newTotal = profile.getTotalQuestions() + 1;
        int newCorrect = profile.getCorrectCount() + (isCorrect ? 1 : 0);
        BigDecimal newRate = BigDecimal.valueOf(newCorrect)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(newTotal), 2, RoundingMode.HALF_UP);

        // 每答对 20 题升 1 级（简单公式，后续可扩展）
        int newLevel = newCorrect / 20 + 1;

        boolean updated = this.lambdaUpdate()
                .eq(UserLearningProfile::getUserId, userId)
                .set(UserLearningProfile::getTotalQuestions, newTotal)
                .set(UserLearningProfile::getCorrectCount, newCorrect)
                .set(UserLearningProfile::getCorrectRate, newRate)
                .set(UserLearningProfile::getLevel, newLevel)
                .set(UserLearningProfile::getUpdatedAt, LocalDateTime.now())
                .update();

        if (!updated) {
            log.warn("更新用户 {} 的学习画像失败", userId);
        } else {
            log.debug("用户 {} 学习画像已更新: total={}, correct={}, rate={}%, level={}",
                    userId, newTotal, newCorrect, newRate, newLevel);
        }
    }
}
