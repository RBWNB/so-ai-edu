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

// 根据物种ID获取关联的知识库文档（用于题目收藏知识库功能）
export const getKbBySpeciesId = (speciesId) =>
    http.get(`/bookmark/kb-by-species/${speciesId}`);

// 根据文档ID获取知识库详情（用于RAG出题无speciesId时，通过sourceDocumentId收藏）
export const getKbByDocumentId = (documentId) =>
    http.get(`/bookmark/kb-by-doc/${documentId}`);
