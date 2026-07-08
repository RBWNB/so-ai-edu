package com.gdou.marine.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.entity.QuizAttempt;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.mapper.QuizAttemptMapper;
import com.gdou.marine.mapper.QuizQuestionMapper;
import com.gdou.marine.service.TaskProgressService;
import com.gdou.marine.service.TtsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * C 端答题闯关 Controller
 */
@RestController
@RequestMapping("/exam")
public class ExamController {

    private static final Logger log = LoggerFactory.getLogger(ExamController.class);

    @Autowired
    private QuizQuestionMapper quizQuestionMapper;

    @Autowired
    private QuizAttemptMapper quizAttemptMapper;

    @Autowired
    private TtsService ttsService;

    @Autowired
    private TaskProgressService taskProgressService;

    /**
     * 开始答题：随机抽取 30 道已启用的题目（不返回答案）
     */
    @GetMapping("/start")
    public Map<String, Object> startExam(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 查询所有已启用的题目
            LambdaQueryWrapper<QuizQuestion> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(QuizQuestion::getStatus, 1);
            wrapper.orderByDesc(QuizQuestion::getCreatedAt);
            List<QuizQuestion> allQuestions = quizQuestionMapper.selectList(wrapper);

            if (allQuestions.isEmpty()) {
                result.put("success", false);
                result.put("message", "题库暂无题目，请等待管理员添加");
                return result;
            }

            // 随机打乱
            Collections.shuffle(allQuestions, new Random());

            // 取最多 30 道
            int takeCount = Math.min(allQuestions.size(), 30);
            List<QuizQuestion> selected = allQuestions.subList(0, takeCount);

            // 给前端的问题：去掉答案，增加序号
            List<Map<String, Object>> questionList = new ArrayList<>();
            for (int i = 0; i < selected.size(); i++) {
                QuizQuestion q = selected.get(i);
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("index", i + 1);
                item.put("id", q.getId());
                item.put("questionType", q.getQuestionType());
                item.put("stem", q.getStem());
                item.put("optionsJson", q.getOptionsJson());
                item.put("difficulty", q.getDifficulty());
                item.put("speciesId", q.getSpeciesId());  // 物种ID，用于关联知识库收藏
                item.put("sourceDocumentId", q.getSourceDocumentId());  // 🌟 来源文档ID（RAG出题时）
                // 不返回 answerJson、explanation
                questionList.add(item);
            }

            result.put("success", true);
            result.put("data", questionList);
            result.put("total", questionList.size());
            result.put("message", "共抽取 " + questionList.size() + " 道题目");
        } catch (Exception e) {
            log.error("获取答题题目失败", e);
            result.put("success", false);
            result.put("message", "获取题目失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 提交答案 & 评分
     */
    @Log(module = "答题模块", description = "提交答案")
    @PostMapping("/submit")
    public Map<String, Object> submitExam(@RequestBody Map<String, Object> body, Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = auth != null && auth.getPrincipal() instanceof Long uid ? uid : null;
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> answers = (List<Map<String, Object>>) body.get("answers");
            if (answers == null || answers.isEmpty()) {
                result.put("success", false);
                result.put("message", "请至少回答一道题");
                return result;
            }

            int correctCount = 0;
            int totalCount = answers.size();
            List<Map<String, Object>> details = new ArrayList<>();

            for (Map<String, Object> ans : answers) {
                Long questionId = Long.valueOf(ans.get("questionId").toString());
                String userAnswer = (String) ans.get("userAnswer");

                // 从数据库获取题目
                QuizQuestion q = quizQuestionMapper.selectById(questionId);
                if (q == null) continue;

                // 比对答案
                String correctAnswer = "";
                try {
                    String answerJson = q.getAnswerJson();
                    if (answerJson != null) {
                        // answerJson 存储为 "\"A\"" 或 "\"正确\"" 格式，需要去引号
                        if (answerJson.startsWith("\"") && answerJson.endsWith("\"")) {
                            correctAnswer = answerJson.substring(1, answerJson.length() - 1);
                        } else {
                            correctAnswer = answerJson;
                        }
                    }
                } catch (Exception e) {
                    correctAnswer = q.getAnswerJson() != null ? q.getAnswerJson().replace("\"", "") : "";
                }

                boolean isCorrect = compareAnswer(userAnswer, correctAnswer, q.getQuestionType());

                // 保存答题记录（userAnswerJson 必须是合法 JSON）
                QuizAttempt attempt = new QuizAttempt();
                attempt.setUserId(userId);
                attempt.setQuestionId(questionId);
                attempt.setUserAnswerJson(userAnswer != null ? "\"" + userAnswer + "\"" : null);
                attempt.setIsCorrect((byte) (isCorrect ? 1 : 0));
                attempt.setTimeSpentSeconds(0);
                attempt.setAttemptedAt(java.time.LocalDateTime.now());
                quizAttemptMapper.insert(attempt);

                if (isCorrect) correctCount++;

                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("questionId", questionId);
                detail.put("userAnswer", userAnswer);
                detail.put("correctAnswer", correctAnswer);
                detail.put("correct", isCorrect);
                detail.put("explanation", q.getExplanation());
                detail.put("speciesId", q.getSpeciesId());  // 返回物种ID，用于关联知识库收藏
                details.add(detail);
            }

            int score = (int) Math.round((double) correctCount / totalCount * 100);

            // 更新每日任务进度：答对 N 题
            if (userId != null && correctCount > 0) {
                taskProgressService.incrementProgress(userId, "daily_quiz", correctCount);
            }

            result.put("success", true);
            result.put("score", score);
            result.put("correctCount", correctCount);
            result.put("totalCount", totalCount);
            result.put("details", details);
            result.put("message", "答对 " + correctCount + "/" + totalCount + " 题，得分 " + score + " 分");
        } catch (Exception e) {
            log.error("提交答案失败", e);
            result.put("success", false);
            result.put("message", "提交失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * TTS 语音合成（C 端使用）
     */
    @PostMapping("/tts")
    public Map<String, Object> synthesizeSpeech(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            StringBuilder textBuilder = new StringBuilder();
            String stem = (String) body.getOrDefault("stem", "");
            if (!stem.isEmpty()) {
                textBuilder.append(stem).append("。");
            }

            Object optionsJson = body.get("optionsJson");
            if (optionsJson instanceof String json && !json.isEmpty()) {
                try {
                    List<Map<String, String>> options = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, List.class);
                    for (int i = 0; i < options.size(); i++) {
                        Map<String, String> opt = options.get(i);
                        String label = opt.getOrDefault("label", "");
                        String text = opt.getOrDefault("text", "");
                        textBuilder.append(label).append("：").append(text).append("。");
                    }
                } catch (Exception e) {
                    log.warn("解析选项JSON失败", e);
                }
            }

            String url = ttsService.synthesize(textBuilder.toString(), null);
            result.put("success", true);
            result.put("url", url);
            result.put("message", "语音合成成功");
        } catch (Exception e) {
            log.error("TTS 合成失败", e);
            result.put("success", false);
            result.put("message", "语音合成失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 答题历史
     */
    @GetMapping("/history")
    public Map<String, Object> getHistory(Authentication auth) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = auth != null && auth.getPrincipal() instanceof Long uid ? uid : null;
            if (userId == null) {
                result.put("success", false);
                result.put("message", "请先登录");
                return result;
            }

            // 按 attempted_at 分组，统计每次答题的得分
            List<QuizAttempt> attempts = quizAttemptMapper.selectList(
                    new LambdaQueryWrapper<QuizAttempt>()
                            .eq(QuizAttempt::getUserId, userId)
                            .orderByDesc(QuizAttempt::getAttemptedAt)
                            .last("LIMIT 200")
            );

            // 按 attempted_at 分组（精确到分钟）
            Map<String, List<QuizAttempt>> groups = new LinkedHashMap<>();
            for (QuizAttempt att : attempts) {
                String key = att.getAttemptedAt() != null
                        ? att.getAttemptedAt().toString().substring(0, 16)
                        : "unknown";
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(att);
            }

            List<Map<String, Object>> historyList = new ArrayList<>();
            for (Map.Entry<String, List<QuizAttempt>> entry : groups.entrySet()) {
                List<QuizAttempt> group = entry.getValue();
                long correct = group.stream().filter(a -> a.getIsCorrect() == 1).count();
                int total = group.size();
                int score = total > 0 ? (int) Math.round((double) correct / total * 100) : 0;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("time", entry.getKey());
                item.put("total", total);
                item.put("correct", correct);
                item.put("score", score);
                historyList.add(item);
            }

            result.put("success", true);
            result.put("data", historyList);
        } catch (Exception e) {
            log.error("获取答题历史失败", e);
            result.put("success", false);
            result.put("message", "获取历史失败：" + e.getMessage());
        }
        return result;
    }

    /**
     * 比较用户答案和正确答案
     */
    private boolean compareAnswer(String userAnswer, String correctAnswer, String questionType) {
        if (userAnswer == null || correctAnswer == null) return false;

        String ua = userAnswer.trim().toUpperCase();
        String ca = correctAnswer.trim().toUpperCase();

        if ("judge".equals(questionType)) {
            // 判断题：正确/错误、对/错、A/B 都算有效输入
            if (ua.equals("正确") || ua.equals("对") || ua.equals("A")) {
                return ca.contains("正确") || ca.equals("A");
            }
            if (ua.equals("错误") || ua.equals("错") || ua.equals("B")) {
                return ca.contains("错误") || ca.equals("B");
            }
            return ua.equals(ca);
        }

        if ("multiple".equals(questionType)) {
            // 多选题：比较集合是否相同（不区分顺序）
            Set<String> userSet = new HashSet<>(Arrays.asList(ua.split(",")));
            Set<String> correctSet = new HashSet<>(Arrays.asList(ca.split(",")));
            return userSet.equals(correctSet);
        }

        // 单选题
        return ua.equals(ca);
    }
}
