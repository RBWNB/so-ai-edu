package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.entity.Species;
import com.gdou.marine.entity.SpeciesBrowseRecord;
import com.gdou.marine.entity.UserBookmark;
import com.gdou.marine.entity.UserObservation;
import com.gdou.marine.mapper.QuizQuestionMapper;
import com.gdou.marine.mapper.SpeciesBrowseRecordMapper;
import com.gdou.marine.mapper.UserBookmarkMapper;
import com.gdou.marine.mapper.UserObservationMapper;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import com.gdou.marine.service.SpeciesBrowseRecordService;
import com.gdou.marine.service.TaskProgressService;
import com.gdou.marine.service.impl.SpeciesServiceImpl;
import com.gdou.marine.utils.QiniuUtils;
import com.gdou.marine.utils.UploadPathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/species")
public class SpeciesController {

    private static final Logger log = LoggerFactory.getLogger(SpeciesController.class);

    @Autowired
    private SpeciesServiceImpl speciesService;

    @Autowired
    private SpeciesBrowseRecordService browseRecordService;

    @Autowired
    private TaskProgressService taskProgressService;

    @Autowired
    private UserBookmarkMapper userBookmarkMapper;

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Autowired
    private SpeciesBrowseRecordMapper speciesBrowseRecordMapper;

    @Autowired
    private UserObservationMapper userObservationMapper;

    @Autowired
    private DataSource dataSource;

    @Value("${upload.dir:uploads}")
    private String uploadDir;

    @GetMapping
    public Page<Species> getSpeciesPage(
            @RequestParam(required = false) String chineseName,
            @RequestParam(required = false) String scientificName,
            @RequestParam(required = false) String conservationStatus,
            @RequestParam(required = false) String phylum,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String orderName,
            @RequestParam(required = false) String familyName,
            @RequestParam(required = false) String genusName,
            @RequestParam(required = false) String distributionArea,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size) {

        int pg = current != null ? current : pageNum;
        int ps = size != null ? size : pageSize;

        LambdaQueryWrapper<Species> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Species::getStatus, 1);

        if (StringUtils.hasText(chineseName)) {
            wrapper.like(Species::getChineseName, chineseName.trim());
        }
        if (StringUtils.hasText(scientificName)) {
            wrapper.like(Species::getScientificName, scientificName.trim());
        }
        if (StringUtils.hasText(conservationStatus)) {
            wrapper.like(Species::getConservationStatus, conservationStatus.trim());
        }
        if (StringUtils.hasText(phylum)) {
            wrapper.like(Species::getPhylum, phylum.trim());
        }
        if (StringUtils.hasText(className)) {
            wrapper.like(Species::getClassName, className.trim());
        }
        if (StringUtils.hasText(orderName)) {
            wrapper.like(Species::getOrderName, orderName.trim());
        }
        if (StringUtils.hasText(familyName)) {
            wrapper.like(Species::getFamilyName, familyName.trim());
        }
        if (StringUtils.hasText(genusName)) {
            wrapper.like(Species::getGenusName, genusName.trim());
        }
        if (StringUtils.hasText(distributionArea)) {
            wrapper.like(Species::getDistributionArea, distributionArea.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Species::getChineseName, keyword.trim())
                    .or().like(Species::getScientificName, keyword.trim())
                    .or().like(Species::getMorphologyDesc, keyword.trim())
                    .or().like(Species::getHabitDesc, keyword.trim())
            );
        }

        wrapper.orderByDesc(Species::getCreatedAt);
        return speciesService.page(new Page<>(pg, ps), wrapper);
    }

    @GetMapping("/{id}")
    public Species getSpeciesById(@PathVariable Long id) {
        Species species = speciesService.getById(id);

        // 记录浏览行为（仅登录用户）
        Long userId = getCurrentUserIdSafe();
        if (userId != null && species != null) {
            browseRecordService.recordBrowse(userId, id);
            // 推进每日任务「浏览物种」进度
            taskProgressService.incrementProgress(userId, "read_species");
        }

        return species;
    }

    /**
     * 安全获取当前登录用户ID，未登录返回null
     */
    private Long getCurrentUserIdSafe() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()
                    || authentication.getPrincipal() == null
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                return null;
            }
            Object principal = authentication.getPrincipal();
            if (principal instanceof Long l) return l;
            if (principal instanceof Integer i) return i.longValue();
            if (principal instanceof String s) {
                try { return Long.parseLong(s); } catch (NumberFormatException ignored) { return null; }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 智能搜索建议：根据关键词返回匹配的物种列表（用于搜索补全）
     */
    @GetMapping("/suggest")
    public Map<String, Object> suggestSpecies(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") Integer limit) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (!StringUtils.hasText(keyword)) {
                result.put("success", false);
                result.put("message", "关键词不能为空");
                return result;
            }

            LambdaQueryWrapper<Species> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Species::getStatus, 1);
            wrapper.and(w -> w
                    .like(Species::getChineseName, keyword.trim())
                    .or().like(Species::getScientificName, keyword.trim())
            );
            wrapper.orderByDesc(Species::getCreatedAt);
            wrapper.last("LIMIT " + Math.min(limit, 20));

            result.put("success", true);
            result.put("data", speciesService.list(wrapper));
        } catch (Exception e) {
            log.error("搜索建议失败", e);
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @Log(module = "物种管理", description = "新增物种")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> createSpecies(@RequestBody Species species) {
        species.setId(null);
        species.setStatus((byte) 1);
        speciesService.save(species);
        return Map.of("id", species.getId(), "message", "创建成功");
    }

    @Log(module = "物种管理", description = "修改物种信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> updateSpecies(@PathVariable Long id, @RequestBody Species species) {
        species.setId(id);
        speciesService.updateById(species);
        return Map.of("message", "更新成功");
    }

    @Log(module = "物种管理", description = "修改物种信息")
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> updateSpeciesByBody(@RequestBody Species species) {
        speciesService.updateById(species);
        return Map.of("message", "更新成功");
    }

    @Log(module = "物种管理", description = "删除物种")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> deleteSpecies(@PathVariable Long id) {
        Species species = speciesService.getById(id);
        String name = species != null ? species.getChineseName() : ("ID=" + id);

        try {
            log.info("🗑️ 开始删除物种: ID={}, 名称={}", id, name);

            // 第1步：清理题目关联（保留题目数据，只设置species_id为NULL）
            // 注意：题目不参与级联删除，因为题目是重要资产，需要保留
            QuizQuestion updateQuestion = new QuizQuestion();
            updateQuestion.setSpeciesId(null);
            quizQuestionMapper.update(updateQuestion,
                    new LambdaQueryWrapper<QuizQuestion>().eq(QuizQuestion::getSpeciesId, id));
            log.info("✅ 已清理物种{}的题目关联（{}条题目已解除绑定）", id,
                    quizQuestionMapper.selectCount(new LambdaQueryWrapper<QuizQuestion>()
                            .eq(QuizQuestion::getSpeciesId, null)));

            // 第2步：删除物种本身（其他关联数据由数据库CASCADE自动处理！）
            boolean deleted = speciesService.removeById(id);

            if (deleted) {
                log.info("✅ 成功删除物种: ID={}, 名称={}", id, name);
                log.info("📋 级联删除说明：MySQL已自动删除该物种的所有关联数据：");
                log.info("   • 浏览记录 (species_browse_record)");
                log.info("   • 观察记录 (user_observation)");
                log.info("   • 收藏记录 (user_bookmark, type=species)");
                log.info("   • 分布点 (species_distribution_point)");
                log.info("   • 媒体资源 (species_media)");

                return Map.of(
                        "success", true,
                        "message", "删除成功",
                        "deletedId", id,
                        "deletedName", name,
                        "cascadeDeleted", true,
                        "note", "所有关联数据已通过数据库级联删除自动清理"
                );
            } else {
                throw new RuntimeException("物种不存在或已被删除");
            }

        } catch (Exception e) {
            log.error("❌ 删除物种失败: ID={}, 错误={}", id, e.getMessage(), e);
            throw new RuntimeException("删除物种失败：" + e.getMessage(), e);
        }
    }

    @Log(module = "物种管理", description = "上传物种图片")
    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> uploadImage(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        try {
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择图片文件");
                return result;
            }

            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }
            // 生成唯一文件名: species/日期/uuid.ext
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = "species/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

            // 上传到七牛云
            byte[] bytes = file.getBytes();
            String url = QiniuUtils.upload2Qiniu(bytes, fileName);

            result.put("success", true);
            result.put("url", url);
            result.put("message", "上传成功");
        } catch (Exception e) {
            log.error("图片上传失败", e);
            result.put("success", false);
            result.put("message", "上传失败：" + e.getMessage());
        }
        return result;
    }

    @Log(module = "物种管理", description = "批量导入CSV")
    @PostMapping("/import-csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> importCsv(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();
        int successCount = 0;
        int skipCount = 0;
        List<String> errors = new ArrayList<>();

        try {
            if (file == null || file.isEmpty()) {
                result.put("success", false);
                result.put("message", "请选择CSV文件");
                return result;
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null || !originalName.toLowerCase().endsWith(".csv")) {
                result.put("success", false);
                result.put("message", "仅支持.csv格式文件");
                return result;
            }

            List<Species> batch = new ArrayList<>();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));

            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.trim().isEmpty()) {
                reader.close();
                result.put("success", false);
                result.put("message", "CSV文件为空");
                return result;
            }

            // Remove BOM if present
            if (headerLine.startsWith("﻿")) {
                headerLine = headerLine.substring(1);
            }

            String line;
            int lineNum = 1;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] fields = parseCsvLine(line);
                    // ⚠️ 新表 marine_species 移除了 image_url/video_url，
                    // CSV 格式调整为 15 列（跳过旧媒体URL列）
                    if (fields.length < 15) {
                        errors.add("第" + lineNum + "行：列数不足（需要15列，实际" + fields.length + "列）");
                        skipCount++;
                        continue;
                    }

                    Species s = new Species();
                    s.setChineseName(nonEmpty(fields[0]));
                    s.setScientificName(nonEmpty(fields[1]));
                    s.setKingdom(nonEmpty(fields[2]));
                    s.setPhylum(nonEmpty(fields[3]));
                    s.setClassName(nonEmpty(fields[4]));
                    s.setOrderName(nonEmpty(fields[5]));
                    s.setFamilyName(nonEmpty(fields[6]));
                    s.setGenusName(nonEmpty(fields[7]));
                    s.setSpeciesName(nonEmpty(fields[8]));
                    s.setConservationStatus(nonEmpty(fields[9]));
                    s.setHabitat(nonEmpty(fields[10]));
                    s.setDistributionArea(nonEmpty(fields[11]));
                    s.setMorphologyDesc(nonEmpty(fields[12]));
                    s.setHabitDesc(nonEmpty(fields[13]));
                    s.setDataSource(nonEmpty(fields[14]));
                    s.setStatus((byte) 1);

                    if (s.getChineseName() == null || s.getChineseName().isEmpty()) {
                        errors.add("第" + lineNum + "行：中文名不能为空");
                        skipCount++;
                        continue;
                    }
                    if (s.getScientificName() == null || s.getScientificName().isEmpty()) {
                        errors.add("第" + lineNum + "行：学名不能为空");
                        skipCount++;
                        continue;
                    }

                    batch.add(s);
                } catch (Exception e) {
                    errors.add("第" + lineNum + "行解析失败：" + e.getMessage());
                    skipCount++;
                }
            }
            reader.close();

            if (!batch.isEmpty()) {
                try {
                    successCount = batch.size();
                    speciesService.saveBatch(batch);
                } catch (Exception e) {
                    // If batch insert fails, try one by one to isolate duplicates
                    log.warn("批量导入部分失败，尝试逐条导入: {}", e.getMessage());
                    successCount = 0;
                    for (Species s : batch) {
                        try {
                            speciesService.save(s);
                            successCount++;
                        } catch (Exception ex) {
                            errors.add("学名 " + s.getScientificName() + " 导入失败：" + ex.getMessage());
                            skipCount++;
                        }
                    }
                }
            }

            result.put("success", true);
            result.put("total", successCount + skipCount);
            result.put("imported", successCount);
            result.put("skipped", skipCount);
            result.put("errors", errors);
            if (successCount > 0) {
                result.put("message", "成功导入" + successCount + "条记录" +
                        (skipCount > 0 ? "，跳过" + skipCount + "条" : ""));
            } else {
                result.put("message", "导入失败，所有记录均未成功导入");
            }
        } catch (IOException e) {
            log.error("CSV导入失败", e);
            result.put("success", false);
            result.put("message", "导入失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * Parse a single CSV line, handling quoted fields.
     */
    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        current.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    current.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(current.toString().trim());
                    current = new StringBuilder();
                } else {
                    current.append(c);
                }
            }
        }
        fields.add(current.toString().trim());

        return fields.toArray(new String[0]);
    }

    private String nonEmpty(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }
}