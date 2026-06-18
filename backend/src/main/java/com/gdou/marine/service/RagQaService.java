package com.gdou.marine.service;

import com.gdou.marine.entity.ConversationMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description Rag问答Service类
 */
public interface RagQaService {

    String ask(String question, String sessionId, Long userId);

    SseEmitter askStream(String question, String sessionId, Long userId);

    List<ConversationMessage> getHistory(String sessionId);
}
