package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.UserBookmark;
import com.gdou.marine.mapper.UserBookmarkMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liangguize2024
 * @version 1.0
 * @date 2026/6/23
 * @Description C 端用户收藏 Controller
 */
@RestController
@RequestMapping("/bookmark")
public class UserBookmarkController {

    private static final Logger log = LoggerFactory.getLogger(UserBookmarkController.class);

    @Autowired
    private UserBookmarkMapper userBookmarkMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.gdou.marine.mapper.MediaAssetMapper mediaAssetMapper;

    @Autowired
    private com.gdou.marine.service.TaskProgressService taskProgressService;

    /**
     * 取消收藏
     * DELETE /bookmark/{targetType}/{targetId}
     */
    @Log(module = "用户收藏", description = "取消收藏")
    @DeleteMapping("/{targetType}/{targetId}")
    public Map<String, Object> removeBookmark(@PathVariable String targetType,
                                              @PathVariable Long targetId,
                                              Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            int deleted = userBookmarkMapper.delete(
                    new LambdaQueryWrapper<UserBookmark>()
                            .eq(UserBookmark::getUserId, userId)
                            .eq(UserBookmark::getTargetType, targetType)
                            .eq(UserBookmark::getTargetId, targetId));

            if (deleted > 0) {
                result.put("success", true);
                result.put("message", "已取消收藏");
            } else {
                result.put("success", false);
                result.put("message", "收藏记录不存在");
            }
        } catch (Exception e) {
            log.error("取消收藏失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 添加收藏
     * POST /bookmark/{targetType}/{targetId}
     */
    @Log(module = "用户收藏", description = "添加收藏")
    @PostMapping("/{targetType}/{targetId}")
    public Map<String, Object> addBookmark(@PathVariable String targetType,
                                           @PathVariable Long targetId,
                                           Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 幂等：已收藏则不重复添加
            Long count = userBookmarkMapper.selectCount(
                    new LambdaQueryWrapper<UserBookmark>()
                            .eq(UserBookmark::getUserId, userId)
                            .eq(UserBookmark::getTargetType, targetType)
                            .eq(UserBookmark::getTargetId, targetId));
            if (count > 0) {
                result.put("success", true);
                result.put("message", "已收藏");
                return result;
            }

            UserBookmark bookmark = new UserBookmark();
            bookmark.setUserId(userId);
            bookmark.setTargetType(targetType);
            bookmark.setTargetId(targetId);
            userBookmarkMapper.insert(bookmark);

            // 推进每日任务「收藏物种或知识」进度
            taskProgressService.incrementProgress(userId, "bookmark");

            result.put("success", true);
            result.put("message", "收藏成功");
        } catch (Exception e) {
            log.error("收藏失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取当前用户的收藏列表（按类型分组，JOIN 业务表获取标题和缩略图）
     * GET /bookmark/list
     */
    @GetMapping("/list")
    public Map<String, Object> listBookmarks(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查询用户所有收藏
            List<UserBookmark> bookmarks = userBookmarkMapper.selectList(
                    new LambdaQueryWrapper<UserBookmark>()
                            .eq(UserBookmark::getUserId, userId)
                            .orderByDesc(UserBookmark::getCreatedAt));

            // 按 target_type 分组
            Map<String, List<UserBookmark>> grouped = bookmarks.stream()
                    .collect(Collectors.groupingBy(UserBookmark::getTargetType));

            // 用 JdbcTemplate 做 JOIN 查询获取标题和缩略图
            Map<String, List<Map<String, Object>>> data = new LinkedHashMap<>();

            // species 类型 → JOIN marine_species
            List<Map<String, Object>> speciesList = new ArrayList<>();
            if (grouped.containsKey("species")) {
                List<Long> ids = grouped.get("species").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String sql = "SELECT id, chinese_name AS title, image_url AS thumbnail FROM marine_species WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("species")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        speciesList.add(item);
                    }
                }
            }
            data.put("species", speciesList);

            // ecosystem 类型 → JOIN marine_ecosystem
            List<Map<String, Object>> ecosystemList = new ArrayList<>();
            if (grouped.containsKey("ecosystem")) {
                List<Long> ids = grouped.get("ecosystem").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String sql = "SELECT id, name AS title, image_url AS thumbnail FROM marine_ecosystem WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("ecosystem")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        ecosystemList.add(item);
                    }
                }
            }
            data.put("ecosystem", ecosystemList);

            // kb_document 类型 → JOIN kb_document
            List<Map<String, Object>> kbList = new ArrayList<>();
            if (grouped.containsKey("kb_document")) {
                List<Long> ids = grouped.get("kb_document").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String sql = "SELECT id, title FROM kb_document WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("kb_document")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        kbList.add(item);
                    }
                }
            }
            data.put("kb_document", kbList);

            // quiz_question 类型 → JOIN quiz_question
            List<Map<String, Object>> quizList = new ArrayList<>();
            if (grouped.containsKey("quiz_question")) {
                List<Long> ids = grouped.get("quiz_question").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String sql = "SELECT id, LEFT(stem, 30) AS title FROM quiz_question WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("quiz_question")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        quizList.add(item);
                    }
                }
            }
            data.put("quiz_question", quizList);

            // user_observation 类型 → JOIN user_observation 和 media_asset
            List<Map<String, Object>> obsList = new ArrayList<>();
            if (grouped.containsKey("user_observation")) {
                List<Long> ids = grouped.get("user_observation").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    String sql = "SELECT id, title, photo_media_id FROM user_observation WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    // 收集 media_id 查照片URL
                    Map<Long, String> mediaUrlMap = new HashMap<>();
                    List<Long> mids = rows.stream()
                            .filter(r -> r.get("photo_media_id") != null)
                            .map(r -> ((Number) r.get("photo_media_id")).longValue())
                            .distinct().collect(Collectors.toList());
                    if (!mids.isEmpty()) {
                        String mInClause = mids.stream().map(String::valueOf).collect(Collectors.joining(","));
                        List<Map<String, Object>> mediaRows = jdbcTemplate.queryForList(
                                "SELECT id, url FROM media_asset WHERE id IN (" + mInClause + ")");
                        for (Map<String, Object> mr : mediaRows) {
                            mediaUrlMap.put(((Number) mr.get("id")).longValue(), (String) mr.get("url"));
                        }
                    }

                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        Long oid = ((Number) row.get("id")).longValue();
                        String url = row.get("photo_media_id") != null
                                ? mediaUrlMap.getOrDefault(((Number) row.get("photo_media_id")).longValue(), "") : "";
                        row.put("thumbnail", url);
                        row.put("title", row.getOrDefault("title", "未知观察"));
                        rowMap.put(oid, row);
                    }
                    for (UserBookmark bm : grouped.get("user_observation")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        obsList.add(item);
                    }
                }
            }
            data.put("user_observation", obsList);

            result.put("success", true);
            result.put("data", data);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    private Map<String, Object> buildItem(UserBookmark bm, Map<String, Object> joinRow) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("bookmarkId", bm.getId());
        item.put("targetType", bm.getTargetType());
        item.put("targetId", bm.getTargetId());
        item.put("title", joinRow != null ? joinRow.getOrDefault("title", "未知") : "未知");
        item.put("thumbnail", joinRow != null ? joinRow.getOrDefault("thumbnail", "") : "");
        item.put("createdAt", bm.getCreatedAt() != null
                ? bm.getCreatedAt().toString().replace("T", " ") : "");
        return item;
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
