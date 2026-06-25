package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.PointExchangeOrder;
import com.gdou.marine.entity.PointShopItem;
import com.gdou.marine.entity.PointTransaction;
import com.gdou.marine.entity.UserPointAccount;
import com.gdou.marine.mapper.PointExchangeOrderMapper;
import com.gdou.marine.entity.UserBadge;
import com.gdou.marine.entity.LearningTask;
import com.gdou.marine.entity.UserTaskRecord;
import com.gdou.marine.mapper.LearningTaskMapper;
import com.gdou.marine.mapper.PointShopItemMapper;
import com.gdou.marine.mapper.PointTransactionMapper;
import com.gdou.marine.mapper.UserBadgeMapper;
import com.gdou.marine.mapper.UserTaskRecordMapper;
import com.gdou.marine.service.TaskProgressService;
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

    @Autowired
    private UserBadgeMapper userBadgeMapper;

    @Autowired
    private UserTaskRecordMapper userTaskRecordMapper;

    @Autowired
    private TaskProgressService taskProgressService;

    @Autowired
    private LearningTaskMapper learningTaskMapper;

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

    /** 已拥有的头像框编码列表 */
    @GetMapping("/owned-frames")
    public Map<String, Object> getOwnedFrames(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查所有 avatar_frame 类型的商品
            List<PointShopItem> frameItems = pointShopItemMapper.selectList(
                    new LambdaQueryWrapper<PointShopItem>()
                            .eq(PointShopItem::getItemType, "avatar_frame")
                            .eq(PointShopItem::getStatus, (byte) 1));

            if (frameItems.isEmpty()) {
                result.put("success", true);
                result.put("data", List.of("default"));
                return result;
            }

            // 用户已兑换的商品 ID 集合
            Set<Long> ownedItemIds = pointExchangeOrderMapper.selectList(
                    new LambdaQueryWrapper<PointExchangeOrder>()
                            .eq(PointExchangeOrder::getUserId, userId)
                            .select(PointExchangeOrder::getItemId))
                    .stream()
                    .map(PointExchangeOrder::getItemId)
                    .collect(Collectors.toSet());

            // 从 description 提取 frame_code，筛选出已拥有的
            List<String> ownedFrames = new ArrayList<>();
            ownedFrames.add("default"); // 默认框永远可用
            for (PointShopItem item : frameItems) {
                if (item.getDescription() != null && item.getDescription().startsWith("frame_code:")
                        && ownedItemIds.contains(item.getId())) {
                    ownedFrames.add(item.getDescription().replace("frame_code:", ""));
                }
            }

            result.put("success", true);
            result.put("data", ownedFrames);
        } catch (Exception e) {
            log.error("获取已拥有头像框失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 是否拥有自定义称号（检查是否已购买 title_custom 商品） */
    @GetMapping("/owned-title")
    public Map<String, Object> getOwnedTitle(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            // 查找 title_custom 商品
            PointShopItem titleItem = pointShopItemMapper.selectOne(
                    new LambdaQueryWrapper<PointShopItem>()
                            .eq(PointShopItem::getDescription, "title_custom")
                            .eq(PointShopItem::getStatus, (byte) 1));
            boolean owned = false;
            if (titleItem != null) {
                long count = pointExchangeOrderMapper.selectCount(
                        new LambdaQueryWrapper<PointExchangeOrder>()
                                .eq(PointExchangeOrder::getUserId, userId)
                                .eq(PointExchangeOrder::getItemId, titleItem.getId()));
                owned = count > 0;
            }
            result.put("success", true);
            result.put("data", owned);
        } catch (Exception e) {
            log.error("检查称号购买状态失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 兑换商品 */
    @Log(module = "积分商城", description = "兑换商品")
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

            // 勋章类商品：一人只能买一次
            if ("badge".equals(item.getItemType())) {
                boolean exists = userBadgeMapper.selectCount(
                        new LambdaQueryWrapper<UserBadge>()
                                .eq(UserBadge::getUserId, userId)
                                .eq(UserBadge::getBadgeCode, "shop_" + itemId)) > 0;
                if (exists) {
                    result.put("success", false);
                    result.put("message", "已拥有该勋章，每人限购一次");
                    return result;
                }
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

            // 执行商品效果
            String extraMsg = applyItemEffect(userId, item);

            result.put("success", true);
            result.put("message", "兑换成功！" + extraMsg);
        } catch (Exception e) {
            log.error("兑换失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 按 item_type 执行商品效果 */
    private String applyItemEffect(Long userId, PointShopItem item) {
        String type = item.getItemType();

        // ⚡ 双倍经验卡 (id=5) — 当天答题正确数在等级计算时翻倍
        if (Long.valueOf(5).equals(item.getId())) {
            return "双倍经验已激活！今日答题等级经验翻倍";
        }

        // 🏅 隐藏勋章 → 直接颁发
        if ("badge".equals(type)) {
            boolean exists = userBadgeMapper.selectCount(
                    new LambdaQueryWrapper<UserBadge>()
                            .eq(UserBadge::getUserId, userId)
                            .eq(UserBadge::getBadgeCode, "shop_" + item.getId())) > 0;
            if (!exists) {
                UserBadge badge = new UserBadge();
                badge.setUserId(userId);
                badge.setBadgeCode("shop_" + item.getId());
                badge.setBadgeName(item.getName().replaceAll("[🏅🎫⚡💎📊🔄]", "").trim());
                badge.setDescription(item.getDescription());
                badge.setEarnedAt(java.time.LocalDateTime.now());
                userBadgeMapper.insert(badge);
                return "获得隐藏勋章「" + badge.getBadgeName() + "」";
            }
            return "已拥有该勋章";
        }

        // 🎫 任务快进卡 → 完成一个进行中的或未开始的任务
        if ("coupon".equals(type)) {
            // 1. 先找有记录但未完成的
            UserTaskRecord rec = userTaskRecordMapper.selectOne(
                    new LambdaQueryWrapper<UserTaskRecord>()
                            .eq(UserTaskRecord::getUserId, userId)
                            .eq(UserTaskRecord::getCompleted, 0)
                            .last("LIMIT 1"));
            // 2. 找不到 → 从未开始的任务中挑一个
            if (rec == null) {
                List<LearningTask> allTasks = learningTaskMapper.selectList(
                        new LambdaQueryWrapper<LearningTask>()
                                .eq(LearningTask::getStatus, (byte) 1));
                Set<Long> hasRecordIds = userTaskRecordMapper.selectList(
                        new LambdaQueryWrapper<UserTaskRecord>()
                                .eq(UserTaskRecord::getUserId, userId))
                        .stream().map(UserTaskRecord::getTaskId)
                        .collect(Collectors.toSet());
                LearningTask pick = allTasks.stream()
                        .filter(t -> !hasRecordIds.contains(t.getId()))
                        .findFirst().orElse(null);
                if (pick != null) {
                    rec = new UserTaskRecord();
                    rec.setUserId(userId);
                    rec.setTaskId(pick.getId());
                    rec.setTaskDate(java.time.LocalDate.now());
                    rec.setProgressValue(0);
                    rec.setCompleted((byte) 0);
                    rec.setRewardClaimed((byte) 0);
                    rec.setCreatedAt(java.time.LocalDateTime.now());
                }
            }
            if (rec != null) {
                LearningTask task = rec.getId() != null
                        ? learningTaskMapper.selectById(rec.getTaskId()) : null;
                rec.setProgressValue(task != null ? task.getTargetValue() : 1);
                rec.setCompleted((byte) 1);
                rec.setCompletedAt(java.time.LocalDateTime.now());
                rec.setUpdatedAt(java.time.LocalDateTime.now());
                if (rec.getId() == null) {
                    userTaskRecordMapper.insert(rec);
                } else {
                    userTaskRecordMapper.updateById(rec);
                }
                return "已快进完成「" + (task != null ? task.getTitle() : "未知任务") + "」，快去领取奖励！";
            }
            return "暂无可快进的任务，所有任务都已完成";
        }

        // 💎 盲盒 (id=1) → 随机返还 50%~200%
        if ("virtual_item".equals(type) && Long.valueOf(1).equals(item.getId())) {
            double rate = 0.5 + Math.random() * 1.5;
            int refund = (int) Math.round(item.getPointsPrice() * rate);
            userPointAccountService.earnPoints(userId, refund, "shop", item.getId(),
                    "积分盲盒返还 (" + Math.round(rate * 100) + "%)");
            return "盲盒返还 " + refund + " 积分（" + Math.round(rate * 100) + "%）";
        }

        // 📊 学习报告 (id=4) → 纯前端展示，后端返回提示
        if ("virtual_item".equals(type) && Long.valueOf(4).equals(item.getId())) {
            return "学习报告已生成，可在个人中心查看";
        }

        return "已消耗" + item.getPointsPrice() + "积分";
    }

    // ════════════════════════════════════════════════
    // 以下为 B 端管理接口（ADMIN / MANAGER）
    // ════════════════════════════════════════════════

    /** 管理端-商品列表（含已下架） */
    @GetMapping("/admin/items")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> adminListItems() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<PointShopItem> items = pointShopItemMapper.selectList(
                    new LambdaQueryWrapper<PointShopItem>()
                            .orderByAsc(PointShopItem::getId));
            List<Map<String, Object>> list = items.stream().map(i -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", i.getId());
                m.put("name", i.getName());
                m.put("description", i.getDescription());
                m.put("itemType", i.getItemType());
                m.put("pointsPrice", i.getPointsPrice());
                m.put("stock", i.getStock());
                m.put("status", i.getStatus());
                m.put("createdAt", i.getCreatedAt() != null
                        ? i.getCreatedAt().toString().replace("T", " ") : "");
                return m;
            }).collect(Collectors.toList());
            result.put("success", true);
            result.put("data", list);
        } catch (Exception e) {
            log.error("管理端获取商品失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 管理端-更新商品 */
    @PutMapping("/admin/items/{id}")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> adminUpdateItem(@PathVariable Long id,
                                                @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            PointShopItem item = pointShopItemMapper.selectById(id);
            if (item == null) {
                result.put("success", false);
                result.put("message", "商品不存在");
                return result;
            }
            if (body.containsKey("name")) item.setName((String) body.get("name"));
            if (body.containsKey("description")) item.setDescription((String) body.get("description"));
            if (body.containsKey("pointsPrice")) item.setPointsPrice((Integer) body.get("pointsPrice"));
            if (body.containsKey("stock")) item.setStock((Integer) body.get("stock"));
            if (body.containsKey("status")) item.setStatus(((Integer) body.get("status")).byteValue());
            pointShopItemMapper.updateById(item);
            result.put("success", true);
            result.put("message", "商品已更新");
        } catch (Exception e) {
            log.error("管理端更新商品失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /** 管理端-新增商品 */
    @PostMapping("/admin/items")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> adminAddItem(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            PointShopItem item = new PointShopItem();
            item.setName((String) body.get("name"));
            item.setDescription((String) body.getOrDefault("description", ""));
            item.setItemType((String) body.getOrDefault("itemType", "virtual_item"));
            item.setPointsPrice((Integer) body.get("pointsPrice"));
            item.setStock((Integer) body.getOrDefault("stock", null));
            item.setStatus(((Integer) body.getOrDefault("status", 1)).byteValue());
            item.setCreatedAt(java.time.LocalDateTime.now());
            pointShopItemMapper.insert(item);
            result.put("success", true);
            result.put("message", "商品已添加");
            result.put("data", Map.of("id", item.getId()));
        } catch (Exception e) {
            log.error("管理端新增商品失败", e);
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
