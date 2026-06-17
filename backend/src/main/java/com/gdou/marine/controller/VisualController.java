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
