import http from "@/utils/http";

/**
 * 获取操作日志分页列表
 */
export const getOperationLogPage = (params) =>
    http.get("/sys-operation-log/page", { params });
