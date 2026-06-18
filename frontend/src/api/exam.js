import http from "@/utils/http";

/**
 * C 端答题闯关 API
 */

// 开始答题：获取随机题目（不包含答案）
export const startExam = () => http.get("/exam/start");

// 提交答案
export const submitExam = (data) => http.post("/exam/submit", data);

// 答题历史
export const getExamHistory = () => http.get("/exam/history");

// TTS 语音合成
export const examTts = (data) => http.post("/exam/tts", data);
