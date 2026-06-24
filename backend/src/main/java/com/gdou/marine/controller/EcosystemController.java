package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.Ecosystem;
import com.gdou.marine.entity.UserBookmark;
import com.gdou.marine.mapper.UserBookmarkMapper;
import com.gdou.marine.service.impl.EcosystemServiceImpl;
import com.gdou.marine.utils.QiniuUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.UUID;

@RestController
@RequestMapping("/ecosystem")
public class EcosystemController {

    @Autowired
    private EcosystemServiceImpl ecosystemService;

    @Autowired
    private UserBookmarkMapper userBookmarkMapper;

    @GetMapping
    public Page<Ecosystem> getPage(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String typicalSpecies,
            @RequestParam(required = false) String threats,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size) {
        int pg = current != null ? current : pageNum;
        int ps = size != null ? size : pageSize;
        LambdaQueryWrapper<Ecosystem> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（名称、描述、典型物种）
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Ecosystem::getName, kw)
                    .or().like(Ecosystem::getDescription, kw)
                    .or().like(Ecosystem::getTypicalSpecies, kw));
        }
        
        // 名称搜索
        if (StringUtils.hasText(name)) {
            wrapper.like(Ecosystem::getName, name.trim());
        }
        
        // 典型物种搜索
        if (StringUtils.hasText(typicalSpecies)) {
            wrapper.like(Ecosystem::getTypicalSpecies, typicalSpecies.trim());
        }
        
        // 主要威胁搜索
        if (StringUtils.hasText(threats)) {
            wrapper.like(Ecosystem::getThreats, threats.trim());
        }
        
        wrapper.eq(Ecosystem::getStatus, 1);
        wrapper.orderByAsc(Ecosystem::getId);
        return ecosystemService.page(new Page<>(pg, ps), wrapper);
    }

    @GetMapping("/list")
    public List<Ecosystem> getAll() {
        return ecosystemService.lambdaQuery()
                .eq(Ecosystem::getStatus, 1)
                .orderByAsc(Ecosystem::getId)
                .list();
    }

    @GetMapping("/{id}")
    public Ecosystem getById(@PathVariable Long id) {
        return ecosystemService.getById(id);
    }

    @Log(module = "生态系统", description = "新增生态系统")
    @PostMapping
    public Map<String, Object> create(@RequestBody Ecosystem ecosystem) {
        ecosystem.setStatus((byte) 1);
        ecosystemService.save(ecosystem);
        return Map.of("id", ecosystem.getId(), "message", "创建成功");
    }

    @Log(module = "生态系统", description = "修改生态系统")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Ecosystem ecosystem) {
        ecosystem.setId(id);
        ecosystemService.updateById(ecosystem);
        return Map.of("message", "更新成功");
    }

    @Log(module = "生态系统", description = "删除生态系统")
    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        ecosystemService.removeById(id);

        // 级联删除该生态系统的所有收藏记录
        userBookmarkMapper.delete(new LambdaQueryWrapper<UserBookmark>()
                .eq(UserBookmark::getTargetType, "ecosystem")
                .eq(UserBookmark::getTargetId, id));

        return Map.of("message", "删除成功");
    }

    @Log(module = "生态系统", description = "上传生态系统图片")
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
            // 生成唯一文件名: ecosystem/日期/uuid.ext
            String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String fileName = "ecosystem/" + datePath + "/" + UUID.randomUUID().toString().replace("-", "") + ext;

            // 上传到七牛云
            byte[] bytes = file.getBytes();
            String url = QiniuUtils.upload2Qiniu(bytes, fileName);

            result.put("success", true);
            result.put("url", url);
            result.put("message", "上传成功");
        } catch (Exception e) {
            throw new RuntimeException("图片上传失败: " + e.getMessage(), e);
        }
        return result;
    }
}
