import http from "@/utils/http";

/**
 * 消息通知服务 API
 */

// 获取当前登录用户未读通知条数
export const getUnreadCount = () =>
    http.get("/notification/unread-count");

// 获取用户的通知消息列表
export const getNotificationList = (params) =>
    http.get("/notification/list", { params });

// 将单条通知置为已读状态
export const markNotificationRead = (id) =>
    http.put(`/notification/${id}/read`);

// 一键清空所有未读状态
export const markAllRead = () =>
    http.put("/notification/read-all");

// 删除单条通知
export const deleteNotification = (id) =>
    http.delete(`/notification/${id}`);

// 清除所有通知
export const clearAllNotifications = () =>
    http.delete("/notification/all");
