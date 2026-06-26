package com.gdou.marine.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
                SELECT n.*, u.username AS sender_name, u.avatar_url AS sender_avatar
                FROM system_notification n
                JOIN app_user u ON n.sender_id = u.id
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

}
