package com.gdou.marine.service.impl;

import com.gdou.marine.entity.AiCallLog;
import com.gdou.marine.entity.ConversationMessage;
import com.gdou.marine.mapper.AiCallLogMapper;
import com.gdou.marine.mapper.ConversationMessageMapper;
import com.gdou.marine.service.RagQaService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.TokenUsage;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description RAG 问答服务实现
 */
@Service
public class RagQaServiceImpl implements RagQaService {
    private static final int DEFAULT_TOP_K = 5;
    private static final int HISTORY_LIMIT = 10;

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ConversationMessageMapper conversationMessageMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final String chatModelName;

    public RagQaServiceImpl(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ChatLanguageModel chatLanguageModel,
                            StreamingChatLanguageModel streamingChatLanguageModel,
                            ConversationMessageMapper conversationMessageMapper,
                            AiCallLogMapper aiCallLogMapper,
                            @Value("${app.ai.chat.model:deepseek-v3}") String chatModelName) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.conversationMessageMapper = conversationMessageMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.chatModelName = chatModelName;
    }

    // 非流式问答

    @Override
    public String ask(String question, String sessionId, Long userId) {
        validateQuestion(question, sessionId);
        long start = System.currentTimeMillis();

        saveMessage(userId, sessionId, "user", question);
        try {
            List<EmbeddingMatch<TextSegment>> matches = retrieve(question, DEFAULT_TOP_K);
            List<ChatMessage> messages = buildMessages(question, sessionId, matches);
            ChatResponse response = chatLanguageModel.chat(messages);
            if (response == null || response.aiMessage() == null) {
                throw new RuntimeException("AI model returned empty response");
            }
            String answer = response.aiMessage().text();

            saveMessage(userId, sessionId, "assistant", answer);
            saveCallLog(userId, "SUCCESS", null, start, response.tokenUsage());
            return answer;
        } catch (Exception e) {
            saveCallLog(userId, "FAIL", e.getMessage(), start, null);
            throw new RuntimeException("RAG question answering failed", e);
        }
    }

    // 流式问答

    @Override
    public SseEmitter askStream(String question, String sessionId, Long userId) {
        SseEmitter emitter = new SseEmitter(180_000L);

        // 参数校验（提前 fail）
        try {
            validateQuestion(question, sessionId);
        } catch (Exception e) {
            sendEvent(emitter, "error", e.getMessage());
            emitter.complete();
            return emitter;
        }

        long start = System.currentTimeMillis();
        StringBuilder answer = new StringBuilder();
        saveMessage(userId, sessionId, "user", question);

        // 检索（同步，因为流式 Chat 前需要上下文）
        List<EmbeddingMatch<TextSegment>> matches;
        try {
            matches = retrieve(question, DEFAULT_TOP_K);
        } catch (Exception e) {
            saveCallLog(userId, "FAIL", "retrieve error: " + e.getMessage(), start, null);
            sendEvent(emitter, "error", "语义检索失败");
            emitter.complete();
            return emitter;
        }

        List<ChatMessage> messages = buildMessages(question, sessionId, matches);

        try {
            streamingChatLanguageModel.chat(messages, new StreamingChatResponseHandler() {
                @Override
                public void onPartialResponse(String token) {
                    answer.append(token);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("message")
                                .data(token));
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onCompleteResponse(ChatResponse chatResponse) {
                    // 保存 assistant 消息
                    saveMessage(userId, sessionId, "assistant", answer.toString());
                    // 保存调用日志（优先用 Response 里的 TokenUsage，否则估算）
                    TokenUsage usage = chatResponse != null ? chatResponse.tokenUsage() : null;
                    saveCallLog(userId, "SUCCESS", null, start, usage);

                    try {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(answer.toString()));
                        emitter.complete();
                    } catch (IOException e) {
                        emitter.completeWithError(e);
                    }
                }

                @Override
                public void onError(Throwable error) {
                    saveCallLog(userId, "FAIL", error.getMessage(), start, null);
                    emitter.completeWithError(error);
                }
            });
        } catch (Exception e) {
            saveCallLog(userId, "FAIL", e.getMessage(), start, null);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    // ==================== 历史查询 ====================

    @Override
    public List<ConversationMessage> getHistory(String sessionId) {
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId is required");
        }
        return conversationMessageMapper.selectBySessionId(sessionId.trim());
    }

    private List<EmbeddingMatch<TextSegment>> retrieve(String question, int maxResults) {
        Embedding questionEmbedding = embeddingModel.embed(question).content();
        EmbeddingSearchResult<TextSegment> result = embeddingStore.search(
                EmbeddingSearchRequest.builder()
                        .queryEmbedding(questionEmbedding)
                        .maxResults(maxResults)
                        .build());
        return result.matches();
    }

    private List<ChatMessage> buildMessages(String question,
                                            String sessionId,
                                            List<EmbeddingMatch<TextSegment>> matches) {
        String context = matches.stream()
                .map(EmbeddingMatch::embedded)
                .filter(segment -> segment != null && StringUtils.hasText(segment.text()))
                .map(TextSegment::text)
                .collect(Collectors.joining("\n---\n"));

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from("""
                You are a professional marine science assistant.
                Answer in Chinese. Use the provided context first.
                If the context does not contain enough information, say so clearly and give a cautious answer.
                Context:
                %s
                """.formatted(StringUtils.hasText(context) ? context : "No retrieved context.")));

        List<ConversationMessage> history = conversationMessageMapper.selectRecentBySessionId(sessionId, HISTORY_LIMIT);
        boolean skipNextAsToolResult = false;
        for (ConversationMessage item : history) {
            if (question.equals(item.getContent())) {
                continue;
            }
            String content = item.getContent();
            boolean hasContent = StringUtils.hasText(content);

            if ("assistant".equalsIgnoreCase(item.getRole())) {
                // 工具调用响应：text 为空 或 内容为 JSON 工具调用描述 → 下一条 user 是工具返回值
                if (looksLikeToolCall(content)) {
                    skipNextAsToolResult = true;
                    continue;
                }
                if (hasContent) {
                    messages.add(AiMessage.from(content));
                }
            } else if ("user".equalsIgnoreCase(item.getRole())) {
                if (skipNextAsToolResult) {
                    skipNextAsToolResult = false;
                    continue;
                }
                if (hasContent) {
                    messages.add(UserMessage.from(content));
                }
            }
        }
        messages.add(UserMessage.from(question));
        return messages;
    }

    /**
     * 判断消息内容是否像工具调用而非正常对话文本。
     * 工具调用通常以 JSON 形式出现，包含 tool_calls / function 等字段。
     */
    private boolean looksLikeToolCall(String content) {
        if (content == null || content.isBlank()) {
            return true;
        }
        String trimmed = content.trim();
        // 以 JSON 开头且包含工具调用特征字段
        if ((trimmed.startsWith("{") || trimmed.startsWith("["))
                && (trimmed.contains("\"tool_calls\"") || trimmed.contains("\"function\""))) {
            return true;
        }
        return false;
    }

    private void validateQuestion(String question, String sessionId) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("question is required");
        }
        if (!StringUtils.hasText(sessionId)) {
            throw new IllegalArgumentException("sessionId is required");
        }
    }

    private void saveMessage(Long userId, String sessionId, String role, String content) {
        ConversationMessage message = new ConversationMessage();
        message.setUserId(userId);
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        conversationMessageMapper.insert(message);
    }

    private void saveCallLog(Long userId,
                             String status,
                             String errorMessage,
                             long startTime,
                             TokenUsage tokenUsage) {
        AiCallLog log = new AiCallLog();
        log.setUserId(userId);
        log.setScene("rag");
        log.setProvider("dashscope");
        log.setModelName(chatModelName);
        if (tokenUsage != null) {
            log.setPromptTokens(tokenUsage.inputTokenCount());
            log.setCompletionTokens(tokenUsage.outputTokenCount());
        }
        log.setLatencyMs(System.currentTimeMillis() - startTime);
        log.setStatus(status);
        log.setErrorMessage(errorMessage != null && errorMessage.length() > 500
                ? errorMessage.substring(0, 500)
                : errorMessage);
        aiCallLogMapper.insert(log);
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }
}
