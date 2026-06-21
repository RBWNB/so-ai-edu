import http from "@/utils/http";

/**
 * C 端学习 API
 * 对应后端 LearningController (/learning)
 */

// 获取学习画像概览（含等级、答题统计、错题数）
export const getLearningProfile = () => http.get("/learning/profile");

// 获取最近答题记录（分页）
export const getAnswerHistory = (pageNum = 1, pageSize = 6) =>
  http.get("/learning/answer-history", { params: { pageNum, pageSize } });

// 获取 AI 问答会话数统计
export const getAiSessionCount = () => http.get("/learning/ai-session-count");

// 获取错题本列表（分页）
export const getWrongBook = (pageNum = 1, pageSize = 10) =>
  http.get("/learning/wrong-book", { params: { pageNum, pageSize } });
