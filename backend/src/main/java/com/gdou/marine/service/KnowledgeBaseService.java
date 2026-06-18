package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.entity.KbDocument;
import org.springframework.web.multipart.MultipartFile;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
public interface KnowledgeBaseService {

    KbDocument uploadDocument(MultipartFile file, Long categoryId, Long userId);

    KbDocument createDocument(KbDocument document, Long userId);

    boolean deleteDocument(Long id);

    Page<KbDocument> pageDocuments(Long categoryId, String keyword, Integer pageNum, Integer pageSize);
}
