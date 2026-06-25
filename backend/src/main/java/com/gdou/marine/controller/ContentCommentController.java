package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.ContentComment;
import com.gdou.marine.mapper.ContentCommentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author so-ai-edu
 * @version 1.0
 * @date 2026/6/24
 * @Description 评论 Controller
 */
@RestController
@RequestMapping("/comment")
public class ContentCommentController {

    private static final Logger log = LoggerFactory.getLogger(ContentCommentController.class);

    @Autowired
    private ContentCommentMapper contentCommentMapper;

    @Autowired
    private com.gdou.marine.mapper.ContentLikeMapper contentLikeMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * 获取某个目标的评论列表
     * GET /comment/{targetType}/{targetId}?sort=latest|hot
     */
    @GetMapping("/{targetType}/{targetId}")
    public Map<String, Object> getComments(@PathVariable String targetType,
                                           @PathVariable Long targetId,
                                           @RequestParam(defaultValue = "latest") String sort,
                                           Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            List<ContentComment> comments = contentCommentMapper.selectList(
                    new LambdaQueryWrapper<ContentComment>()
                            .eq(ContentComment::getTargetType, targetType)
                            .eq(ContentComment::getTargetId, targetId)
                            .eq(ContentComment::getStatus, (byte) 1)
                            .eq(ContentComment::getParentId, 0L)
                            .orderByAsc(ContentComment::getCreatedAt));

            if (comments.isEmpty()) {
                result.put("success", true);
                result.put("data", Collections.emptyList());
                return result;
            }

            // 收集所有评论的用户ID
            Set<Long> userIds = comments.stream()
                    .map(ContentComment::getUserId)
                    .collect(Collectors.toSet());

            // 批量查用户信息
            Map<Long, Map<String, Object>> userInfoMap = queryUserInfo(userIds);

            // 批量查用户的最近一枚徽章（称号）
            Map<Long, String> userBadgeMap = queryUserBadges(userIds);

            // 批量查点赞数据
            Set<Long> commentIds = comments.stream().map(ContentComment::getId).collect(Collectors.toSet());
            Map<Long, Long> likeCountMap = batchQueryLikeCount("comment", commentIds);
            Set<Long> likedByMe = currentUserId != null
                    ? batchQueryUserLikes(currentUserId, "comment", commentIds)
                    : Collections.emptySet();

            List<Map<String, Object>> records = comments.stream().map(c -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", c.getId());
                item.put("userId", c.getUserId());
                item.put("targetType", c.getTargetType());
                item.put("targetId", c.getTargetId());
                item.put("parentId", c.getParentId());
                item.put("content", c.getContent());
                item.put("createdAt", c.getCreatedAt() != null
                        ? c.getCreatedAt().toString().replace("T", " ") : "");

                // 用户信息
                Map<String, Object> userInfo = userInfoMap.getOrDefault(c.getUserId(), new LinkedHashMap<>());
                item.put("username", userInfo.getOrDefault("username", "未知用户"));
                item.put("avatarUrl", userInfo.getOrDefault("avatar_url", ""));
                item.put("avatarFrame", userInfo.getOrDefault("avatar_frame", "default"));
                item.put("title", commentUserTitle(userInfo, userBadgeMap.getOrDefault(c.getUserId(), "")));

                // 点赞数据
                item.put("liked", likedByMe.contains(c.getId()));
                item.put("likeCount", likeCountMap.getOrDefault(c.getId(), 0L));

                // 是否是当前用户的评论
                item.put("isOwner", currentUserId != null && currentUserId.equals(c.getUserId()));

                // 子评论计数
                Long childCount = contentCommentMapper.selectCount(
                        new LambdaQueryWrapper<ContentComment>()
                                .eq(ContentComment::getParentId, c.getId())
                                .eq(ContentComment::getStatus, (byte) 1));
                item.put("replyCount", childCount);

                // 热度分（用于前端排序）：点赞*2 + 回复*3
                long heat = likeCountMap.getOrDefault(c.getId(), 0L) * 2 + childCount * 3;
                item.put("heat", heat);

                return item;
            }).collect(Collectors.toList());

            // 按热度排序
            if ("hot".equalsIgnoreCase(sort)) {
                records.sort((a, b) -> Long.compare(
                        ((Number) b.get("heat")).longValue(),
                        ((Number) a.get("heat")).longValue()));
            }

            result.put("success", true);
            result.put("data", records);
        } catch (Exception e) {
            log.error("获取评论列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取子评论（回复）
     * GET /comment/{parentId}/replies
     */
    @GetMapping("/{parentId}/replies")
    public Map<String, Object> getReplies(@PathVariable Long parentId,
                                          Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            List<ContentComment> replies = contentCommentMapper.selectList(
                    new LambdaQueryWrapper<ContentComment>()
                            .eq(ContentComment::getParentId, parentId)
                            .eq(ContentComment::getStatus, (byte) 1)
                            .orderByAsc(ContentComment::getCreatedAt));

            if (replies.isEmpty()) {
                result.put("success", true);
                result.put("data", Collections.emptyList());
                return result;
            }

            Set<Long> userIds = replies.stream()
                    .map(ContentComment::getUserId)
                    .collect(Collectors.toSet());
            Map<Long, Map<String, Object>> userInfoMap = queryUserInfo(userIds);
            Map<Long, String> userBadgeMap = queryUserBadges(userIds);

            // 批量查点赞数据
            Set<Long> replyIds = replies.stream().map(ContentComment::getId).collect(Collectors.toSet());
            Map<Long, Long> likeCountMap = batchQueryLikeCount("comment", replyIds);
            Set<Long> likedByMe = currentUserId != null
                    ? batchQueryUserLikes(currentUserId, "comment", replyIds)
                    : Collections.emptySet();

            List<Map<String, Object>> records = replies.stream().map(r -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", r.getId());
                item.put("userId", r.getUserId());
                item.put("parentId", r.getParentId());
                item.put("content", r.getContent());
                item.put("createdAt", r.getCreatedAt() != null
                        ? r.getCreatedAt().toString().replace("T", " ") : "");

                Map<String, Object> userInfo = userInfoMap.getOrDefault(r.getUserId(), new LinkedHashMap<>());
                item.put("username", userInfo.getOrDefault("username", "未知用户"));
                item.put("avatarUrl", userInfo.getOrDefault("avatar_url", ""));
                item.put("avatarFrame", userInfo.getOrDefault("avatar_frame", "default"));
                item.put("title", commentUserTitle(userInfo, userBadgeMap.getOrDefault(r.getUserId(), "")));
                item.put("isOwner", currentUserId != null && currentUserId.equals(r.getUserId()));

                item.put("liked", likedByMe.contains(r.getId()));
                item.put("likeCount", likeCountMap.getOrDefault(r.getId(), 0L));

                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", records);
        } catch (Exception e) {
            log.error("获取回复列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 发布评论
     * POST /comment
     */
    @Log(module = "社区模块", description = "发表评论")
    @PostMapping
    public Map<String, Object> createComment(@RequestBody Map<String, Object> body,
                                             Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            String targetType = (String) body.get("targetType");
            Long targetId = body.get("targetId") != null
                    ? Long.valueOf(body.get("targetId").toString()) : null;
            String content = (String) body.get("content");

            if (targetType == null || targetId == null || content == null || content.isBlank()) {
                result.put("success", false);
                result.put("message", "参数不完整");
                return result;
            }
            if (content.length() > 1000) {
                result.put("success", false);
                result.put("message", "评论内容不能超过1000字");
                return result;
            }

            ContentComment comment = new ContentComment();
            comment.setUserId(userId);
            comment.setTargetType(targetType);
            comment.setTargetId(targetId);
            if (body.get("parentId") != null) {
                comment.setParentId(Long.valueOf(body.get("parentId").toString()));
            } else {
                comment.setParentId(0L);
            }
            comment.setContent(content.trim());
            comment.setStatus((byte) 1);

            contentCommentMapper.insert(comment);

            result.put("success", true);
            result.put("message", "评论成功");
            result.put("data", Map.of("id", comment.getId()));
        } catch (Exception e) {
            log.error("发布评论失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除自己的评论
     * DELETE /comment/{id}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteComment(@PathVariable Long id,
                                             Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            ContentComment comment = contentCommentMapper.selectById(id);
            if (comment == null) {
                result.put("success", false);
                result.put("message", "评论不存在");
                return result;
            }
            if (!comment.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "无权删除他人评论");
                return result;
            }

            contentCommentMapper.deleteById(id);

            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除评论失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量查询用户信息
     */
    private Map<Long, Map<String, Object>> queryUserInfo(Set<Long> userIds) {
        Map<Long, Map<String, Object>> map = new HashMap<>();
        if (userIds.isEmpty()) return map;
        String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, username, avatar_url, avatar_frame, user_title FROM app_user WHERE id IN (" + inClause + ")");
        for (Map<String, Object> row : rows) {
            map.put(((Number) row.get("id")).longValue(), row);
        }
        return map;
    }

    /**
     * 批量查询用户的最近一枚徽章（称号）
     */
    private Map<Long, String> queryUserBadges(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds.isEmpty()) return map;
        String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        // 取每个用户最近的一条 badge
        String sql = "SELECT u.user_id, u.badge_name FROM (" +
                " SELECT user_id, badge_name, " +
                " ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY earned_at DESC) AS rn" +
                " FROM user_badge WHERE user_id IN (" + inClause + ")" +
                ") u WHERE u.rn = 1";
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
            for (Map<String, Object> row : rows) {
                map.put(((Number) row.get("user_id")).longValue(),
                        (String) row.getOrDefault("badge_name", ""));
            }
        } catch (Exception e) {
            log.warn("查询用户徽章失败", e);
        }
        return map;
    }

    /**
     * 批量查询多个目标的点赞数
     */
    private Map<Long, Long> batchQueryLikeCount(String targetType, Set<Long> targetIds) {
        Map<Long, Long> map = new HashMap<>();
        if (targetIds.isEmpty()) return map;
        for (Long id : targetIds) {
            long count = contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                            .eq(com.gdou.marine.entity.ContentLike::getTargetType, targetType)
                            .eq(com.gdou.marine.entity.ContentLike::getTargetId, id));
            map.put(id, count);
        }
        return map;
    }

    /**
     * 批量查询当前用户对多个目标的点赞状态
     */
    private Set<Long> batchQueryUserLikes(Long userId, String targetType, Set<Long> targetIds) {
        Set<Long> liked = new HashSet<>();
        if (targetIds.isEmpty()) return liked;
        for (Long id : targetIds) {
            long count = contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                            .eq(com.gdou.marine.entity.ContentLike::getUserId, userId)
                            .eq(com.gdou.marine.entity.ContentLike::getTargetType, targetType)
                            .eq(com.gdou.marine.entity.ContentLike::getTargetId, id));
            if (count > 0) liked.add(id);
        }
        return liked;
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
     * 获取用户称号（与 UserObservationController 逻辑一致）：
     * - __none__ → 不显示任何称号
     * - 有值 → 显示 user_title
     * - 空值 → 降级显示勋章名称
     */
    private String commentUserTitle(Map<String, Object> userInfo, String fallbackBadge) {
        String raw = (String) userInfo.getOrDefault("user_title", "");
        if ("__none__".equals(raw)) return "";
        if (raw != null && !raw.isEmpty()) return raw;
        return fallbackBadge;
    }
}
