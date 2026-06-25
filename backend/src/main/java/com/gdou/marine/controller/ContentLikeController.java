package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.ContentLike;
import com.gdou.marine.mapper.ContentLikeMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * @author so-ai-edu
 * @version 1.0
 * @date 2026/6/24
 * @Description 点赞 Controller
 */
@RestController
@RequestMapping("/like")
public class ContentLikeController {

    private static final Logger log = LoggerFactory.getLogger(ContentLikeController.class);

    @Autowired
    private ContentLikeMapper contentLikeMapper;

    /**
     * 切换点赞状态（已赞→取消，未赞→点赞）
     * POST /like/toggle
     */
    @Log(module = "社区模块", description = "切换点赞状态")
    @PostMapping("/toggle")
    public Map<String, Object> toggleLike(@RequestBody Map<String, Object> body,
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

            if (targetType == null || targetId == null) {
                result.put("success", false);
                result.put("message", "参数不完整");
                return result;
            }

            // 查找是否已点赞
            ContentLike existing = contentLikeMapper.selectOne(
                    new LambdaQueryWrapper<ContentLike>()
                            .eq(ContentLike::getUserId, userId)
                            .eq(ContentLike::getTargetType, targetType)
                            .eq(ContentLike::getTargetId, targetId));

            boolean liked;
            if (existing != null) {
                // 已点赞 → 取消点赞
                contentLikeMapper.deleteById(existing.getId());
                liked = false;
            } else {
                // 未点赞 → 点赞
                ContentLike like = new ContentLike();
                like.setUserId(userId);
                like.setTargetType(targetType);
                like.setTargetId(targetId);
                contentLikeMapper.insert(like);
                liked = true;
            }

            // 统计点赞数
            long count = contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<ContentLike>()
                            .eq(ContentLike::getTargetType, targetType)
                            .eq(ContentLike::getTargetId, targetId));

            result.put("success", true);
            result.put("data", Map.of(
                    "liked", liked,
                    "count", count
            ));
        } catch (Exception e) {
            log.error("切换点赞失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取点赞状态和数量
     * GET /like/status/{targetType}/{targetId}
     */
    @GetMapping("/status/{targetType}/{targetId}")
    public Map<String, Object> getLikeStatus(@PathVariable String targetType,
                                             @PathVariable Long targetId,
                                             Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);

            // 总数
            long count = contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<ContentLike>()
                            .eq(ContentLike::getTargetType, targetType)
                            .eq(ContentLike::getTargetId, targetId));

            // 当前用户是否点赞
            boolean liked = false;
            if (userId != null) {
                Long likedCount = contentLikeMapper.selectCount(
                        new LambdaQueryWrapper<ContentLike>()
                                .eq(ContentLike::getUserId, userId)
                                .eq(ContentLike::getTargetType, targetType)
                                .eq(ContentLike::getTargetId, targetId));
                liked = likedCount > 0;
            }

            result.put("success", true);
            result.put("data", Map.of(
                    "liked", liked,
                    "count", count
            ));
        } catch (Exception e) {
            log.error("获取点赞状态失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量获取点赞状态（用于帖子列表）
     * POST /like/batch-status
     */
    @PostMapping("/batch-status")
    public Map<String, Object> batchLikeStatus(@RequestBody Map<String, Object> body,
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
            @SuppressWarnings("unchecked")
            var targetIds = (java.util.List<Integer>) body.get("targetIds");

            if (targetType == null || targetIds == null || targetIds.isEmpty()) {
                result.put("success", true);
                result.put("data", Map.of());
                return result;
            }

            // 查当前用户对这些目标的点赞
            var wrapper = new LambdaQueryWrapper<ContentLike>()
                    .eq(ContentLike::getUserId, userId)
                    .eq(ContentLike::getTargetType, targetType)
                    .in(ContentLike::getTargetId, targetIds);

            java.util.Set<Long> likedIds = new java.util.HashSet<>();
            for (ContentLike like : contentLikeMapper.selectList(wrapper)) {
                likedIds.add(like.getTargetId());
            }

            // 查每个目标的点赞总数
            Map<Long, Long> countMap = new HashMap<>();
            for (Integer id : targetIds) {
                Long tid = id.longValue();
                long c = contentLikeMapper.selectCount(
                        new LambdaQueryWrapper<ContentLike>()
                                .eq(ContentLike::getTargetType, targetType)
                                .eq(ContentLike::getTargetId, tid));
                countMap.put(tid, c);
            }

            Map<String, Object> data = new HashMap<>();
            for (Integer id : targetIds) {
                Long tid = id.longValue();
                Map<String, Object> st = new HashMap<>();
                st.put("liked", likedIds.contains(tid));
                st.put("count", countMap.getOrDefault(tid, 0L));
                data.put(String.valueOf(tid), st);
            }

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("批量获取点赞状态失败", e);
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
}
