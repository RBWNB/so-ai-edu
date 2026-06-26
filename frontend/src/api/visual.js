import http from "@/utils/http";

/** 保护等级统计 */
export const getSpeciesProtectionStats = () =>
    http.get("/visual/species-protection-stats");

/** 分类单元统计（phylum/class/order） */
export const getSpeciesTaxonomyStats = (level) =>
    http.get("/visual/species-taxonomy-stats", { params: { level } });

/** 生态系统统计 */
export const getEcosystemStats = () =>
    http.get("/visual/ecosystem-stats");

/** 综合看板汇总 */
export const getDashboardSummary = () =>
    http.get("/visual/summary");

/** CSV 数据导出 */
export const exportCsv = (type) =>
    `/api/visual/export/csv?type=${type}`;

/** 物种原始数据 */
export const getSpeciesRawList = (params) => http.get("/species", { params });

/** 物种数量统计（包含分类和保护等级） */
export const getSpeciesCountStats = (level) =>
    http.get("/visual/species-count-stats", { params: { level } });

/** 获取顶部 KPI 核心指标 */
export const getAdminDashboardKpi = () =>
    http.get("/visual/admin/dashboard-kpi");

/** 获取 AI 问答词云数据 */
export const getAiWordCloud = () =>
    http.get("/visual/admin/ai-word-cloud");

/** 获取近7天活跃度(答题)趋势 */
export const getActivityTrend = () =>
    http.get("/visual/admin/activity-trend");

// 发送全站广播
export const sendSystemBroadcast = (data) =>
    http.post("/notification/admin/broadcast", data);
