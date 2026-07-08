package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private ObjectMapper objectMapper;

    @Autowired
    private com.gdou.marine.mapper.MediaAssetMapper mediaAssetMapper;

    @Autowired
    private com.gdou.marine.service.TaskProgressService taskProgressService;

    @Autowired
    private com.gdou.marine.mapper.KbDocumentMapper kbDocumentMapper;

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
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 并发情况下可能触发唯一索引冲突，视为已收藏
            log.warn("收藏重复插入: userId={}, targetType={}, targetId={}", 
                    extractUserId(auth), targetType, targetId);
            result.put("success", true);
            result.put("message", "已收藏");
        } catch (Exception e) {
            log.error("收藏失败", e);
            result.put("success", false);
            result.put("message", "收藏失败：" + e.getMessage());
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
                    String sql = "SELECT id, name, description, typical_species, threats, protection_advice, image_url, status FROM marine_ecosystem WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        row.put("title", row.getOrDefault("name", "未知生态区"));
                        row.put("thumbnail", row.getOrDefault("image_url", ""));
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("ecosystem")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        if (rowMap.get(bm.getTargetId()) != null) {
                            item.putAll(rowMap.get(bm.getTargetId()));
                        }
                        ecosystemList.add(item);
                    }
                }
            }
            data.put("ecosystem", ecosystemList);

            // kb_document 类型 → JOIN kb_document（不包含species_id字段）
            List<Map<String, Object>> kbList = new ArrayList<>();
            if (grouped.containsKey("kb_document")) {
                List<Long> ids = grouped.get("kb_document").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    // 查询时不包含 species_id 字段（已从kb_document表移除）
                    String sql = "SELECT id, title, source_type, content FROM kb_document WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    for (UserBookmark bm : grouped.get("kb_document")) {
                        Map<String, Object> item = buildItem(bm, rowMap.get(bm.getTargetId()));
                        // 只添加 source_type、content 字段（不含species_id）
                        if (rowMap.get(bm.getTargetId()) != null) {
                            item.put("sourceType", rowMap.get(bm.getTargetId()).get("source_type"));
                            item.put("content", rowMap.get(bm.getTargetId()).get("content"));
                        }
                        kbList.add(item);
                    }
                }
            }
            data.put("kb_document", kbList);

            // quiz_question 类型 → JOIN quiz_question + 关联知识库
            List<Map<String, Object>> quizList = new ArrayList<>();
            if (grouped.containsKey("quiz_question")) {
                List<Long> ids = grouped.get("quiz_question").stream()
                        .map(UserBookmark::getTargetId).collect(Collectors.toList());
                if (!ids.isEmpty()) {
                    String inClause = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
                    // 查询题目基本信息（包含 species_id 和 source_document_id）
                    String sql = "SELECT id, stem, stem AS title, question_type, difficulty, species_id, source_document_id, options_json, answer_json, explanation FROM quiz_question WHERE id IN (" + inClause + ")";
                    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);

                    // 批量收集需要查询的物种ID和文档ID
                    Set<Long> speciesIds = new HashSet<>();
                    Set<Long> docIds = new HashSet<>();
                    for (Map<String, Object> row : rows) {
                        if (row.get("species_id") != null) {
                            speciesIds.add(((Number) row.get("species_id")).longValue());
                        }
                        if (row.get("source_document_id") != null) {
                            docIds.add(((Number) row.get("source_document_id")).longValue());
                        }
                    }

                    // 批量查询物种详情（作为知识库 - 物种出题时使用）
                    Map<Long, Map<String, Object>> speciesMap = new HashMap<>();
                    if (!speciesIds.isEmpty()) {
                        String speciesInClause = speciesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        List<Map<String, Object>> speciesRows = jdbcTemplate.queryForList(
                                "SELECT id, chinese_name AS title, scientific_name, conservation_status, image_url, fun_fact " +
                                        "FROM marine_species WHERE id IN (" + speciesInClause + ")");
                        for (Map<String, Object> sp : speciesRows) {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.putAll(sp);
                            info.put("type", "species");
                            info.put("id", sp.get("id"));
                            speciesMap.put(((Number) sp.get("id")).longValue(), info);
                        }
                    }

                    // 批量查询知识库文档（作为知识库 - RAG出题时使用）
                    Map<Long, Map<String, Object>> docMap = new HashMap<>();
                    if (!docIds.isEmpty()) {
                        String docInClause = docIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                        List<Map<String, Object>> docRows = jdbcTemplate.queryForList(
                                "SELECT id, title, source_type, content FROM kb_document WHERE id IN (" + docInClause + ")");
                        for (Map<String, Object> d : docRows) {
                            Map<String, Object> info = new LinkedHashMap<>();
                            info.putAll(d);
                            info.put("type", "document");
                            info.put("id", d.get("id"));
                            docMap.put(((Number) d.get("id")).longValue(), info);
                        }
                    }

                    // 组装结果：每个题目关联对应的知识库
                    Map<Long, Map<String, Object>> rowMap = new HashMap<>();
                    for (Map<String, Object> row : rows) {
                        rowMap.put(((Number) row.get("id")).longValue(), row);
                    }
                    
                    for (UserBookmark bm : grouped.get("quiz_question")) {
                        Map<String, Object> row = rowMap.get(bm.getTargetId());
                        Map<String, Object> item = buildItem(bm, row);
                        
                        if (row != null) {
                            item.put("questionType", row.get("question_type"));
                            item.put("difficulty", row.get("difficulty"));
                            item.put("speciesId", row.get("species_id"));
                            item.put("sourceDocumentId", row.get("source_document_id"));
                            item.put("optionsJson", row.get("options_json"));
                            item.put("correctAnswer", parseAnswerJson((String) row.get("answer_json")));
                            item.put("explanation", row.get("explanation"));

                            // 根据题目来源，关联不同的知识库
                            Long speciesId = row.get("species_id") != null ? ((Number) row.get("species_id")).longValue() : null;
                            Long sourceDocId = row.get("source_document_id") != null ? ((Number) row.get("source_document_id")).longValue() : null;

                            if (speciesId != null && speciesMap.containsKey(speciesId)) {
                                // ✅ 物种出题：关联该物种详情作为知识库
                                item.put("relatedKnowledgeBase", speciesMap.get(speciesId));
                                item.put("knowledgeBaseType", "species");
                            } else if (sourceDocId != null && docMap.containsKey(sourceDocId)) {
                                // ✅ RAG知识库出题：关联该文档作为知识库
                                item.put("relatedKnowledgeBase", docMap.get(sourceDocId));
                                item.put("knowledgeBaseType", "document");
                            }
                        }
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

    /**
     * 根据物种ID查询该物种详情（用于答题页收藏知识库功能）
     * 物种出题时：将物种本身作为"知识库"进行收藏
     * GET /bookmark/kb-by-species/{speciesId}
     */
    @GetMapping("/kb-by-species/{speciesId}")
    public Map<String, Object> getKbBySpeciesId(@PathVariable Long speciesId,
                                                 Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 查询物种详情（物种本身就是知识库）
            String sql = "SELECT id, chinese_name AS title, scientific_name, alias_names, " +
                    "kingdom, phylum, class_name, order_name, family_name, genus_name, " +
                    "conservation_status, habitat, distribution_area, morphology_desc, " +
                    "habit_desc, fun_fact, image_url " +
                    "FROM marine_species WHERE id = ? AND status = 1";

            List<Map<String, Object>> speciesRows = jdbcTemplate.queryForList(sql, speciesId);

            if (speciesRows.isEmpty()) {
                result.put("success", false);
                result.put("message", "物种不存在或已下架");
                return result;
            }

            // 将物种信息包装为知识库格式
            Map<String, Object> speciesInfo = speciesRows.get(0);
            speciesInfo.put("type", "species");
            speciesInfo.put("knowledgeBaseType", "species");

            result.put("success", true);
            result.put("data", List.of(speciesInfo));
            log.info("成功查询物种知识库: speciesId={}, name={}", speciesId, speciesInfo.get("title"));
        } catch (Exception e) {
            log.error("根据物种ID查询知识库失败, speciesId={}", speciesId, e);
            result.put("success", false);
            result.put("message", "查询知识库失败: " + e.getMessage());
        }
        return result;
    }

    /**
     * 根据文档ID获取知识库详情（用于题目无speciesId时，通过来源文档ID直接收藏）
     * GET /bookmark/kb-by-doc/{documentId}
     */
    @GetMapping("/kb-by-doc/{documentId}")
    public Map<String, Object> getKbByDocumentId(@PathVariable Long documentId,
                                                   Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            com.gdou.marine.entity.KbDocument doc = kbDocumentMapper.selectById(documentId);
            if (doc == null || doc.getStatus() == null || doc.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "知识库文档不存在或未发布");
                return result;
            }

            Map<String, Object> docMap = new HashMap<>();
            docMap.put("id", doc.getId());
            docMap.put("title", doc.getTitle());
            docMap.put("sourceType", doc.getSourceType());
            docMap.put("status", doc.getStatus());
            
            result.put("success", true);
            result.put("data", List.of(docMap));
        } catch (Exception e) {
            log.error("根据文档ID查询知识库失败, documentId={}", documentId, e);
            result.put("success", false);
            result.put("message", "查询知识库失败: " + e.getMessage());
        }
        return result;
    }

    private String parseAnswerJson(String answerJson) {
        if (answerJson == null || answerJson.isEmpty()) return "";
        String trimmed = answerJson.trim();
        try {
            if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                List<String> arr = objectMapper.readValue(trimmed, List.class);
                return String.join(",", arr);
            }
        } catch (Exception ignored) {
        }
        return trimmed;
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