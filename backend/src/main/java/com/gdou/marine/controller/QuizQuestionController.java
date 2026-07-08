package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.dto.AiGenerateDTO;
import com.gdou.marine.dto.QuizQuestionDTO;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.service.TtsService;
import com.gdou.marine.service.impl.QuizQuestionServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 题库管理 Controller
 */
@RestController
@RequestMapping("/quiz")
public class QuizQuestionController {

    private static final Logger log = LoggerFactory.getLogger(QuizQuestionController.class);

    @Autowired
    private QuizQuestionServiceImpl quizQuestionService;

    @Autowired
    private TtsService ttsService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.gdou.marine.mapper.UserBookmarkMapper userBookmarkMapper;

    /**
     * 分页查询题目列表
     */
    @GetMapping("/question/page")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Page<QuizQuestion> getQuestionPage(
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String stem,
            @RequestParam(required = false) Byte createdByAi,
            @RequestParam(required = false) Byte status,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer current,
            @RequestParam(required = false) Integer size) {

        int pg = current != null ? current : pageNum;
        int ps = size != null ? size : pageSize;

        LambdaQueryWrapper<QuizQuestion> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(questionType)) {
            wrapper.eq(QuizQuestion::getQuestionType, questionType.trim());
        }
        if (StringUtils.hasText(difficulty)) {
            wrapper.eq(QuizQuestion::getDifficulty, difficulty.trim());
        }
        if (StringUtils.hasText(stem)) {
            wrapper.like(QuizQuestion::getStem, stem.trim());
        }
        if (createdByAi != null) {
            wrapper.eq(QuizQuestion::getCreatedByAi, createdByAi);
        }
        if (status != null) {
            wrapper.eq(QuizQuestion::getStatus, status);
        }

        wrapper.orderByDesc(QuizQuestion::getCreatedAt);
        return quizQuestionService.page(new Page<>(pg, ps), wrapper);
    }

    /**
     * 获取题目详情
     */
    @GetMapping("/question/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public QuizQuestion getQuestionById(@PathVariable Long id) {
        return quizQuestionService.getById(id);
    }

    /**
     * 人工新增题目
     */
    @Log(module = "题库管理", description = "新增题目")
    @PostMapping("/question")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> createQuestion(@RequestBody QuizQuestionDTO dto, Authentication auth) {
        QuizQuestion question = new QuizQuestion();
        question.setQuestionType(dto.getQuestionType());
        question.setStem(dto.getStem());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAnswerJson(dto.getAnswerJson());
        question.setExplanation(dto.getExplanation());
        question.setDifficulty(dto.getDifficulty() != null ? dto.getDifficulty() : "normal");
        // 空字符串转为 null，避免 MySQL JSON 列报错
        String kp = dto.getKnowledgePoints();
        question.setKnowledgePoints((kp == null || kp.isEmpty() || kp.isBlank()) ? null : kp);
        question.setSpeciesId(dto.getSpeciesId());
        question.setCreatedByAi((byte) 0);
        question.setStatus(dto.getStatus() != null ? dto.getStatus() : (byte) 1);

        // JwtAuthenticationFilter 将 principal 设为 Long userId
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            question.setCreatedBy(userId);
        }

        quizQuestionService.save(question);
        return Map.of("id", question.getId(), "message", "创建成功");
    }

    /**
     * 修改题目
     */
    @Log(module = "题库管理", description = "修改题目")
    @PutMapping("/question/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> updateQuestion(@PathVariable Long id, @RequestBody QuizQuestionDTO dto) {
        QuizQuestion question = new QuizQuestion();
        question.setId(id);
        question.setQuestionType(dto.getQuestionType());
        question.setStem(dto.getStem());
        question.setOptionsJson(dto.getOptionsJson());
        question.setAnswerJson(dto.getAnswerJson());
        question.setExplanation(dto.getExplanation());
        question.setDifficulty(dto.getDifficulty());
        String kp = dto.getKnowledgePoints();
        question.setKnowledgePoints((kp == null || kp.isEmpty() || kp.isBlank()) ? null : kp);
        question.setSpeciesId(dto.getSpeciesId());
        if (dto.getStatus() != null) {
            question.setStatus(dto.getStatus());
        }

        quizQuestionService.updateById(question);
        return Map.of("message", "更新成功");
    }

    /**
     * 删除题目
     */
    @Log(module = "题库管理", description = "删除题目")
    @DeleteMapping("/question/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> deleteQuestion(@PathVariable Long id) {
        quizQuestionService.removeById(id);
        // 级联删除：清理该题目的所有用户收藏记录（target_type=quiz_question）
        userBookmarkMapper.delete(
                new LambdaQueryWrapper<com.gdou.marine.entity.UserBookmark>()
                        .eq(com.gdou.marine.entity.UserBookmark::getTargetType, "quiz_question")
                        .eq(com.gdou.marine.entity.UserBookmark::getTargetId, id)
        );
        return Map.of("message", "删除成功");
    }

    /**
     * 切换题目状态（启用/禁用）
     */
    @Log(module = "题库管理", description = "切换题目状态")
    @PutMapping("/question/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> toggleStatus(@PathVariable Long id, @RequestBody Map<String, Byte> body) {
        Byte status = body.get("status");
        QuizQuestion question = new QuizQuestion();
        question.setId(id);
        question.setStatus(status);
        quizQuestionService.updateById(question);
        return Map.of("message", status == 1 ? "已启用" : "已禁用");
    }

    /**
     * AI 生成题目（预览，不保存）
     */
    @Log(module = "题库管理", description = "AI生成题目")
    @PostMapping("/question/ai-generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> aiGenerate(@RequestBody AiGenerateDTO dto, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = null;
            if (auth != null && auth.getPrincipal() instanceof Long id) {
                userId = id;
            }

            List<QuizQuestion> questions = quizQuestionService.generateByAi(dto, userId);
            result.put("success", true);
            result.put("data", questions);
            result.put("message", "生成成功，共" + questions.size() + "道题目");
        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            result.put("success", false);
            result.put("message", "生成失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 批量保存AI生成的题目
     */
    @Log(module = "题库管理", description = "批量保存AI生成的题目")
    @PostMapping("/question/batch-save")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> batchSave(@RequestBody List<QuizQuestion> questions) {
        int savedCount = 0;
        int speciesCount = 0;
        int docCount = 0;

        for (QuizQuestion q : questions) {
            // 记录原始数据（用于调试）
            log.info("📥 接收题目数据: id={}, stem={}, speciesId={}, sourceDocumentId={}",
                    q.getId(),
                    q.getStem() != null ? q.getStem().substring(0, Math.min(q.getStem().length(), 30)) : null,
                    q.getSpeciesId(),
                    q.getSourceDocumentId());

            q.setId(null);  // 清除ID让数据库自动生成

            if (q.getStatus() == null) {
                q.setStatus((byte) 1);
            }
            if (q.getCreatedByAi() == null) {
                q.setCreatedByAi((byte) 1);
            }

            // 🌟 关键：统计并验证来源信息
            if (q.getSpeciesId() != null) {
                speciesCount++;
                log.info("✅ 题目将关联物种: speciesId={}", q.getSpeciesId());
            }
            if (q.getSourceDocumentId() != null) {
                docCount++;
                log.info("✅ 题目将关联文档: sourceDocumentId={}", q.getSourceDocumentId());
            }

            if (q.getSpeciesId() == null && q.getSourceDocumentId() == null && q.getCreatedByAi() == 1) {
                log.warn("⚠️ AI生成的题目缺少来源信息! stem={}",
                        q.getStem() != null ? q.getStem().substring(0, Math.min(q.getStem().length(), 50)) : null);
            }

            savedCount++;
        }

        quizQuestionService.saveBatch(questions);

        log.info("📊 批量保存完成: 总数={}, 物种出题={}, RAG出题={}, 无来源={}",
                savedCount, speciesCount, docCount, (savedCount - speciesCount - docCount));

        return Map.of(
                "success", true,
                "message", "成功保存" + savedCount + "道题目（物种出题:" + speciesCount + ", RAG出题:" + docCount + "）",
                "count", savedCount,
                "speciesCount", speciesCount,
                "documentCount", docCount
        );
    }

    /**
     * TTS 语音合成：念出题目及选项
     *
     * @param body { questionId, stem, optionsJson, voiceType }
     * @return { success, url, message }
     */
    @Log(module = "题库管理", description = "TTS语音合成")
    @PostMapping("/question/tts")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public Map<String, Object> synthesizeSpeech(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 拼接文本
            StringBuilder textBuilder = new StringBuilder();
            String stem = (String) body.getOrDefault("stem", "");
            if (!stem.isEmpty()) {
                textBuilder.append(stem).append("。");
            }

            Object optionsJson = body.get("optionsJson");
            if (optionsJson instanceof String json && !json.isEmpty()) {
                try {
                    List<Map<String, String>> options = objectMapper.readValue(json, List.class);
                    for (int i = 0; i < options.size(); i++) {
                        Map<String, String> opt = options.get(i);
                        String label = opt.getOrDefault("label", "");
                        String text = opt.getOrDefault("text", "");
                        textBuilder.append(label).append("：").append(text).append("。");
                    }
                } catch (Exception e) {
                    log.warn("解析选项JSON失败，跳过选项", e);
                }
            }

            String text = textBuilder.toString();
            String voiceType = (String) body.get("voiceType");

            String url = ttsService.synthesize(text, voiceType);
            result.put("success", true);
            result.put("url", url);
            result.put("message", "语音合成成功");
        } catch (Exception e) {
            log.error("TTS语音合成失败", e);
            result.put("success", false);
            result.put("message", "语音合成失败：" + e.getMessage());
        }
        return result;
    }
}