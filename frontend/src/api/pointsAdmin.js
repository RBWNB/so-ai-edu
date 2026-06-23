import http from "@/utils/http";

/**
 * B 端积分商店管理 API
 * 对应后端 PointsController /points/admin/*
 */

// 商品列表（含已下架）
export const adminListItems = () => http.get("/points/admin/items");

// 更新商品
export const adminUpdateItem = (id, data) => http.put(`/points/admin/items/${id}`, data);

// 新增商品
export const adminAddItem = (data) => http.post("/points/admin/items", data);
