package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.PointExchangeOrder;
import com.gdou.marine.entity.PointShopItem;
import com.gdou.marine.entity.PointTransaction;
import com.gdou.marine.entity.UserPointAccount;
import com.gdou.marine.mapper.PointExchangeOrderMapper;
import com.gdou.marine.mapper.PointShopItemMapper;
import com.gdou.marine.mapper.PointTransactionMapper;
import com.gdou.marine.service.UserPointAccountService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/22
 * @Description C 端积分 Controller
 */
@RestController
@RequestMapping("/points")
public class PointsController {

    private static final Logger log = LoggerFactory.getLogger(PointsController.class);

    @Autowired
    private UserPointAccountService userPointAccountService;

    @Autowired
    private PointTransactionMapper pointTransactionMapper;

    @Autowired
    private PointExchangeOrderMapper pointExchangeOrderMapper;

    @Autowired
    private PointShopItemMapper pointShopItemMapper;

    /** 积分余额（含累计统计） */
    @GetMapping("/account")
    public Map<String, Object> getAccount(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            UserPointAccount account = userPointAccountService.getOrCreateAccount(userId);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("availablePoints", account.getAvailablePoints());
            data.put("totalEarned", account.getTotalEarnedPoints());
            data.put("totalSpent", account.getTotalSpentPoints());
            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取积分账户失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 积分流水（分页） */
    @GetMapping("/transactions")
    public Map<String, Object> getTransactions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "6") Integer pageSize,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            Long total = pointTransactionMapper.selectCount(
                    new LambdaQueryWrapper<PointTransaction>()
                            .eq(PointTransaction::getUserId, userId));

            int offset = (pageNum - 1) * pageSize;
            List<PointTransaction> list = pointTransactionMapper.selectList(
                    new LambdaQueryWrapper<PointTransaction>()
                            .eq(PointTransaction::getUserId, userId)
                            .orderByDesc(PointTransaction::getCreatedAt)
                            .last("LIMIT " + offset + ", " + pageSize));

            List<Map<String, Object>> records = list.stream().map(tx -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", tx.getId());
                item.put("points", tx.getPoints());
                item.put("bizType", tx.getBizType());
                item.put("bizTypeLabel", bizTypeLabel(tx.getBizType()));
                item.put("description", tx.getDescription());
                item.put("createdAt", tx.getCreatedAt() != null
                        ? tx.getCreatedAt().toString().replace("T", " ") : "");
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> page = new LinkedHashMap<>();
            page.put("pageNum", pageNum);
            page.put("pageSize", pageSize);
            page.put("total", total);
            page.put("records", records);
            result.put("success", true);
            result.put("data", page);
        } catch (Exception e) {
            log.error("获取积分流水失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 兑换记录（分页），JOIN point_shop_item 获取商品名 */
    @GetMapping("/exchange-orders")
    public Map<String, Object> getExchangeOrders(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "6") Integer pageSize,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            Long total = pointExchangeOrderMapper.selectCount(
                    new LambdaQueryWrapper<PointExchangeOrder>()
                            .eq(PointExchangeOrder::getUserId, userId));

            int offset = (pageNum - 1) * pageSize;
            List<PointExchangeOrder> orders = pointExchangeOrderMapper.selectList(
                    new LambdaQueryWrapper<PointExchangeOrder>()
                            .eq(PointExchangeOrder::getUserId, userId)
                            .orderByDesc(PointExchangeOrder::getCreatedAt)
                            .last("LIMIT " + offset + ", " + pageSize));

            // 批量查商品名
            List<Long> itemIds = orders.stream()
                    .map(PointExchangeOrder::getItemId)
                    .distinct().collect(Collectors.toList());
            Map<Long, String> itemNameMap = new HashMap<>();
            if (!itemIds.isEmpty()) {
                pointShopItemMapper.selectBatchIds(itemIds)
                        .forEach(i -> itemNameMap.put(i.getId(), i.getName()));
            }

            List<Map<String, Object>> records = orders.stream().map(o -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", o.getId());
                item.put("goodsName", itemNameMap.getOrDefault(o.getItemId(), "商品已下架"));
                item.put("pointsCost", o.getPointsCost());
                item.put("orderStatus", o.getOrderStatus());
                item.put("createdAt", o.getCreatedAt() != null
                        ? o.getCreatedAt().toString().replace("T", " ") : "");
                return item;
            }).collect(Collectors.toList());

            Map<String, Object> page = new LinkedHashMap<>();
            page.put("pageNum", pageNum);
            page.put("pageSize", pageSize);
            page.put("total", total);
            page.put("records", records);
            result.put("success", true);
            result.put("data", page);
        } catch (Exception e) {
            log.error("获取兑换记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 积分商店商品列表 */
    @GetMapping("/shop-items")
    public Map<String, Object> getShopItems(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<PointShopItem> items = pointShopItemMapper.selectList(
                    new LambdaQueryWrapper<PointShopItem>()
                            .eq(PointShopItem::getStatus, (byte) 1)
                            .orderByAsc(PointShopItem::getPointsPrice));

            List<Map<String, Object>> list = items.stream().map(i -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", i.getId());
                item.put("name", i.getName());
                item.put("description", i.getDescription());
                item.put("itemType", i.getItemType());
                item.put("pointsPrice", i.getPointsPrice());
                item.put("stock", i.getStock()); // null=无限
                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", list);
        } catch (Exception e) {
            log.error("获取积分商店失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 兑换商品 */
    @PostMapping("/exchange/{itemId}")
    public Map<String, Object> exchange(@PathVariable Long itemId, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            PointShopItem item = pointShopItemMapper.selectById(itemId);
            if (item == null || item.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "商品不存在或已下架");
                return result;
            }

            // 库存检查
            if (item.getStock() != null && item.getStock() <= 0) {
                result.put("success", false);
                result.put("message", "该商品已售罄");
                return result;
            }

            // 扣积分（余额不足会抛异常）
            UserPointAccount account = new UserPointAccount();
            account.setUserId(userId);
            account.setAvailablePoints(0);
            try {
                userPointAccountService.spendPoints(userId, item.getPointsPrice(),
                        "shop", item.getId(), "兑换商品：" + item.getName());
            } catch (IllegalStateException e) {
                result.put("success", false);
                result.put("message", "积分不足，需要" + item.getPointsPrice() + "积分");
                return result;
            }

            // 扣库存
            if (item.getStock() != null) {
                item.setStock(item.getStock() - 1);
                pointShopItemMapper.updateById(item);
            }

            // 建订单
            PointExchangeOrder order = new PointExchangeOrder();
            order.setUserId(userId);
            order.setItemId(itemId);
            order.setPointsCost(item.getPointsPrice());
            order.setOrderStatus("SUCCESS");
            order.setCreatedAt(java.time.LocalDateTime.now());
            pointExchangeOrderMapper.insert(order);

            result.put("success", true);
            result.put("message", "兑换成功！已消耗" + item.getPointsPrice() + "积分");
        } catch (Exception e) {
            log.error("兑换失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ==================== 私有方法 ====================

    private Long extractUserId(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) return null;
        Object principal = auth.getPrincipal();
        if (principal instanceof Long uid) return uid;
        if (principal instanceof Integer uid) return uid.longValue();
        if (principal instanceof String text) {
            try { return Long.parseLong(text); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private String bizTypeLabel(String bizType) {
        if (bizType == null) return "未知";
        return switch (bizType) {
            case "quiz" -> "答题奖励";
            case "task" -> "任务奖励";
            case "shop" -> "兑换消耗";
            case "admin" -> "系统调整";
            default -> bizType;
        };
    }
}
