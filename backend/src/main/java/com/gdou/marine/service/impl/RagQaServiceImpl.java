package com.gdou.marine.service.impl;

import com.gdou.marine.dto.ChatSessionDTO;
import com.gdou.marine.entity.AiCallLog;
import com.gdou.marine.entity.ConversationMessage;
import com.gdou.marine.mapper.AiCallLogMapper;
import com.gdou.marine.mapper.ConversationMessageMapper;
import com.gdou.marine.service.RagQaService;
import com.gdou.marine.service.TaskProgressService;
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
    private static final double DEFAULT_MIN_SCORE = 0.75; // 相似度阈值
    private static final String NO_KNOWLEDGE_TIP = "非常抱歉，知识库中暂时没有找到与您问题匹配的内容，请换个问题提问awa";

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final ChatLanguageModel chatLanguageModel;
    private final StreamingChatLanguageModel streamingChatLanguageModel;
    private final ConversationMessageMapper conversationMessageMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final String chatModelName;
    private final TaskProgressService taskProgressService;

    public RagQaServiceImpl(EmbeddingModel embeddingModel,
                            EmbeddingStore<TextSegment> embeddingStore,
                            ChatLanguageModel chatLanguageModel,
                            StreamingChatLanguageModel streamingChatLanguageModel,
                            ConversationMessageMapper conversationMessageMapper,
                            AiCallLogMapper aiCallLogMapper,
                            TaskProgressService taskProgressService,
                            @Value("${app.ai.chat.model:deepseek-v3}") String chatModelName) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.chatLanguageModel = chatLanguageModel;
        this.streamingChatLanguageModel = streamingChatLanguageModel;
        this.conversationMessageMapper = conversationMessageMapper;
        this.aiCallLogMapper = aiCallLogMapper;
        this.taskProgressService = taskProgressService;
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

            //过滤低相似度，无匹配直接短路返回
            List<EmbeddingMatch<TextSegment>> validMatches = matches.stream()
                    .filter(match -> match.score() >= DEFAULT_MIN_SCORE)
                    .collect(Collectors.toList());

            if (validMatches.isEmpty()) {
                // 直接保存答复并返回，不调用大模型
                saveMessage(userId, sessionId, "assistant", NO_KNOWLEDGE_TIP);
                saveCallLog(userId, "SUCCESS", null, start, null);
                return NO_KNOWLEDGE_TIP;
            }

            List<ChatMessage> messages = buildMessages(question, sessionId, validMatches);
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

        List<EmbeddingMatch<TextSegment>> matches;
        try {
            matches = retrieve(question, DEFAULT_TOP_K);
        } catch (Exception e) {
            saveCallLog(userId, "FAIL", "retrieve error: " + e.getMessage(), start, null);
            sendEvent(emitter, "error", "语义检索失败");
            emitter.complete();
            return emitter;
        }

        // 过滤低相似度，无匹配直接短路返回
        List<EmbeddingMatch<TextSegment>> validMatches = matches.stream()
                .filter(match -> match.score() >= DEFAULT_MIN_SCORE)
                .collect(Collectors.toList());

        if (validMatches.isEmpty()) {
            saveMessage(userId, sessionId, "assistant", NO_KNOWLEDGE_TIP);
            saveCallLog(userId, "SUCCESS", null, start, null);
            sendEvent(emitter, "complete", NO_KNOWLEDGE_TIP);
            emitter.complete();
            return emitter;
        }

        List<ChatMessage> messages = buildMessages(question, sessionId, validMatches);

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
                    saveMessage(userId, sessionId, "assistant", answer.toString());
                    TokenUsage usage = chatResponse != null ? chatResponse.tokenUsage() : null;
                    saveCallLog(userId, "SUCCESS", null, start, usage);
                    try {
                        emitter.send(SseEmitter.event()
                                .name("complete")
                                .data(""));
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
    @Override
    public List<ChatSessionDTO> getSessionList(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("必须登录才能查看历史会话");
        }
        return conversationMessageMapper.selectSessionList(userId);
    }
    // 历史查询

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
            你是专业的海洋科学知识助手，必须严格遵守以下所有规则：
            1. 绝对禁止使用上下文以外的任何知识、常识、推断或脑补内容，所有回答必须完全来自下方【知识库上下文】。
            2. 如果上下文内容不足以回答问题，直接输出"知识库中暂无相关内容"，不得编造、补充、引申或道歉。
            3. 回答必须严格按照以下固定格式输出，不得增减模块、不得添加额外解释和客套语。

            ### 问题解答
            （基于上下文整理的清晰答案，分点表述，逻辑清晰）

            ### 参考依据
            （列出回答所依据的知识库原文核心要点）

            【知识库上下文】
            %s
            """.formatted(StringUtils.hasText(context) ? context : "无")));

        List<ConversationMessage> history = conversationMessageMapper.selectRecentBySessionId(sessionId, HISTORY_LIMIT);
        boolean skipNextAsToolResult = false;
        for (ConversationMessage item : history) {
            if (question.equals(item.getContent())) {
                continue;
            }
            String content = item.getContent();
            boolean hasContent = StringUtils.hasText(content);
            if ("assistant".equalsIgnoreCase(item.getRole())) {
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

        // 用户提问时更新每日任务进度
        if ("user".equals(role) && userId != null) {
            taskProgressService.incrementProgress(userId, "ask_ai");
        }
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
