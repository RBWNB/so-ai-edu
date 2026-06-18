package com.gdou.marine.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.community.model.dashscope.QwenStreamingChatModel;
import dev.langchain4j.community.model.dashscope.QwenTokenizer;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import redis.clients.jedis.JedisPooled;

/**
 * @author FlnyXx
 * @version 2.0
 * @date 2026/6/17
 * @Description LangChain4j 核心组件配置：Chat / Embedding / VectorStore / Tokenizer。
 * 替代旧版 Spring AI 配置，全部使用 DashScope (百炼) API。
 */
@Configuration
public class SaaLLMConfig {

    @Value("${app.ai.api-key}")
    private String apiKey;

    @Value("${app.ai.chat.model:deepseek-v3}")
    private String chatModel;

    @Value("${app.ai.embedding.model:text-embedding-v3}")
    private String embeddingModel;

    // ==================== Chat ====================

    @Bean
    public ChatLanguageModel chatLanguageModel() {
        return QwenChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModel)
                .build();
    }

    @Bean
    public StreamingChatLanguageModel streamingChatLanguageModel() {
        return QwenStreamingChatModel.builder()
                .apiKey(apiKey)
                .modelName(chatModel)
                .build();
    }

    // ==================== Embedding ====================

    @Primary
    @Bean
    public EmbeddingModel embeddingModel() {
        System.err.println(">>> Creating DashScopeOpenAiEmbeddingModel bean, model=" + embeddingModel);
        return new DashScopeOpenAiEmbeddingModel(apiKey, embeddingModel, null);
    }

    // ==================== Tokenizer ====================

    @Bean
    public Tokenizer tokenizer() {
        // Tokenization API 不支持带日期的快照模型名，使用 qwen-plus 基名
        String tokenizerModel = chatModel.contains("-20") ? "qwen-plus" : chatModel;
        return QwenTokenizer.builder()
                .apiKey(apiKey)
                .modelName(tokenizerModel)
                .build();
    }

    // ==================== Vector Store ====================

    @Value("${spring.data.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.data.redis.port:16379}")
    private int redisPort;

    @Value("${app.ai.vectorstore.redis.index-name:rag-index}")
    private String indexName;

    @Value("${app.ai.vectorstore.redis.prefix:rag:chunk}")
    private String prefix;

    @Value("${app.ai.embedding.dimension:1024}")
    private int dimension;

    @Bean
    public JedisPooled jedisPooled() {
        return new JedisPooled(redisHost, redisPort);
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore(JedisPooled jedisPooled) {
        return new RedisVectorStore(jedisPooled, indexName, prefix, dimension);
    }
}
