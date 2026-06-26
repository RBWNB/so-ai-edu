import http from "@/utils/http";

/**
 * 竞技模式 API
 * 对应后端 CompetitionController (/competition)
 */

// 提交竞技成绩
export const submitCompetitionResult = (data) =>
    http.post("/competition/submit", data);

// 获取全局排行榜
export const getLeaderboard = (limit = 50) =>
    http.get("/competition/leaderboard", { params: { limit } });

// 竞技模式抽题（含答案，默认10道）
export const getCompetitionQuestions = (count = 10) =>
    http.get("/competition/questions", { params: { count } });

// 获取当前用户竞技统计
export const getMyCompetitionStats = () =>
    http.get("/competition/my-stats");
