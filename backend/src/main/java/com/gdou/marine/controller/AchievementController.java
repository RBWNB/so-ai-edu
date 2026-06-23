package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.LearningTask;
import com.gdou.marine.entity.UserBadge;
import com.gdou.marine.entity.UserTaskRecord;
import com.gdou.marine.mapper.LearningTaskMapper;
import com.gdou.marine.mapper.UserBadgeMapper;
import com.gdou.marine.mapper.UserTaskRecordMapper;
import com.gdou.marine.service.BadgeAwardService;
import com.gdou.marine.service.TaskProgressService;
import com.gdou.marine.service.UserPointAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description C 端成就 Controller —— 勋章墙 + 每日任务
 */
@RestController
@RequestMapping("/achievement")
public class AchievementController {

    private static final Logger log = LoggerFactory.getLogger(AchievementController.class);

    @Autowired
    private UserBadgeMapper userBadgeMapper;

    @Autowired
    private LearningTaskMapper learningTaskMapper;

    @Autowired
    private UserTaskRecordMapper userTaskRecordMapper;

    @Autowired
    private BadgeAwardService badgeAwardService;

    @Autowired
    private TaskProgressService taskProgressService;

    @Autowired
    private UserPointAccountService userPointAccountService;

    /**
     * 勋章墙：已获得勋章 + 全量勋章定义（前端区分灰显）
     * 已获得：user_badge  WHERE user_id = ?
     * 未获得：从 point_shop_item WHERE item_type='badge' 或前端静态列表取差值
     */
    @GetMapping("/badges")
    public Map<String, Object> getBadges(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 自动检测并颁发满足条件的新勋章（幂等）
            List<Map<String, String>> newlyAwarded = badgeAwardService.autoCheckAndAward(userId);

            List<UserBadge> earnedBadges = userBadgeMapper.selectList(
                    new LambdaQueryWrapper<UserBadge>()
                            .eq(UserBadge::getUserId, userId)
                            .orderByDesc(UserBadge::getEarnedAt));

            List<Map<String, Object>> list = earnedBadges.stream().map(b -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("badgeCode", b.getBadgeCode());
                item.put("badgeName", b.getBadgeName());
                item.put("description", b.getDescription());
                item.put("earnedAt", b.getEarnedAt() != null
                        ? b.getEarnedAt().toString().replace("T", " ") : "");
                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", list);
            result.put("newlyAwarded", newlyAwarded);
        } catch (Exception e) {
            log.error("获取勋章列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 每日任务列表（含当前用户完成进度）
     * LEFT JOIN learning_task + user_task_record
     */
    @GetMapping("/daily-tasks")
    public Map<String, Object> getDailyTasks(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查询所有启用的任务
            List<LearningTask> tasks = learningTaskMapper.selectList(
                    new LambdaQueryWrapper<LearningTask>()
                            .eq(LearningTask::getStatus, (byte) 1)
                            .orderByAsc(LearningTask::getId));

            // 查询用户今日任务记录（按 task_date 隔离，每天独立）
            List<UserTaskRecord> records = userTaskRecordMapper.selectList(
                    new LambdaQueryWrapper<UserTaskRecord>()
                            .eq(UserTaskRecord::getUserId, userId)
                            .eq(UserTaskRecord::getTaskDate, LocalDate.now()));

            Map<Long, UserTaskRecord> recordMap = records.stream()
                    .collect(Collectors.toMap(UserTaskRecord::getTaskId, r -> r, (a, b) -> b));

            List<Map<String, Object>> list = new ArrayList<>();
            for (LearningTask task : tasks) {
                UserTaskRecord rec = recordMap.get(task.getId());
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("taskId", task.getId());
                item.put("title", task.getTitle());
                item.put("taskType", task.getTaskType());
                item.put("targetValue", task.getTargetValue());
                item.put("rewardPoints", task.getRewardPoints());
                item.put("progressValue", rec != null ? rec.getProgressValue() : 0);
                item.put("completed", rec != null && rec.getCompleted() == 1);
                item.put("rewardClaimed", rec != null && rec.getRewardClaimed() == 1);
                item.put("progressPercent", task.getTargetValue() > 0
                        ? Math.min(100, (rec != null ? rec.getProgressValue() : 0) * 100 / task.getTargetValue())
                        : 0);
                list.add(item);
            }

            result.put("success", true);
            result.put("data", list);
        } catch (Exception e) {
            log.error("获取每日任务失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 领取任务奖励
     * 前置条件：任务已完成 + 奖励未领取
     */
    @Log(module = "积分模块", description = "领取任务奖励")
    @PostMapping("/claim/{taskId}")
    public Map<String, Object> claimReward(@PathVariable Long taskId, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 校验任务存在且启用
            LearningTask task = learningTaskMapper.selectById(taskId);
            if (task == null || task.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "任务不存在或已下架");
                return result;
            }

            // 查询用户今日任务记录（按 task_date 定位今日记录）
            UserTaskRecord rec = userTaskRecordMapper.selectOne(
                    new LambdaQueryWrapper<UserTaskRecord>()
                            .eq(UserTaskRecord::getUserId, userId)
                            .eq(UserTaskRecord::getTaskId, taskId)
                            .eq(UserTaskRecord::getTaskDate, LocalDate.now()));
            if (rec == null) {
                result.put("success", false);
                result.put("message", "今日尚未完成该任务");
                return result;
            }
            if (rec.getCompleted() != 1) {
                result.put("success", false);
                result.put("message", "任务尚未完成");
                return result;
            }
            if (rec.getRewardClaimed() == 1) {
                result.put("success", false);
                result.put("message", "奖励已领取，请勿重复操作");
                return result;
            }

            // 标记已领取
            rec.setRewardClaimed((byte) 1);
            rec.setUpdatedAt(LocalDateTime.now());
            userTaskRecordMapper.updateById(rec);

            // 发放积分奖励
            userPointAccountService.earnPoints(userId, task.getRewardPoints(),
                    "task", taskId, "完成每日任务：" + task.getTitle());

            result.put("success", true);
            result.put("message", "已领取「" + task.getTitle() + "」奖励 +" + task.getRewardPoints() + " 积分");
            result.put("data", Map.of("rewardPoints", task.getRewardPoints()));
        } catch (Exception e) {
            log.error("领取奖励失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 每日签到（原地完成，无需跳转）
     */
    @Log(module = "积分模块", description = "每日签到")
    @PostMapping("/checkin")
    public Map<String, Object> checkin(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            boolean ok = taskProgressService.dailyCheckin(userId);
            if (ok) {
                result.put("success", true);
                result.put("message", "签到成功 +5 积分");
            } else {
                result.put("success", false);
                result.put("message", "今日已签到，明天再来吧");
            }
        } catch (Exception e) {
            log.error("签到失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 私有方法 ====================

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
