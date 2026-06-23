import http from "@/utils/http";

/**
 * C 端积分 API
 * 对应后端 PointsController (/points)
 */

// 积分余额（含累计获得/消耗）
export const getPointsAccount = () => http.get("/points/account");

// 积分流水（分页）
export const getPointsTransactions = (pageNum = 1, pageSize = 6) =>
  http.get("/points/transactions", { params: { pageNum, pageSize } });

// 兑换记录（分页）
export const getExchangeOrders = (pageNum = 1, pageSize = 6) =>
  http.get("/points/exchange-orders", { params: { pageNum, pageSize } });

// 积分商店商品列表
export const getShopItems = () => http.get("/points/shop-items");

// 兑换商品
export const exchangeItem = (itemId) => http.post(`/points/exchange/${itemId}`);

// 已拥有的头像框编码列表
export const getOwnedFrames = () => http.get("/points/owned-frames");
