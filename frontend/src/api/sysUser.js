import http from "@/utils/http";

export const getUserPage = (params) =>
    http.get("/sys-user/page", { params });

export const updateUserStatus = (id, status) =>
    http.put(`/sys-user/${id}/status`, { status });

export const assignUserRoles = (id, roleIds) =>
    http.put(`/sys-user/${id}/roles`, { roleIds });

/**
 * 获取当前登录用户的个人资料
 */
export const getUserProfile = () => {
    return http.get("/sys-user/profile");
};

/**
 * 更新个人资料（姓名、邮箱、手机号）
 */
export const updateUserProfile = (data) => {
    return http.put("/sys-user/profile", data);
};

/**
 * 上传头像
 * @param {File} file 文件对象
 */
export const uploadAvatarApi = (file) => {
    const formData = new FormData();
    formData.append("file", file);
    return http.post("/sys-user/upload/avatar", formData, {
        headers: {
            "Content-Type": "multipart/form-data",
        },
    });
};

/**
 * 修改密码
 */
export const updatePasswordApi = (data) =>
    http.put("/sys-user/password", data);

/**
 * 更新头像框
 */
export const updateAvatarFrameApi = (frameCode) =>
    http.put("/sys-user/avatar-frame", { frameCode });

/**
 * 审核通过用户
 */
export const approveUser = (id) =>
    http.post(`/sys-user/${id}/approve`);

/**
 * 审核驳回用户
 */
export const rejectUser = (id, rejectReason) =>
    http.post(`/sys-user/${id}/reject`, { rejectReason });