package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.marine.entity.UserPointAccount;

/**
 * 积分账户 Service 接口
 * @author FlnyXx
 */
public interface UserPointAccountService extends IService<UserPointAccount> {

    /**
     * 获取或初始化积分账户
     * 首次访问自动创建（available=0, totalEarned=0, totalSpent=0）
     */
    UserPointAccount getOrCreateAccount(Long userId);

    /**
     * 增加积分（答题奖励、任务奖励等）
     * @return 变动后的 availablePoints
     */
    int earnPoints(Long userId, int amount, String bizType, Long bizId, String description);

    /**
     * 消费积分（兑换商品）
     * @return 变动后的 availablePoints
     * @throws IllegalStateException 积分不足
     */
    int spendPoints(Long userId, int amount, String bizType, Long bizId, String description);
}
