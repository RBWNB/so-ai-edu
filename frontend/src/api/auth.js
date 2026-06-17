import http from "@/utils/http";

export const loginApi = (data) => http.post('/api/auth/login', data)
export const registerApi = (data) => http.post("/api/auth/register", data);
export const logoutApi = () => http.post("/api/auth/logout");

