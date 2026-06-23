package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.LearningTask;
import com.gdou.marine.entity.UserTaskRecord;
import com.gdou.marine.mapper.LearningTaskMapper;
import com.gdou.marine.mapper.UserTaskRecordMapper;
import com.gdou.marine.service.TaskProgressService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description 每日任务进度 ServiceImpl
 */

@Service
public class TaskProgressServiceImpl implements TaskProgressService {

    private static final Logger log = LoggerFactory.getLogger(TaskProgressServiceImpl.class);

    private final LearningTaskMapper learningTaskMapper;
    private final UserTaskRecordMapper userTaskRecordMapper;

    public TaskProgressServiceImpl(LearningTaskMapper learningTaskMapper,
                                   UserTaskRecordMapper userTaskRecordMapper) {
        this.learningTaskMapper = learningTaskMapper;
        this.userTaskRecordMapper = userTaskRecordMapper;
    }

    @Override
    public void incrementProgress(Long userId, String taskType) {
        incrementProgress(userId, taskType, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementProgress(Long userId, String taskType, int step) {
        if (userId == null || taskType == null || step <= 0) return;

        // 找对应任务定义
        LearningTask task = learningTaskMapper.selectOne(
                new LambdaQueryWrapper<LearningTask>()
                        .eq(LearningTask::getTaskType, taskType)
                        .eq(LearningTask::getStatus, (byte) 1));
        if (task == null) {
            log.debug("任务类型 {} 未启用或无定义", taskType);
            return;
        }

        // 查找今天的任务记录（按 task_date 隔离每日进度）
        LocalDate today = LocalDate.now();
        UserTaskRecord rec = userTaskRecordMapper.selectOne(
                new LambdaQueryWrapper<UserTaskRecord>()
                        .eq(UserTaskRecord::getUserId, userId)
                        .eq(UserTaskRecord::getTaskId, task.getId())
                        .eq(UserTaskRecord::getTaskDate, today));

        if (rec == null) {
            rec = new UserTaskRecord();
            rec.setUserId(userId);
            rec.setTaskId(task.getId());
            rec.setTaskDate(today);
            rec.setProgressValue(0);
            rec.setCompleted((byte) 0);
            rec.setRewardClaimed((byte) 0);
            rec.setCreatedAt(LocalDateTime.now());
        }

        // 已完成的不再更新
        if (rec.getCompleted() == 1) return;

        int newProgress = Math.min(rec.getProgressValue() + step, task.getTargetValue());
        boolean justCompleted = newProgress >= task.getTargetValue();

        rec.setProgressValue(newProgress);
        if (justCompleted) {
            rec.setCompleted((byte) 1);
            rec.setCompletedAt(LocalDateTime.now());
        }
        rec.setUpdatedAt(LocalDateTime.now());

        if (rec.getId() == null) {
            userTaskRecordMapper.insert(rec);
        } else {
            userTaskRecordMapper.updateById(rec);
        }

        if (justCompleted) {
            log.info("用户 {} 完成任务「{}」", userId, task.getTitle());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean dailyCheckin(Long userId) {
        if (userId == null) return false;

        LearningTask task = learningTaskMapper.selectOne(
                new LambdaQueryWrapper<LearningTask>()
                        .eq(LearningTask::getTaskType, "daily_checkin")
                        .eq(LearningTask::getStatus, (byte) 1));
        if (task == null) return false;

        LocalDate today = LocalDate.now();

        // 查今日是否已签到（利用 task_date 索引，简洁准确）
        UserTaskRecord rec = userTaskRecordMapper.selectOne(
                new LambdaQueryWrapper<UserTaskRecord>()
                        .eq(UserTaskRecord::getUserId, userId)
                        .eq(UserTaskRecord::getTaskId, task.getId())
                        .eq(UserTaskRecord::getTaskDate, today));

        if (rec != null) {
            return false; // 今日已签到
        }

        // 执行签到（新建今日记录）
        rec = new UserTaskRecord();
        rec.setUserId(userId);
        rec.setTaskId(task.getId());
        rec.setTaskDate(today);
        rec.setProgressValue(1);
        rec.setCompleted((byte) 1);
        rec.setCompletedAt(LocalDateTime.now());
        rec.setRewardClaimed((byte) 0);
        rec.setCreatedAt(LocalDateTime.now());
        userTaskRecordMapper.insert(rec);

        log.info("用户 {} 每日签到成功", userId);
        return true;
    }
}
