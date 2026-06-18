import axios from "axios";
import { ElMessage } from "element-plus";
import { getStoredToken } from "@/store/auth";

const http = axios.create({
  baseURL: "/api",
  timeout: 120000,
});

http.interceptors.request.use(
  (config) => {
    const token = getStoredToken();
    if (token) {
      config.headers = config.headers || {};
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

http.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error?.response?.status;
    const messageFromServer =
      error?.response?.data?.message ||
      error?.response?.data?.msg ||
      error?.response?.data?.error;

    let friendly = messageFromServer || "请求失败，请稍后重试";
    if (status === 401) {
      friendly = "登录已过期或未登录，请重新登录";
      localStorage.removeItem("marine_token");
      localStorage.removeItem("marine_username");
      if (window.location.pathname !== "/login") {
        window.location.href = "/login";
      }
    } else if (status === 403) {
      friendly = "没有权限访问该资源";
    } else if (error?.code === "ECONNABORTED") {
      friendly = "请求超时，请检查网络后重试";
    } else if (!error?.response) {
      friendly = "网络异常，请检查网络连接";
    }

    ElMessage.error(friendly);
    return Promise.reject(error);
  }
);

export default http;

