import http from "@/utils/http";

/**
 * C 端收藏 API
 * 对应后端 UserBookmarkController (/bookmark)
 */

// 取消收藏
export const removeBookmark = (targetType, targetId) =>
    http.delete(`/bookmark/${targetType}/${targetId}`);

// 添加收藏
export const addBookmark = (targetType, targetId) =>
    http.post(`/bookmark/${targetType}/${targetId}`);

// 获取收藏列表（按类型分组）
export const getBookmarkList = () =>
    http.get("/bookmark/list");
