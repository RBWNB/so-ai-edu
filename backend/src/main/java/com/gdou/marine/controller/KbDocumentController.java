package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.entity.KbCategory;
import com.gdou.marine.entity.KbDocument;
import com.gdou.marine.mapper.KbCategoryMapper;
import com.gdou.marine.mapper.KbDocumentMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 知识库文档 Controller（用于题库选择知识库帖子）
 */
@RestController
@RequestMapping("/kb")
public class KbDocumentController {

    @Autowired
    private KbDocumentMapper kbDocumentMapper;

    @Autowired
    private KbCategoryMapper kbCategoryMapper;

    /**
     * 分页查询知识库文档列表
     */
    @GetMapping("/document/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<KbDocument> getDocumentPage(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size) {

        int pg = current != null ? current : pageNum;
        int ps = size != null ? size : pageSize;

        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbDocument::getStatus, 1);

        if (categoryId != null && categoryId > 0) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(title)) {
            wrapper.like(KbDocument::getTitle, title.trim());
        }
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(KbDocument::getTitle, keyword.trim())
                    .or().like(KbDocument::getContent, keyword.trim())
            );
        }

        wrapper.orderByDesc(KbDocument::getCreatedAt);
        return kbDocumentMapper.selectPage(new Page<>(pg, ps), wrapper);
    }

    /**
     * 获取知识库文档详情
     */
    @GetMapping("/document/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public KbDocument getDocumentById(@PathVariable Long id) {
        return kbDocumentMapper.selectById(id);
    }

    /**
     * 获取所有知识分类
     */
    @GetMapping("/category/list")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public List<KbCategory> getCategoryList() {
        LambdaQueryWrapper<KbCategory> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KbCategory::getStatus, 1);
        wrapper.orderByAsc(KbCategory::getSortOrder);
        return kbCategoryMapper.selectList(wrapper);
    }
}
