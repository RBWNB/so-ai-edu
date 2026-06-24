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
