package com.gdou.marine.controller;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.service.HighlightBroadcastService;
import com.gdou.marine.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description 互动通知消息控制器
 */
@RestController
@RequestMapping("/notification")
public class NotificationController {
    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private HighlightBroadcastService highlightBroadcastService;

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    /**
     * 获取当前登录用户的未读通知数量
     * GET /notification/unread-count
     */
    @GetMapping("/unread-count")
    public Map<String, Object> getUnreadCount(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM system_notification WHERE receiver_id = ? AND is_read = 0",
                    Integer.class, userId);
            result.put("success", true);
            result.put("data", count != null ? count : 0);
        } catch (Exception e) {
            log.error("获取未读通知数失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 分页查询当前用户的通知列表（联查发送者头像与昵称）
     * GET /notification/list?pageNum=1&pageSize=5
     */
    @GetMapping("/list")
    public Map<String, Object> getList(@RequestParam(defaultValue = "1") int pageNum,
                                       @RequestParam(defaultValue = "5") int pageSize,
                                       Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            int offset = (pageNum - 1) * pageSize;
            String sql = """
                SELECT n.*, u.username AS sender_name, u.real_name AS sender_real_name, u.avatar_url AS sender_avatar
                FROM system_notification n
                LEFT JOIN app_user u ON n.sender_id = u.id
                WHERE n.receiver_id = ?
                ORDER BY n.created_at DESC
                LIMIT ? OFFSET ?
                """;
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, userId, pageSize, offset);

            List<Map<String, Object>> records = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", row.get("id"));
                item.put("receiverId", row.get("receiver_id"));
                item.put("senderId", row.get("sender_id"));
                item.put("senderName", row.get("sender_name"));
                item.put("senderRealName", row.get("sender_real_name"));
                item.put("senderAvatar", row.get("sender_avatar"));
                item.put("type", row.get("type"));
                item.put("targetId", row.get("target_id"));
                item.put("postId", row.get("post_id"));
                item.put("content", row.get("content"));
                item.put("isRead", row.get("is_read"));
                item.put("createdAt", row.get("created_at") != null
                        ? row.get("created_at").toString().replace("T", " ") : "");
                records.add(item);
            }

            result.put("success", true);
            result.put("data", Map.of("records", records));
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 标记单条通知为已读
     * PUT /notification/{id}/read
     */
    @Log(module = "通知管理", description = "阅读通知")
    @PutMapping("/{id}/read")
    public Map<String, Object> markAsRead(@PathVariable Long id, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            jdbcTemplate.update("UPDATE system_notification SET is_read = 1 WHERE id = ? AND receiver_id = ?", id, userId);
            result.put("success", true);
            result.put("message", "已标记已读");
        } catch (Exception e) {
            log.error("标记通知已读失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 一键全部标记已读
     * PUT /notification/read-all
     */
    @Log(module = "通知管理", description = "一键全部标记已读")
    @PutMapping("/read-all")
    public Map<String, Object> readAll(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }
            jdbcTemplate.update("UPDATE system_notification SET is_read = 1 WHERE receiver_id = ?", userId);
            result.put("success", true);
            result.put("message", "全部消息已标记已读");
        } catch (Exception e) {
            log.error("全部标记已读失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

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

    /**
     * 删除单条通知
     * DELETE /notification/{id}
     */
    @Log(module = "通知管理", description = "删除单条通知")
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteNotification(@PathVariable Long id, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM system_notification WHERE id = ? AND receiver_id = ?", id, userId);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
        }
        return result;
    }

    /**
     * 清空当前用户所有通知
     * DELETE /notification/all
     */
    @Log(module = "通知管理", description = "清除所有通知")
    @DeleteMapping("/all")
    public Map<String, Object> clearAll(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId != null) {
                jdbcTemplate.update("DELETE FROM system_notification WHERE receiver_id = ?", userId);
            }
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
        }
        return result;
    }

    /* B端管理员：发布全站系统广播
     * POST /notification/admin/broadcast
     * body: { "content": "广播内容" }
     */
    @Log(module = "运营管理", description = "发布全站广播消息")
    @PostMapping("/admin/broadcast")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> sendBroadcast(@RequestBody Map<String, String> body, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long adminId = extractUserId(auth);
            String content = body.get("content");

            if (content == null || content.trim().isEmpty()) {
                result.put("success", false);
                result.put("message", "广播内容不能为空");
                return result;
            }

            // 查询所有活跃用户
            List<Long> activeUserIds = jdbcTemplate.queryForList(
                    "SELECT id FROM app_user WHERE status = 1", Long.class);

            // 逐条插入通知（雪花 ID 每行唯一）
            int rowsAffected = 0;
            for (Long receiverId : activeUserIds) {
                jdbcTemplate.update(
                        "INSERT INTO system_notification (id, receiver_id, sender_id, type, target_id, post_id, content) VALUES (?, ?, ?, 'broadcast', 0, 0, ?)",
                        snowflakeIdGenerator.nextId(), receiverId, adminId, content.trim());
                rowsAffected++;
            }

            result.put("success", true);
            result.put("message", "广播发送成功，覆盖 " + rowsAffected + " 名用户");
        } catch (Exception e) {
            log.error("发送全站广播失败", e);
            result.put("success", false);
            result.put("message", "发送失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * B端管理员：手动立即触发一次【社区高光时刻】广播
     * POST /notification/admin/trigger-highlight
     */
    @Log(module = "运营管理", description = "手动触发社区高光广播")
    @PostMapping("/admin/trigger-highlight")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> triggerHighlight() {
        Map<String, Object> result = new HashMap<>();
        try {
            // 异步执行，不要阻塞请求响应，因为 AI 生成需要时间
            new Thread(() -> highlightBroadcastService.generateAndSendHighlight()).start();

            result.put("success", true);
            result.put("message", "触发指令已发送，AI正在提炼文案，稍后将下发至全站");
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "触发失败：" + e.getMessage());
        }
        return result;
    }
}
