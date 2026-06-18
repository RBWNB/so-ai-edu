package com.gdou.marine.service;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
public interface DocumentChunkingService {
    void chunkAndStore(Long documentId, String content);

    void deleteDocumentVectors(Long documentId);
}
