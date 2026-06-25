import http from "@/utils/http";

/**
 * C 端成就 API
 * 对应后端 AchievementController (/achievement)
 */

// 获取已获得勋章列表
export const getBadges = () => http.get("/achievement/badges");

// 获取每日任务（含当前用户进度）
export const getDailyTasks = () => http.get("/achievement/daily-tasks");

// 领取任务奖励
export const claimTaskReward = (taskId) => http.post(`/achievement/claim/${taskId}`);

// 每日签到
export const dailyCheckin = () => http.post("/achievement/checkin");

// 获取已获得勋章数量（用于称号解锁判断）
export const getBadgeCount = () => http.get("/achievement/badge-count");
