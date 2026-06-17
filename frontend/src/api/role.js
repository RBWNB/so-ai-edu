import http from "@/utils/http";

export const getRolePage = (params) => http.get("/sys-role/page", { params });

export const createRole = (data) => http.post("/sys-role", data);

export const updateRole = (id, data) => http.put(`/sys-role/${id}`, data);

export const deleteRole = (id) => http.delete(`/sys-role/${id}`);

export const updateRoleStatus = (id, status) =>
  http.put(`/sys-role/${id}/status`, { status });

export const getEnabledRoleList = () => http.get("/sys-role/enabled-list");
