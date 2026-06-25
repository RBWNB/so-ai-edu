import http from "@/utils/http";

/**
 * 评论 API
 * 对应后端 ContentCommentController (/comment)
 */

// 获取某个目标的评论列表（支持 sort=latest|hot）
export const getComments = (targetType, targetId, params) =>
    http.get(`/comment/${targetType}/${targetId}`, params);

// 获取子评论（回复）
export const getReplies = (parentId) =>
    http.get(`/comment/${parentId}/replies`);

// 发布评论
export const createComment = (data) =>
    http.post("/comment", data);

// 删除评论
export const deleteComment = (id) =>
    http.delete(`/comment/${id}`);
