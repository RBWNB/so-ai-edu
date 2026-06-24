package com.gdou.marine.controller;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.Ecosystem;
import com.gdou.marine.entity.Species;
import com.gdou.marine.service.impl.EcosystemServiceImpl;
import com.gdou.marine.service.impl.SpeciesServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/visual")
public class VisualController {

    @Autowired
    private SpeciesServiceImpl speciesService;

    @Autowired
    private EcosystemServiceImpl ecosystemService;

    @Autowired
    private com.gdou.marine.mapper.KbDocumentMapper kbDocumentMapper;

    @Autowired
    private com.gdou.marine.mapper.QuizAttemptMapper quizAttemptMapper;

    @Autowired
    private com.gdou.marine.mapper.SysUserMapper appUserMapper;

    @Autowired
    private com.gdou.marine.mapper.AiCallLogMapper aiCallLogMapper;

    @Autowired
    private com.gdou.marine.mapper.ConversationMessageMapper conversationMessageMapper;

    // ==================== 综合看板 ====================

    @GetMapping("/summary")
    public Map<String, Object> getSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();

        long speciesCount = speciesService.lambdaQuery().eq(Species::getStatus, 1).count();
        long ecoCount = ecosystemService.lambdaQuery().eq(Ecosystem::getStatus, 1).count();

        summary.put("speciesCount", speciesCount);
        summary.put("ecosystemCount", ecoCount);
        return summary;
    }

    // ==================== 物种统计 ====================

    @GetMapping("/species-protection-stats")
    public List<Map<String, Object>> getSpeciesProtectionStats() {
        List<Species> speciesList = speciesService.lambdaQuery()
                .eq(Species::getStatus, 1).list();

        Map<String, Long> grouped = speciesList.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getConservationStatus() != null && !s.getConservationStatus().isEmpty()
                                ? s.getConservationStatus() : "未标注",
                        Collectors.counting()
                ));

        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList());
    }

    @GetMapping("/species-taxonomy-stats")
    public List<Map<String, Object>> getSpeciesTaxonomyStats(
            @RequestParam(required = false, defaultValue = "phylum") String level) {
        List<Species> speciesList = speciesService.lambdaQuery()
                .eq(Species::getStatus, 1).list();

        Map<String, Long> grouped;
        switch (level) {
            case "class":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getClassName()), Collectors.counting()));
                break;
            case "order":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getOrderName()), Collectors.counting()));
                break;
            case "family":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getFamilyName()), Collectors.counting()));
                break;
            case "genus":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getGenusName()), Collectors.counting()));
                break;
            default:
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getPhylum()), Collectors.counting()));
                break;
        }

        return grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .collect(Collectors.toList());
    }

    @GetMapping("/species-count-stats")
    public Map<String, Object> getSpeciesCountStats(
            @RequestParam(required = false, defaultValue = "phylum") String level) {
        List<Species> speciesList = speciesService.lambdaQuery()
                .eq(Species::getStatus, 1).list();

        Map<String, Long> grouped;
        switch (level) {
            case "kingdom":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getKingdom()), Collectors.counting()));
                break;
            case "class":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getClassName()), Collectors.counting()));
                break;
            case "order":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getOrderName()), Collectors.counting()));
                break;
            case "family":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getFamilyName()), Collectors.counting()));
                break;
            case "genus":
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getGenusName()), Collectors.counting()));
                break;
            default:
                grouped = speciesList.stream().collect(Collectors.groupingBy(
                        s -> blankDefault(s.getPhylum()), Collectors.counting()));
                break;
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taxonomyStats", grouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .collect(Collectors.toList()));

        Map<String, Long> protectionGrouped = speciesList.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getConservationStatus() != null && !s.getConservationStatus().isEmpty()
                                ? s.getConservationStatus() : "未标注",
                        Collectors.counting()
                ));

        result.put("protectionStats", protectionGrouped.entrySet().stream().map(e -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList()));

        return result;
    }

    // ==================== 生态系统统计 ====================

    @GetMapping("/ecosystem-stats")
    public List<Map<String, Object>> getEcosystemStats() {
        List<Ecosystem> ecosystems = ecosystemService.lambdaQuery()
                .eq(Ecosystem::getStatus, 1).list();

        List<Map<String, Object>> result = new ArrayList<>();
        for (Ecosystem eco : ecosystems) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", eco.getId());
            m.put("name", eco.getName());
            m.put("typicalSpecies", eco.getTypicalSpecies());
            m.put("threats", eco.getThreats());
            m.put("description", eco.getDescription());
            result.add(m);
        }
        return result;
    }
// ==================== B端运营大盘数据 (Dashboard) ====================

    /**
     *  顶部 KPI 卡片汇总数据
     */
    @GetMapping("/admin/dashboard-kpi")
    public Map<String, Object> getAdminDashboardKpi() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 总用户数
        long userCount = appUserMapper.selectCount(null);
        // 总答题人次
        long quizAttempts = quizAttemptMapper.selectCount(null);
        // 知识库总文档数
        long kbCount = kbDocumentMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.gdou.marine.entity.KbDocument>()
                        .eq(com.gdou.marine.entity.KbDocument::getStatus, 1)
        );
        // AI 调用总数
        long aiCallCount = aiCallLogMapper.selectCount(null);

        // 也可以保留之前的物种和生态数据
        long speciesCount = speciesService.lambdaQuery().eq(com.gdou.marine.entity.Species::getStatus, 1).count();
        long ecoCount = ecosystemService.lambdaQuery().eq(com.gdou.marine.entity.Ecosystem::getStatus, 1).count();

        result.put("userCount", userCount);
        result.put("quizAttempts", quizAttempts);
        result.put("kbCount", kbCount);
        result.put("aiCallCount", aiCallCount);
        result.put("speciesCount", speciesCount);
        result.put("ecosystemCount", ecoCount);

        return result;
    }

    /**
     * AI 问答词云热力图数据
     */
    @GetMapping("/admin/ai-word-cloud")
    public Map<String, Object> getAiWordCloud() {
        // 1. 只查询用户的提问 (role = 'user')
        List<com.gdou.marine.entity.ConversationMessage> messages = conversationMessageMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.gdou.marine.entity.ConversationMessage>()
                        .eq(com.gdou.marine.entity.ConversationMessage::getRole, "user")
        );

        // 2. 统计提问频率 (去除常见标点符号，避免相似提问被拆分)
        Map<String, Long> freqMap = messages.stream()
                .map(com.gdou.marine.entity.ConversationMessage::getContent)
                .filter(org.springframework.util.StringUtils::hasText)
                .map(text -> text.replaceAll("[。？！，、\\?\\!\\.\\,\\s]", "")) // 剔除标点
                .collect(java.util.stream.Collectors.groupingBy(text -> text, java.util.stream.Collectors.counting()));

        // 3. 转换为 ECharts 需要的 [{name: 'xxx', value: 10}] 格式
        List<Map<String, Object>> wordCloudData = freqMap.entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    String text = e.getKey();
                    // 如果句子太长，截断它以保证词云的美观性
                    if (text.length() > 12) {
                        text = text.substring(0, 12) + "..";
                    }
                    map.put("name", text);
                    map.put("value", e.getValue());
                    return map;
                })
                // 按热度降序，最多取前 50 个热词/热句
                .sorted((a, b) -> Long.compare((Long) b.get("value"), (Long) a.get("value")))
                .limit(50)
                .collect(java.util.stream.Collectors.toList());

        Map<String, Object> result = new java.util.HashMap<>();
        result.put("success", true);
        result.put("data", wordCloudData);
        return result;
    }

    /**
     *  近7天活跃度趋势 (答题数)
     */
    @GetMapping("/admin/activity-trend")
    public Map<String, Object> getActivityTrend() {
        // 取过去7天的数据
        java.time.LocalDateTime sevenDaysAgo = java.time.LocalDateTime.now().minusDays(6).with(java.time.LocalTime.MIN);

        List<com.gdou.marine.entity.QuizAttempt> attempts = quizAttemptMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.gdou.marine.entity.QuizAttempt>()
                        .ge(com.gdou.marine.entity.QuizAttempt::getAttemptedAt, sevenDaysAgo)
        );

        // 初始化近7天的日期列表 (格式: MM-dd)
        List<String> dateLabels = new ArrayList<>();
        Map<String, Long> dailyCounts = new LinkedHashMap<>();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        for (int i = 6; i >= 0; i--) {
            String d = java.time.LocalDate.now().minusDays(i).format(fmt);
            dateLabels.add(d);
            dailyCounts.put(d, 0L); // 默认补0
        }

        // 填充真实数据
        for (com.gdou.marine.entity.QuizAttempt att : attempts) {
            if (att.getAttemptedAt() != null) {
                String d = att.getAttemptedAt().format(fmt);
                if (dailyCounts.containsKey(d)) {
                    dailyCounts.put(d, dailyCounts.get(d) + 1);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("dates", dateLabels);
        result.put("quizData", new ArrayList<>(dailyCounts.values()));
        return result;
    }


    // ==================== 数据导出 ====================

    @Log(module = "数据可视化", description = "导出物种CSV数据报表")
    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public void exportCsv(@RequestParam(defaultValue = "species") String type,
                          HttpServletResponse response) throws Exception {
        response.setContentType("text/csv;charset=utf-8");
        String filename = URLEncoder.encode(type + "_export_" + LocalDate.now(), StandardCharsets.UTF_8);
        response.setHeader("Content-Disposition", "attachment;filename=" + filename + ".csv");

        StringBuilder sb = new StringBuilder();

        if ("species".equals(type)) {
            sb.append("中文名,学名,保护等级,门,纲,目,科,属,分布区域,栖息地\n");
            List<Species> list = speciesService.lambdaQuery().eq(Species::getStatus, 1).list();
            for (Species s : list) {
                sb.append(csvCell(s.getChineseName())).append(",");
                sb.append(csvCell(s.getScientificName())).append(",");
                sb.append(csvCell(s.getConservationStatus())).append(",");
                sb.append(csvCell(s.getPhylum())).append(",");
                sb.append(csvCell(s.getClassName())).append(",");
                sb.append(csvCell(s.getOrderName())).append(",");
                sb.append(csvCell(s.getFamilyName())).append(",");
                sb.append(csvCell(s.getGenusName())).append(",");
                sb.append(csvCell(s.getDistributionArea())).append(",");
                sb.append(csvCell(s.getHabitat())).append("\n");
            }
        } else if ("ecosystem".equals(type)) {
            sb.append("名称,描述,典型物种,主要威胁\n");
            List<Ecosystem> list = ecosystemService.lambdaQuery().eq(Ecosystem::getStatus, 1).list();
            for (Ecosystem e : list) {
                sb.append(csvCell(e.getName())).append(",");
                sb.append(csvCell(e.getDescription())).append(",");
                sb.append(csvCell(e.getTypicalSpecies())).append(",");
                sb.append(csvCell(e.getThreats())).append("\n");
            }
        }

        response.getWriter().write('﻿' + sb.toString()); // BOM for Excel UTF-8
    }

    // ==================== 私有方法 ====================

    private String blankDefault(String v) {
        return v != null && !v.isEmpty() ? v : "未分类";
    }

    private String csvCell(String v) {
        if (v == null) return "";
        if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            return "\"" + v.replace("\"", "\"\"") + "\"";
        }
        return v;
    }
}
