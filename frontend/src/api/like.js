import http from "@/utils/http";

/**
 * 点赞 API
 * 对应后端 ContentLikeController (/like)
 */

// 切换点赞状态
export const toggleLike = (targetType, targetId) =>
    http.post("/like/toggle", { targetType, targetId });

// 获取点赞状态和数量
export const getLikeStatus = (targetType, targetId) =>
    http.get(`/like/status/${targetType}/${targetId}`);

// 批量获取点赞状态
export const batchLikeStatus = (targetType, targetIds) =>
    http.post("/like/batch-status", { targetType, targetIds });
