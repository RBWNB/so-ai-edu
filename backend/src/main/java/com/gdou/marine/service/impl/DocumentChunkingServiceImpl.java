package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.entity.KbDocumentChunk;
import com.gdou.marine.mapper.KbDocumentChunkMapper;
import com.gdou.marine.service.DocumentChunkingService;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description
 */
@Service
public class DocumentChunkingServiceImpl implements DocumentChunkingService{

    private final KbDocumentChunkMapper chunkMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final Tokenizer tokenizer;

    public DocumentChunkingServiceImpl(KbDocumentChunkMapper chunkMapper,
                                       EmbeddingModel embeddingModel,
                                       EmbeddingStore<TextSegment> embeddingStore,
                                       Tokenizer tokenizer) {
        this.chunkMapper = chunkMapper;
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.tokenizer = tokenizer;

    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void chunkAndStore(Long documentId, String content) {
        if (documentId == null || !StringUtils.hasText(content)) {
            return;
        }


        deleteDocumentVectors(documentId);

        Metadata metadata = new Metadata().put("documentId", documentId);
        Document document = Document.from(content, metadata);
        // 使用本地字符估算替代 API Token 化，避免网络调用导致限流或失败
        Tokenizer localTokenizer = new Tokenizer() {
            @Override public int estimateTokenCountInText(String text) { return text == null ? 0 : Math.max(1, text.length() / 2); }
            @Override public int estimateTokenCountInMessage(ChatMessage msg) { return estimateTokenCountInText(msg.toString()); }
            @Override public int estimateTokenCountInMessages(Iterable<ChatMessage> msgs) { int c=0; for (var m : msgs) c+=estimateTokenCountInMessage(m); return c; }
        };
        List<TextSegment> segments;
        try {
            DocumentSplitter splitter = DocumentSplitters.recursive(500, 100, localTokenizer);
            segments = splitter.split(document);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }

        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = TextSegment.from(segments.get(i).text(), metadata.copy().put("chunkIndex", i));
            Embedding embedding = embeddingModel.embed(segment).content();
            String embeddingKey = embeddingStore.add(embedding, segment);

            KbDocumentChunk chunk = new KbDocumentChunk();
            chunk.setDocumentId(documentId);
            chunk.setChunkIndex(i);
            chunk.setContent(segment.text());
            chunk.setEmbeddingKey(embeddingKey);
            chunk.setTokenCount(estimateTokenCount(segment.text()));
            chunkMapper.insert(chunk);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocumentVectors(Long documentId) {
        if (documentId == null) {
            return;
        }
        List<KbDocumentChunk> chunks = chunkMapper.selectByDocumentId(documentId);
        List<String> embeddingKeys = chunks.stream()
                .map(KbDocumentChunk::getEmbeddingKey)
                .filter(StringUtils::hasText)
                .toList();
        if (!embeddingKeys.isEmpty()) {
            embeddingStore.removeAll(embeddingKeys);
        }
        chunkMapper.delete(new LambdaQueryWrapper<KbDocumentChunk>()
                .eq(KbDocumentChunk::getDocumentId, documentId));
    }


    private int estimateTokenCount(String text) {
        if (!StringUtils.hasText(text)) {
            return 0;
        }
        try {
            return tokenizer.estimateTokenCountInText(text);
        } catch (Exception ignored) {
            return Math.max(1, Objects.toString(text, "").length() / 2);
        }
    }
}
