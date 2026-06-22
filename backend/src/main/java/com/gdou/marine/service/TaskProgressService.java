package com.gdou.marine.service;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description 每日任务进度 Service
 */
public interface TaskProgressService {

    /**
     * 按任务类型递增进度
     * 自动查找 learning_task WHERE task_type=? AND status=1，
     * 对 user_task_record 进度+1，达标时标记 completed=1
     */
    void incrementProgress(Long userId, String taskType);

    /**
     * 递增进度（指定步长，如浏览5个一次性+5）
     */
    void incrementProgress(Long userId, String taskType, int step);

    /**
     * 每日签到：仅当日首次有效
     * @return true=签到成功  false=今日已签到
     */
    boolean dailyCheckin(Long userId);
}
