package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.dto.AiGenerateDTO;
import com.gdou.marine.dto.QuizQuestionDTO;
import com.gdou.marine.entity.QuizQuestion;
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
        question.setKnowledgePoints(dto.getKnowledgePoints());
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
        question.setKnowledgePoints(dto.getKnowledgePoints());
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
        for (QuizQuestion q : questions) {
            q.setId(null);
            if (q.getStatus() == null) {
                q.setStatus((byte) 1);
            }
            if (q.getCreatedByAi() == null) {
                q.setCreatedByAi((byte) 1);
            }
        }
        quizQuestionService.saveBatch(questions);
        return Map.of("success", true, "message", "成功保存" + questions.size() + "道题目", "count", questions.size());
    }
}
