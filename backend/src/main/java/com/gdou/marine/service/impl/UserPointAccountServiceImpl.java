package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.entity.PointTransaction;
import com.gdou.marine.entity.UserPointAccount;
import com.gdou.marine.mapper.PointTransactionMapper;
import com.gdou.marine.mapper.UserPointAccountMapper;
import com.gdou.marine.service.UserPointAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 积分账户 Service 实现
 * @author FlnyXx
 */
@Service
public class UserPointAccountServiceImpl
        extends ServiceImpl<UserPointAccountMapper, UserPointAccount>
        implements UserPointAccountService {

    private static final Logger log = LoggerFactory.getLogger(UserPointAccountServiceImpl.class);

    private final PointTransactionMapper pointTransactionMapper;

    public UserPointAccountServiceImpl(PointTransactionMapper pointTransactionMapper) {
        this.pointTransactionMapper = pointTransactionMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserPointAccount getOrCreateAccount(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
        UserPointAccount account = this.getOne(
                new LambdaQueryWrapper<UserPointAccount>()
                        .eq(UserPointAccount::getUserId, userId), false);
        if (account == null) {
            account = new UserPointAccount();
            account.setUserId(userId);
            account.setAvailablePoints(0);
            account.setTotalEarnedPoints(0);
            account.setTotalSpentPoints(0);
            account.setUpdatedAt(LocalDateTime.now());
            this.save(account);
            log.info("初始化用户 {} 的积分账户", userId);
        }
        return account;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int earnPoints(Long userId, int amount, String bizType, Long bizId, String description) {
        if (amount <= 0) throw new IllegalArgumentException("积分数必须大于0");

        UserPointAccount account = getOrCreateAccount(userId);
        int newAvailable = account.getAvailablePoints() + amount;
        int newTotalEarned = account.getTotalEarnedPoints() + amount;

        this.lambdaUpdate()
                .eq(UserPointAccount::getUserId, userId)
                .set(UserPointAccount::getAvailablePoints, newAvailable)
                .set(UserPointAccount::getTotalEarnedPoints, newTotalEarned)
                .set(UserPointAccount::getUpdatedAt, LocalDateTime.now())
                .update();

        // 记录流水
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setPoints(amount);  // 正数 = 收入
        tx.setBizType(bizType);
        tx.setBizId(bizId);
        tx.setDescription(description);
        tx.setCreatedAt(LocalDateTime.now());
        pointTransactionMapper.insert(tx);

        log.info("用户 {} 获得积分 +{} (业务: {})", userId, amount, bizType);
        return newAvailable;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int spendPoints(Long userId, int amount, String bizType, Long bizId, String description) {
        if (amount <= 0) throw new IllegalArgumentException("积分数必须大于0");

        UserPointAccount account = getOrCreateAccount(userId);
        if (account.getAvailablePoints() < amount) {
            throw new IllegalStateException("积分不足，需要" + amount + "，当前" + account.getAvailablePoints());
        }

        int newAvailable = account.getAvailablePoints() - amount;
        int newTotalSpent = account.getTotalSpentPoints() + amount;

        this.lambdaUpdate()
                .eq(UserPointAccount::getUserId, userId)
                .set(UserPointAccount::getAvailablePoints, newAvailable)
                .set(UserPointAccount::getTotalSpentPoints, newTotalSpent)
                .set(UserPointAccount::getUpdatedAt, LocalDateTime.now())
                .update();

        // 记录流水（负数 = 支出）
        PointTransaction tx = new PointTransaction();
        tx.setUserId(userId);
        tx.setPoints(-amount);
        tx.setBizType(bizType);
        tx.setBizId(bizId);
        tx.setDescription(description);
        tx.setCreatedAt(LocalDateTime.now());
        pointTransactionMapper.insert(tx);

        log.info("用户 {} 消费积分 -{} (业务: {})", userId, amount, bizType);
        return newAvailable;
    }
}
