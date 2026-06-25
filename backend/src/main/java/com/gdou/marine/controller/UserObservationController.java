package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.MediaAsset;
import com.gdou.marine.entity.UserObservation;
import com.gdou.marine.mapper.MediaAssetMapper;
import com.gdou.marine.mapper.UserObservationMapper;
import com.gdou.marine.utils.QiniuUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liangguize2024
 * @version 1.0
 * @date 2026/6/23
 * @Description C 端用户观察 Controller
 */
@RestController
@RequestMapping("/observation")
public class UserObservationController {

    private static final Logger log = LoggerFactory.getLogger(UserObservationController.class);

    @Autowired
    private UserObservationMapper userObservationMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private com.gdou.marine.mapper.ContentLikeMapper contentLikeMapper;

    @Autowired
    private com.gdou.marine.mapper.ContentCommentMapper contentCommentMapper;

    @Autowired
    private com.gdou.marine.mapper.UserBookmarkMapper userBookmarkMapper;

    private static final long MAX_PHOTO_SIZE = 10 * 1024 * 1024; // 10MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    /**
     * 上传观察照片到七牛云
     * POST /observation/upload/photo
     */
    @PostMapping("/upload/photo")
    public Map<String, Object> uploadObservationPhoto(@RequestParam("file") MultipartFile file,
                                                        Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 校验文件
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择要上传的图片");
                return result;
            }
            if (file.getSize() > MAX_PHOTO_SIZE) {
                result.put("success", false);
                result.put("message", "图片大小不能超过 10MB");
                return result;
            }
            if (!isAllowedImage(file)) {
                result.put("success", false);
                result.put("message", "仅支持 JPG、PNG、GIF、WEBP 格式的图片");
                return result;
            }

            // 上传到七牛云
            String fileName = generatePhotoFileName(file);
            String photoUrl = QiniuUtils.upload2Qiniu(file.getBytes(), fileName);

            // 保存到 media_asset 表
            MediaAsset mediaAsset = new MediaAsset();
            mediaAsset.setProvider("qiniu");
            mediaAsset.setBucket(QiniuUtils.bucket);
            mediaAsset.setObjectKey(fileName);
            mediaAsset.setUrl(photoUrl);
            mediaAsset.setMimeType(file.getContentType());
            mediaAsset.setFileSize(file.getSize());
            mediaAsset.setOriginalName(file.getOriginalFilename());
            mediaAsset.setCreatedBy(userId);
            mediaAssetMapper.insert(mediaAsset);

            result.put("success", true);
            result.put("message", "图片上传成功");
            result.put("data", Map.of(
                    "mediaId", mediaAsset.getId(),
                    "photoUrl", photoUrl
            ));
        } catch (Exception e) {
            log.error("上传观察照片失败", e);
            result.put("success", false);
            result.put("message", "上传失败: " + e.getMessage());
        }
        return result;
    }

    private boolean isAllowedImage(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return false;
        }
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return false;
        }
        try (java.io.InputStream in = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = in.read(header);
            if (read < 4) return false;
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                return (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8;
            }
            if (ext.equals("png")) {
                return (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 && (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47;
            }
            if (ext.equals("gif")) {
                return header[0] == 0x47 && header[1] == 0x49
                    && header[2] == 0x46 && header[3] == 0x38;
            }
            if (ext.equals("webp")) {
                return (header[0] & 0xFF) == 0x52 && (header[1] & 0xFF) == 0x49
                    && (header[2] & 0xFF) == 0x46 && (header[3] & 0xFF) == 0x46;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private String generatePhotoFileName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf("."));
        }
        return "observation/photos/" + UUID.randomUUID().toString().replace("-", "") + extension;
    }

    /**
     * 发布观察记录
     * POST /observation
     */
    @PostMapping
    public Map<String, Object> createObservation(@RequestBody Map<String, Object> body,
                                                  Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            UserObservation obs = new UserObservation();
            obs.setUserId(userId);
            obs.setTitle((String) body.get("title"));
            obs.setDescription((String) body.get("description"));
            obs.setLocationName((String) body.get("locationName"));

            if (body.get("latitude") != null) {
                obs.setLatitude(new java.math.BigDecimal(body.get("latitude").toString()));
            }
            if (body.get("longitude") != null) {
                obs.setLongitude(new java.math.BigDecimal(body.get("longitude").toString()));
            }
            if (body.get("speciesId") != null) {
                obs.setSpeciesId(Long.valueOf(body.get("speciesId").toString()));
            }
            if (body.get("photoMediaId") != null) {
                obs.setPhotoMediaId(Long.valueOf(body.get("photoMediaId").toString()));
            }
            if (body.get("observedAt") != null) {
                String dateStr = body.get("observedAt").toString();
                obs.setObservedAt(LocalDateTime.parse(dateStr.replace(" ", "T")));
            }

            obs.setAiIdentified((byte) 0);
            obs.setStatus((byte) 2); // 默认待审核

            userObservationMapper.insert(obs);

            result.put("success", true);
            result.put("message", "观察记录提交成功，等待审核");
            result.put("data", Map.of("id", obs.getId()));
        } catch (Exception e) {
            log.error("发布观察记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取当前用户的观察记录列表
     * GET /observation/list
     */
    @GetMapping("/list")
    public Map<String, Object> listObservations(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            List<UserObservation> list = userObservationMapper.selectList(
                    new LambdaQueryWrapper<UserObservation>()
                            .eq(UserObservation::getUserId, userId)
                            .orderByDesc(UserObservation::getCreatedAt));

            // 收集需要 JOIN 的 ID
            List<Long> speciesIds = list.stream()
                    .filter(o -> o.getSpeciesId() != null)
                    .map(UserObservation::getSpeciesId).distinct().collect(Collectors.toList());
            List<Long> mediaIds = list.stream()
                    .filter(o -> o.getPhotoMediaId() != null)
                    .map(UserObservation::getPhotoMediaId).distinct().collect(Collectors.toList());

            // JOIN marine_species → chinese_name
            Map<Long, String> speciesNameMap = new HashMap<>();
            if (!speciesIds.isEmpty()) {
                String inClause = speciesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, chinese_name FROM marine_species WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    speciesNameMap.put(((Number) row.get("id")).longValue(), (String) row.get("chinese_name"));
                }
            }

            // JOIN media_asset → url (photoUrl)
            Map<Long, String> mediaUrlMap = new HashMap<>();
            if (!mediaIds.isEmpty()) {
                String inClause = mediaIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, url FROM media_asset WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    mediaUrlMap.put(((Number) row.get("id")).longValue(), (String) row.get("url"));
                }
            }

            List<Map<String, Object>> records = list.stream().map(obs -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", obs.getId());
                item.put("title", obs.getTitle());
                item.put("description", obs.getDescription());
                item.put("locationName", obs.getLocationName());
                item.put("observedAt", obs.getObservedAt() != null
                        ? obs.getObservedAt().toString().replace("T", " ") : "");
                item.put("speciesId", obs.getSpeciesId());
                item.put("aiIdentified", obs.getAiIdentified());
                item.put("aiConfidence", obs.getAiConfidence());
                item.put("status", obs.getStatus());
                // photoUrl: 通过 photo_media_id JOIN media_asset
                item.put("photoUrl", obs.getPhotoMediaId() != null
                        ? mediaUrlMap.getOrDefault(obs.getPhotoMediaId(), "") : "");
                // aiSpeciesName: 通过 species_id JOIN marine_species
                item.put("aiSpeciesName", obs.getSpeciesId() != null
                        ? speciesNameMap.getOrDefault(obs.getSpeciesId(), "") : "");
                item.put("createdAt", obs.getCreatedAt() != null
                        ? obs.getCreatedAt().toString().replace("T", " ") : "");
                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", records);
        } catch (Exception e) {
            log.error("获取观察记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取观察记录详情
     * GET /observation/{id}
     */
    @GetMapping("/{id}")
    public Map<String, Object> getObservationDetail(@PathVariable Long id,
                                                      Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            UserObservation obs = userObservationMapper.selectById(id);
            if (obs == null || !obs.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "记录不存在或无权访问");
                return result;
            }

            // JOIN marine_species → chinese_name
            String speciesName = "";
            if (obs.getSpeciesId() != null) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT chinese_name FROM marine_species WHERE id = " + obs.getSpeciesId());
                if (!rows.isEmpty()) {
                    speciesName = (String) rows.get(0).get("chinese_name");
                }
            }

            // JOIN media_asset → url
            String photoUrl = "";
            if (obs.getPhotoMediaId() != null) {
                MediaAsset media = mediaAssetMapper.selectById(obs.getPhotoMediaId());
                if (media != null) {
                    photoUrl = media.getUrl();
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", obs.getId());
            item.put("title", obs.getTitle());
            item.put("description", obs.getDescription());
            item.put("locationName", obs.getLocationName());
            item.put("observedAt", obs.getObservedAt() != null
                    ? obs.getObservedAt().toString().replace("T", " ") : "");
            item.put("latitude", obs.getLatitude());
            item.put("longitude", obs.getLongitude());
            item.put("speciesId", obs.getSpeciesId());
            item.put("speciesName", speciesName);
            item.put("aiIdentified", obs.getAiIdentified());
            item.put("aiConfidence", obs.getAiConfidence());
            item.put("status", obs.getStatus());
            item.put("photoMediaId", obs.getPhotoMediaId());
            item.put("photoUrl", photoUrl);
            item.put("createdAt", obs.getCreatedAt() != null
                    ? obs.getCreatedAt().toString().replace("T", " ") : "");

            result.put("success", true);
            result.put("data", item);
        } catch (Exception e) {
            log.error("获取观察记录详情失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 更新观察记录
     * PUT /observation/{id}
     */
    @PutMapping("/{id}")
    public Map<String, Object> updateObservation(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body,
                                                  Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            UserObservation obs = userObservationMapper.selectById(id);
            if (obs == null || !obs.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "记录不存在或无权修改");
                return result;
            }

            // 仅允许修改以下字段（待审核状态可改，已发布/已隐藏需重新审核）
            if (body.containsKey("title")) {
                obs.setTitle((String) body.get("title"));
            }
            if (body.containsKey("description")) {
                obs.setDescription((String) body.get("description"));
            }
            if (body.containsKey("locationName")) {
                obs.setLocationName((String) body.get("locationName"));
            }
            if (body.get("latitude") != null) {
                obs.setLatitude(new java.math.BigDecimal(body.get("latitude").toString()));
            }
            if (body.get("longitude") != null) {
                obs.setLongitude(new java.math.BigDecimal(body.get("longitude").toString()));
            }
            if (body.get("speciesId") != null) {
                obs.setSpeciesId(Long.valueOf(body.get("speciesId").toString()));
            }
            if (body.get("photoMediaId") != null) {
                obs.setPhotoMediaId(Long.valueOf(body.get("photoMediaId").toString()));
            }
            if (body.get("observedAt") != null) {
                String dateStr = body.get("observedAt").toString();
                obs.setObservedAt(LocalDateTime.parse(dateStr.replace(" ", "T")));
            }

            // 修改后重新设为待审核
            obs.setStatus((byte) 2);

            userObservationMapper.updateById(obs);

            result.put("success", true);
            result.put("message", "观察记录更新成功，将重新审核");
        } catch (Exception e) {
            log.error("更新观察记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 删除观察记录
     * DELETE /observation/{id}
     */
    @DeleteMapping("/{id}")
    public Map<String, Object> deleteObservation(@PathVariable Long id,
                                                  Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = extractUserId(auth);
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            UserObservation obs = userObservationMapper.selectById(id);
            if (obs == null || !obs.getUserId().equals(userId)) {
                result.put("success", false);
                result.put("message", "记录不存在或无权删除");
                return result;
            }

            userObservationMapper.deleteById(id);

            result.put("success", true);
            result.put("message", "删除成功");
        } catch (Exception e) {
            log.error("删除观察记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    // 观察社区（所有用户可见的观察记录）
    // ════════════════════════════════════════════════════════════════

    /**
     * 获取社区观察列表（所有用户的可见观察）
     * GET /observation/community
     */
    @GetMapping("/community")
    public Map<String, Object> communityList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(defaultValue = "latest") String sort,
            @RequestParam(required = false) String keyword,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            // 关键词搜索条件（标题或描述模糊匹配）
            LambdaQueryWrapper<UserObservation> baseQuery = new LambdaQueryWrapper<UserObservation>()
                    .eq(UserObservation::getStatus, (byte) 1);
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                baseQuery.and(w -> w.like(UserObservation::getTitle, kw)
                        .or().like(UserObservation::getDescription, kw));
            }

            // 分页查询状态为可见(1)的观察记录
            int offset = (pageNum - 1) * pageSize;
            Long total = userObservationMapper.selectCount(baseQuery);

            LambdaQueryWrapper<UserObservation> queryWrapper = new LambdaQueryWrapper<UserObservation>()
                    .eq(UserObservation::getStatus, (byte) 1);
            if (keyword != null && !keyword.trim().isEmpty()) {
                String kw = "%" + keyword.trim() + "%";
                queryWrapper.and(w -> w.like(UserObservation::getTitle, kw)
                        .or().like(UserObservation::getDescription, kw));
            }
            if ("hot".equalsIgnoreCase(sort)) {
                queryWrapper.orderByDesc(UserObservation::getId);
            } else {
                queryWrapper.orderByDesc(UserObservation::getCreatedAt);
            }
            List<UserObservation> list = userObservationMapper.selectList(
                    queryWrapper.last("LIMIT " + offset + ", " + pageSize));

            if (list.isEmpty()) {
                result.put("success", true);
                result.put("data", Map.of("records", Collections.emptyList(), "total", total, "pageNum", pageNum, "pageSize", pageSize));
                return result;
            }

            // 批量查物种名称
            List<Long> speciesIds = list.stream()
                    .filter(o -> o.getSpeciesId() != null)
                    .map(UserObservation::getSpeciesId).distinct().collect(Collectors.toList());
            Map<Long, String> speciesNameMap = new HashMap<>();
            if (!speciesIds.isEmpty()) {
                String inClause = speciesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, chinese_name FROM marine_species WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    speciesNameMap.put(((Number) row.get("id")).longValue(), (String) row.get("chinese_name"));
                }
            }

            // 批量查媒体URL
            List<Long> mediaIds = list.stream()
                    .filter(o -> o.getPhotoMediaId() != null)
                    .map(UserObservation::getPhotoMediaId).distinct().collect(Collectors.toList());
            Map<Long, String> mediaUrlMap = new HashMap<>();
            if (!mediaIds.isEmpty()) {
                String inClause = mediaIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, url FROM media_asset WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    mediaUrlMap.put(((Number) row.get("id")).longValue(), (String) row.get("url"));
                }
            }

            // 收集用户ID
            Set<Long> userIds = list.stream()
                    .map(UserObservation::getUserId).collect(Collectors.toSet());
            // 批量查用户信息
            Map<Long, Map<String, Object>> userInfoMap = queryUsersByIds(userIds);
            // 批量查用户徽章
            Map<Long, String> userBadgeMap = queryUserBadgesByIds(userIds);

            // 收集观察记录ID用于查点赞和评论数
            List<Long> obsIds = list.stream().map(UserObservation::getId).collect(Collectors.toList());
            // 点赞数
            Map<Long, Long> likeCountMap = new HashMap<>();
            // 当前用户点赞状态
            java.util.Set<Long> likedByMe = new java.util.HashSet<>();
            // 评论数
            Map<Long, Long> commentCountMap = new HashMap<>();
            // 收藏状态
            java.util.Set<Long> bookmarkedByMe = new java.util.HashSet<>();

            // 批量查点赞数
            for (Long oid : obsIds) {
                long lc = contentLikeMapper.selectCount(
                        new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                                .eq(com.gdou.marine.entity.ContentLike::getTargetType, "user_observation")
                                .eq(com.gdou.marine.entity.ContentLike::getTargetId, oid));
                likeCountMap.put(oid, lc);

                long cc = contentCommentMapper.selectCount(
                        new LambdaQueryWrapper<com.gdou.marine.entity.ContentComment>()
                                .eq(com.gdou.marine.entity.ContentComment::getTargetType, "user_observation")
                                .eq(com.gdou.marine.entity.ContentComment::getTargetId, oid)
                                .eq(com.gdou.marine.entity.ContentComment::getStatus, (byte) 1));
                commentCountMap.put(oid, cc);
            }

            // 当前用户的点赞状态和收藏状态
            if (currentUserId != null) {
                for (Long oid : obsIds) {
                    long myLike = contentLikeMapper.selectCount(
                            new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                                    .eq(com.gdou.marine.entity.ContentLike::getUserId, currentUserId)
                                    .eq(com.gdou.marine.entity.ContentLike::getTargetType, "user_observation")
                                    .eq(com.gdou.marine.entity.ContentLike::getTargetId, oid));
                    if (myLike > 0) likedByMe.add(oid);

                    long myBm = userBookmarkMapper.selectCount(
                            new LambdaQueryWrapper<com.gdou.marine.entity.UserBookmark>()
                                    .eq(com.gdou.marine.entity.UserBookmark::getUserId, currentUserId)
                                    .eq(com.gdou.marine.entity.UserBookmark::getTargetType, "user_observation")
                                    .eq(com.gdou.marine.entity.UserBookmark::getTargetId, oid));
                    if (myBm > 0) bookmarkedByMe.add(oid);
                }
            }

            List<Map<String, Object>> records = list.stream().map(obs -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", obs.getId());
                item.put("title", obs.getTitle());
                item.put("description", obs.getDescription());
                item.put("locationName", obs.getLocationName());
                item.put("observedAt", obs.getObservedAt() != null
                        ? obs.getObservedAt().toString().replace("T", " ") : "");
                item.put("latitude", obs.getLatitude());
                item.put("longitude", obs.getLongitude());
                item.put("speciesId", obs.getSpeciesId());
                item.put("speciesName", obs.getSpeciesId() != null
                        ? speciesNameMap.getOrDefault(obs.getSpeciesId(), "") : "");
                item.put("photoUrl", obs.getPhotoMediaId() != null
                        ? mediaUrlMap.getOrDefault(obs.getPhotoMediaId(), "") : "");
                item.put("createdAt", obs.getCreatedAt() != null
                        ? obs.getCreatedAt().toString().replace("T", " ") : "");

                // 用户信息
                Map<String, Object> uInfo = userInfoMap.getOrDefault(obs.getUserId(), new LinkedHashMap<>());
                item.put("userId", obs.getUserId());
                item.put("username", uInfo.getOrDefault("username", "未知用户"));
                item.put("avatarUrl", uInfo.getOrDefault("avatar_url", ""));
                item.put("avatarFrame", uInfo.getOrDefault("avatar_frame", "default"));
                item.put("userTitle", userTitle(uInfo, userBadgeMap.getOrDefault(obs.getUserId(), "")));

                // 互动数据
                item.put("likeCount", likeCountMap.getOrDefault(obs.getId(), 0L));
                item.put("liked", likedByMe.contains(obs.getId()));
                item.put("commentCount", commentCountMap.getOrDefault(obs.getId(), 0L));
                item.put("bookmarked", bookmarkedByMe.contains(obs.getId()));

                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", Map.of(
                    "records", records,
                    "total", total,
                    "pageNum", pageNum,
                    "pageSize", pageSize
            ));
        } catch (Exception e) {
            log.error("获取社区观察列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取社区观察详情
     * GET /observation/community/{id}
     */
    @GetMapping("/community/{id}")
    public Map<String, Object> communityDetail(@PathVariable Long id,
                                               Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long currentUserId = extractUserId(auth);

            UserObservation obs = userObservationMapper.selectById(id);
            if (obs == null || obs.getStatus() != 1) {
                result.put("success", false);
                result.put("message", "记录不存在或未公开");
                return result;
            }

            // 物种名
            String speciesName = "";
            if (obs.getSpeciesId() != null) {
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT chinese_name FROM marine_species WHERE id = " + obs.getSpeciesId());
                if (!rows.isEmpty()) {
                    speciesName = (String) rows.get(0).get("chinese_name");
                }
            }

            // 媒体URL
            String photoUrl = "";
            if (obs.getPhotoMediaId() != null) {
                com.gdou.marine.entity.MediaAsset media = mediaAssetMapper.selectById(obs.getPhotoMediaId());
                if (media != null) {
                    photoUrl = media.getUrl();
                }
            }

            // 用户信息
            Map<Long, Map<String, Object>> userInfoMap = queryUsersByIds(Set.of(obs.getUserId()));
            Map<String, Object> uInfo = userInfoMap.getOrDefault(obs.getUserId(), new LinkedHashMap<>());

            Map<Long, String> badgeMap = queryUserBadgesByIds(Set.of(obs.getUserId()));

            // 点赞
            long likeCount = contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                            .eq(com.gdou.marine.entity.ContentLike::getTargetType, "user_observation")
                            .eq(com.gdou.marine.entity.ContentLike::getTargetId, id));
            boolean liked = currentUserId != null && contentLikeMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.ContentLike>()
                            .eq(com.gdou.marine.entity.ContentLike::getUserId, currentUserId)
                            .eq(com.gdou.marine.entity.ContentLike::getTargetType, "user_observation")
                            .eq(com.gdou.marine.entity.ContentLike::getTargetId, id)) > 0;

            // 收藏
            boolean bookmarked = currentUserId != null && userBookmarkMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.UserBookmark>()
                            .eq(com.gdou.marine.entity.UserBookmark::getUserId, currentUserId)
                            .eq(com.gdou.marine.entity.UserBookmark::getTargetType, "user_observation")
                            .eq(com.gdou.marine.entity.UserBookmark::getTargetId, id)) > 0;

            // 评论数
            long commentCount = contentCommentMapper.selectCount(
                    new LambdaQueryWrapper<com.gdou.marine.entity.ContentComment>()
                            .eq(com.gdou.marine.entity.ContentComment::getTargetType, "user_observation")
                            .eq(com.gdou.marine.entity.ContentComment::getTargetId, id)
                            .eq(com.gdou.marine.entity.ContentComment::getStatus, (byte) 1));

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", obs.getId());
            item.put("title", obs.getTitle());
            item.put("description", obs.getDescription());
            item.put("locationName", obs.getLocationName());
            item.put("observedAt", obs.getObservedAt() != null
                    ? obs.getObservedAt().toString().replace("T", " ") : "");
            item.put("latitude", obs.getLatitude());
            item.put("longitude", obs.getLongitude());
            item.put("speciesId", obs.getSpeciesId());
            item.put("speciesName", speciesName);
            item.put("photoUrl", photoUrl);
            item.put("createdAt", obs.getCreatedAt() != null
                    ? obs.getCreatedAt().toString().replace("T", " ") : "");

            item.put("userId", obs.getUserId());
            item.put("username", uInfo.getOrDefault("username", "未知用户"));
            item.put("avatarUrl", uInfo.getOrDefault("avatar_url", ""));
            item.put("avatarFrame", uInfo.getOrDefault("avatar_frame", "default"));
            item.put("userTitle", userTitle(uInfo, badgeMap.getOrDefault(obs.getUserId(), "")));

            item.put("likeCount", likeCount);
            item.put("liked", liked);
            item.put("commentCount", commentCount);
            item.put("bookmarked", bookmarked);

            result.put("success", true);
            result.put("data", item);
        } catch (Exception e) {
            log.error("获取社区观察详情失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 获取常用地点标签（Top 5）
     * 从 user_observation 表中按使用频次统计 location_name
     * GET /observation/common-locations
     */
    @GetMapping("/common-locations")
    public Map<String, Object> commonLocations() {
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT location_name, COUNT(*) AS cnt FROM user_observation " +
                    "WHERE location_name IS NOT NULL AND location_name != '' " +
                    "GROUP BY location_name ORDER BY cnt DESC LIMIT 5");
            List<String> locations = rows.stream()
                    .map(row -> (String) row.get("location_name"))
                    .collect(Collectors.toList());
            result.put("success", true);
            result.put("data", locations);
        } catch (Exception e) {
            log.error("获取常用地点失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    // ════════════════════════════════════════════════════════════════
    // 管理后台：观察审核
    // ════════════════════════════════════════════════════════════════

    /**
     * 管理后台 — 观察记录列表（含审核状态筛选）
     * GET /observation/admin/list?status=&pageNum=1&pageSize=10
     */
    @GetMapping("/admin/list")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> adminList(
            @RequestParam(required = false) Integer status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 构建查询条件
            LambdaQueryWrapper<UserObservation> wrapper = new LambdaQueryWrapper<UserObservation>();
            if (status != null) {
                wrapper.eq(UserObservation::getStatus, status.byteValue());
            }
            wrapper.orderByDesc(UserObservation::getCreatedAt);

            // 总数
            Long total = userObservationMapper.selectCount(wrapper);

            // 分页
            int offset = (pageNum - 1) * pageSize;
            List<UserObservation> list = userObservationMapper.selectList(
                    wrapper.last("LIMIT " + offset + ", " + pageSize));

            // 收集用户ID查信息
            Set<Long> userIds = list.stream()
                    .map(UserObservation::getUserId).collect(Collectors.toSet());
            Map<Long, Map<String, Object>> userInfoMap = queryUsersByIds(userIds);
            Map<Long, String> userBadgeMap = queryUserBadgesByIds(userIds);

            // 物种名和媒体URL
            List<Long> speciesIds = list.stream()
                    .filter(o -> o.getSpeciesId() != null)
                    .map(UserObservation::getSpeciesId).distinct().collect(Collectors.toList());
            Map<Long, String> speciesNameMap = new HashMap<>();
            if (!speciesIds.isEmpty()) {
                String inClause = speciesIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, chinese_name FROM marine_species WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    speciesNameMap.put(((Number) row.get("id")).longValue(), (String) row.get("chinese_name"));
                }
            }
            Map<Long, String> mediaUrlMap = new HashMap<>();
            List<Long> mediaIds = list.stream()
                    .filter(o -> o.getPhotoMediaId() != null)
                    .map(UserObservation::getPhotoMediaId).distinct().collect(Collectors.toList());
            if (!mediaIds.isEmpty()) {
                String inClause = mediaIds.stream().map(String::valueOf).collect(Collectors.joining(","));
                List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                        "SELECT id, url FROM media_asset WHERE id IN (" + inClause + ")");
                for (Map<String, Object> row : rows) {
                    mediaUrlMap.put(((Number) row.get("id")).longValue(), (String) row.get("url"));
                }
            }

            List<Map<String, Object>> records = list.stream().map(obs -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", obs.getId());
                item.put("title", obs.getTitle());
                item.put("description", obs.getDescription());
                item.put("locationName", obs.getLocationName());
                item.put("observedAt", obs.getObservedAt() != null
                        ? obs.getObservedAt().toString().replace("T", " ") : "");
                item.put("speciesName", obs.getSpeciesId() != null
                        ? speciesNameMap.getOrDefault(obs.getSpeciesId(), "") : "");
                item.put("photoUrl", obs.getPhotoMediaId() != null
                        ? mediaUrlMap.getOrDefault(obs.getPhotoMediaId(), "") : "");
                item.put("status", obs.getStatus());
                item.put("auditRemark", obs.getAuditRemark());
                item.put("createdAt", obs.getCreatedAt() != null
                        ? obs.getCreatedAt().toString().replace("T", " ") : "");

                // 用户信息
                Map<String, Object> uInfo = userInfoMap.getOrDefault(obs.getUserId(), new LinkedHashMap<>());
                item.put("userId", obs.getUserId());
                item.put("username", uInfo.getOrDefault("username", "未知用户"));
                item.put("avatarUrl", uInfo.getOrDefault("avatar_url", ""));
                item.put("userTitle", userTitle(uInfo, userBadgeMap.getOrDefault(obs.getUserId(), "")));

                return item;
            }).collect(Collectors.toList());

            result.put("success", true);
            result.put("data", Map.of("records", records, "total", total, "pageNum", pageNum, "pageSize", pageSize));
        } catch (Exception e) {
            log.error("管理后台获取观察列表失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 管理后台 — 审核观察记录（通过/下架）
     * PUT /observation/admin/{id}/status
     * body: { status: 1(通过)/0(下架), auditRemark: "原因" }
     */
    @PutMapping("/admin/{id}/status")
    @org.springframework.security.access.prepost.PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Map<String, Object> adminUpdateStatus(@PathVariable Long id,
                                                  @RequestBody Map<String, Object> body,
                                                  Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            UserObservation obs = userObservationMapper.selectById(id);
            if (obs == null) {
                result.put("success", false);
                result.put("message", "记录不存在");
                return result;
            }

            Object statusObj = body.get("status");
            if (statusObj == null) {
                result.put("success", false);
                result.put("message", "请指定审核状态");
                return result;
            }
            byte newStatus = Byte.parseByte(statusObj.toString());

            // 更新状态
            obs.setStatus(newStatus);

            // 如果下架，记录原因
            if (newStatus == 0 && body.containsKey("auditRemark")) {
                obs.setAuditRemark((String) body.get("auditRemark"));
            } else if (newStatus == 1) {
                // 通过审核，清空之前的审核备注
                obs.setAuditRemark(null);
            }

            userObservationMapper.updateById(obs);

            String statusText = newStatus == 1 ? "审核通过" : "已下架";
            result.put("success", true);
            result.put("message", statusText + "成功");
        } catch (Exception e) {
            log.error("审核观察记录失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    /**
     * 批量查询用户信息
     */
    private Map<Long, Map<String, Object>> queryUsersByIds(Set<Long> userIds) {
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
     * 批量查询用户徽章（称号）
     */
    private Map<Long, String> queryUserBadgesByIds(Set<Long> userIds) {
        Map<Long, String> map = new HashMap<>();
        if (userIds.isEmpty()) return map;
        String inClause = userIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        try {
            String sql = "SELECT u.user_id, u.badge_name FROM (" +
                    " SELECT user_id, badge_name, " +
                    " ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY earned_at DESC) AS rn" +
                    " FROM user_badge WHERE user_id IN (" + inClause + ")" +
                    ") u WHERE u.rn = 1";
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
     * 获取用户称号：
     * - __none__ → 不显示任何称号
     * - 有值 → 显示 user_title
     * - 空值 → 降级显示勋章名称
     */
    private String userTitle(Map<String, Object> uInfo, String fallbackBadge) {
        String raw = (String) uInfo.getOrDefault("user_title", "");
        if ("__none__".equals(raw)) return "";
        if (raw != null && !raw.isEmpty()) return raw;
        return fallbackBadge;
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
