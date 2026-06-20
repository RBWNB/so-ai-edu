package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.marine.entity.UserLearningProfile;

/**
 * 学习画像 Service 接口
 * @author FlnyXx
 */
public interface UserLearningProfileService extends IService<UserLearningProfile> {

    /**
     * 获取或初始化学习画像
     * 如果用户首次访问（无画像记录），自动创建一条默认记录
     * @param userId 用户ID
     * @return 学习画像（永不返回 null）
     */
    UserLearningProfile getOrCreateProfile(Long userId);

    /**
     * 答题后更新学习画像统计
     * @param userId    用户ID
     * @param isCorrect 本次答题是否正确
     */
    void updateAfterQuiz(Long userId, boolean isCorrect);
}
