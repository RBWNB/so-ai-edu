package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.entity.KbDocument;
import com.gdou.marine.mapper.KbDocumentMapper;
import com.gdou.marine.service.DocumentChunkingService;
import com.gdou.marine.service.KnowledgeBaseService;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KbDocumentMapper, KbDocument>
        implements KnowledgeBaseService {

    private final DocumentChunkingService documentChunkingService;
    private final Tika tika = new Tika();

    public KnowledgeBaseServiceImpl(DocumentChunkingService documentChunkingService) {
        this.documentChunkingService = documentChunkingService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument uploadDocument(MultipartFile file, Long categoryId, Long userId) {
        // 1. 文件解析阶段
        String content;
        try (InputStream inputStream = file.getInputStream()) {
            content = tika.parseToString(inputStream);
        } catch (Exception e) {
            throw new RuntimeException("failed to parse document", e); // 只有这里报错才是真正的解析失败
        }

        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("document content is empty");
        }

        // 2. 业务落库阶段
        KbDocument document = new KbDocument();
            document.setCategoryId(categoryId);
            document.setTitle(resolveTitle(file.getOriginalFilename()));
            document.setContent(content.trim());
            document.setSource(file.getOriginalFilename());
            document.setSourceType("upload");
            document.setStatus((byte) 1);
            document.setCreatedBy(userId);
            save(document);
        // 3. AI 向量化阶段
        try {
            documentChunkingService.chunkAndStore(document.getId(), document.getContent());
        } catch (Exception e) {
            // 明确抛出向量化失败的原因，而不是解析失败
            throw new RuntimeException("文档保存成功，但在向量化分块时失败: " + e.getMessage(), e);
        }
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public KbDocument createDocument(KbDocument document, Long userId) {
        if (document == null || !StringUtils.hasText(document.getTitle()) || !StringUtils.hasText(document.getContent())) {
            throw new IllegalArgumentException("title and content are required");
        }

        document.setId(null);
        document.setSourceType(StringUtils.hasText(document.getSourceType()) ? document.getSourceType() : "manual");
        document.setStatus(document.getStatus() == null ? (byte) 1 : document.getStatus());
        document.setCreatedBy(userId);
        save(document);

        documentChunkingService.chunkAndStore(document.getId(), document.getContent());
        return document;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long id) {
        if (id == null) {
            return false;
        }
        documentChunkingService.deleteDocumentVectors(id);
        return removeById(id);
    }

    @Override
    public Page<KbDocument> pageDocuments(Long categoryId, String keyword, Integer pageNum, Integer pageSize) {
        long current = pageNum == null || pageNum < 1 ? 1 : pageNum;
        long size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        return baseMapper.searchByKeyword(new Page<>(current, size), categoryId, keyword);
    }

    private String resolveTitle(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "Untitled document";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            return originalFilename.substring(0, dotIndex);
        }
        return originalFilename;
    }

}
