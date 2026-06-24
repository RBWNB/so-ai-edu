import http from "@/utils/http";

/**
 * C 端观察记录 API
 * 对应后端 UserObservationController (/observation)
 */

// 发布观察记录
export const createObservation = (data) =>
    http.post("/observation", data);

// 获取当前用户观察记录列表
export const getObservationList = () =>
    http.get("/observation/list");

// 获取观察记录详情
export const getObservationDetail = (id) =>
    http.get(`/observation/${id}`);

// 更新观察记录
export const updateObservation = (id, data) =>
    http.put(`/observation/${id}`, data);

// 删除观察记录
export const deleteObservation = (id) =>
    http.delete(`/observation/${id}`);

// 上传观察照片
export const uploadObservationPhoto = (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/observation/upload/photo", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};

// ═══ 观察社区（所有用户可见的观察）═══

// 获取社区观察列表（分页）
export const getCommunityObservations = (params) =>
    http.get("/observation/community", { params });

// 获取社区观察详情
export const getCommunityObservationDetail = (id) =>
    http.get(`/observation/community/${id}`);
