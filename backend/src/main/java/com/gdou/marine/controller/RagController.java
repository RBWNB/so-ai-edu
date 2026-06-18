package com.gdou.marine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.KbDocument;
import com.gdou.marine.entity.Result;
import com.gdou.marine.service.KnowledgeBaseService;
import com.gdou.marine.service.RagQaService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/17
 * @Description RAG 智能问答 + 知识库管理控制器。
 * 前端 baseURL=/api，Vite 代理剥离 /api 前缀后到达本控制器。
 */
@RestController
public class RagController {

    private final RagQaService ragQaService;
    private final KnowledgeBaseService knowledgeBaseService;

    public RagController(RagQaService ragQaService, KnowledgeBaseService knowledgeBaseService) {
        this.ragQaService = ragQaService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    // RAG 问答

    @Log(module = "RAG智能问答", description = "非流式问答")
    @PostMapping("/rag/ask")
    public Result ask(@RequestBody Map<String, Object> body) {
        String question = stringValue(body.get("question"));
        String sessionId = stringValue(body.get("sessionId"));
        if (!StringUtils.hasText(sessionId)) {
            sessionId = stringValue(body.get("session_id"));
        }

        try {
            String answer = ragQaService.ask(question, sessionId, getCurrentUserId());
            return new Result(true, "success", answer);
        } catch (Exception e) {
            return new Result(false, e.getMessage());
        }
    }

    @Log(module = "RAG智能问答", description = "流式问答")
    @PostMapping(value = "/rag/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter askStream(@RequestBody Map<String, Object> body) {
        String question = stringValue(body.get("question"));
        String sessionId = stringValue(body.get("sessionId"));
        if (!StringUtils.hasText(sessionId)) {
            sessionId = stringValue(body.get("session_id"));
        }
        Long userId = getCurrentUserId();

        // 直接委托给 Service，Service 内部已完成 SSE 事件推送 + 对话保存 + 调用日志
        return ragQaService.askStream(question, sessionId, userId);
    }

    @GetMapping("/rag/history/{sessionId}")
    public Result history(@PathVariable String sessionId) {
        try {
            return new Result(true, "success", ragQaService.getHistory(sessionId));
        } catch (Exception e) {
            return new Result(false, e.getMessage());
        }
    }

    // 知识库管理

    @GetMapping("/kb/documents")
    public Result pageDocuments(@RequestParam(required = false) Long categoryId,
                                @RequestParam(required = false) String keyword,
                                @RequestParam(defaultValue = "1") Integer pageNum,
                                @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<KbDocument> page = knowledgeBaseService.pageDocuments(categoryId, keyword, pageNum, pageSize);
        return new Result(true, "success", page);
    }

    @Log(module = "知识库管理", description = "上传知识库文档")
    @PostMapping("/kb/documents/upload")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result uploadDocument(@RequestParam("file") MultipartFile file,
                                 @RequestParam(required = false) Long categoryId) {
        try {
            KbDocument document = knowledgeBaseService.uploadDocument(file, categoryId, getCurrentUserId());
            return new Result(true, "upload success", document);
        } catch (Exception e) {
            return new Result(false, e.getMessage());
        }
    }

    @Log(module = "知识库管理", description = "创建知识库文档")
    @PostMapping("/kb/documents")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result createDocument(@RequestBody KbDocument document) {
        try {
            KbDocument saved = knowledgeBaseService.createDocument(document, getCurrentUserId());
            return new Result(true, "create success", saved);
        } catch (Exception e) {
            return new Result(false, e.getMessage());
        }
    }

    @Log(module = "知识库管理", description = "删除知识库文档")
    @DeleteMapping("/kb/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Result deleteDocument(@PathVariable Long id) {
        boolean deleted = knowledgeBaseService.deleteDocument(id);
        return new Result(deleted, deleted ? "delete success" : "document not found");
    }

    // 辅助方法

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Integer id) {
            return id.longValue();
        }
        if (principal instanceof String value) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }
}
