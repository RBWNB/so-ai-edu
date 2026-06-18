import http from "@/utils/http";

/**
 * 题库管理 API
 */

// 分页查询题目列表
export const getQuestionPage = (params) => http.get("/quiz/question/page", { params });

// 获取题目详情
export const getQuestionById = (id) => http.get(`/quiz/question/${id}`);

// 人工新增题目
export const createQuestion = (data) => http.post("/quiz/question", data);

// 修改题目
export const updateQuestion = (id, data) => http.put(`/quiz/question/${id}`, data);

// 删除题目
export const deleteQuestion = (id) => http.delete(`/quiz/question/${id}`);

// 切换题目状态
export const toggleQuestionStatus = (id, status) =>
  http.put(`/quiz/question/${id}/status`, { status });

// AI 生成题目（预览）
export const aiGenerateQuestions = (data) =>
  http.post("/quiz/question/ai-generate", data);

// 批量保存AI生成的题目
export const batchSaveQuestions = (data) =>
  http.post("/quiz/question/batch-save", data);

// TTS 语音合成
export const synthesizeSpeech = (data) =>
  http.post("/quiz/question/tts", data);

/**
 * 知识库文档 API（供题库选择帖子用）
 */

// 分页查询知识库文档
export const getKbDocumentPage = (params) => http.get("/kb/document/page", { params });

// 获取知识库文档详情
export const getKbDocumentById = (id) => http.get(`/kb/document/${id}`);

// 获取知识分类列表
export const getKbCategoryList = () => http.get("/kb/category/list");
