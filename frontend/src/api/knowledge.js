import axios from "axios";
import { getStoredToken } from "@/store/auth";

const api = axios.create({
  baseURL: "/api",
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = getStoredToken();
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 分页查询知识库文档
 */
export const getDocumentPage = (params) =>
  api.get("/kb/documents", { params });

/**
 * 上传文档（PDF/Word/TXT），Tika 自动解析
 */
export const uploadDocument = (formData) =>
  api.post("/kb/documents/upload", formData);

/**
 * 手动创建知识库文档
 */
export const createDocument = (data) => api.post("/kb/documents", data);

/**
 * 删除文档（级联删除 chunks + 向量）
 */
export const deleteDocument = (id) => api.delete(`/kb/documents/${id}`);

export default api;
