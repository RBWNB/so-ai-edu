package com.gdou.marine.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.util.StringUtils;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {
    default Page<KbDocument> selectPageByCategory(Page<KbDocument> page, Long categoryId) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        wrapper.orderByDesc(KbDocument::getCreatedAt);
        return selectPage(page, wrapper);
    }

    default Page<KbDocument> searchByKeyword(Page<KbDocument> page, Long categoryId, String keyword) {
        LambdaQueryWrapper<KbDocument> wrapper = new LambdaQueryWrapper<>();
        if (categoryId != null) {
            wrapper.eq(KbDocument::getCategoryId, categoryId);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(KbDocument::getTitle, kw)
                    .or().like(KbDocument::getContent, kw)
                    .or().like(KbDocument::getSource, kw));
        }
        wrapper.orderByDesc(KbDocument::getCreatedAt);
        return selectPage(page, wrapper);
    }
}
