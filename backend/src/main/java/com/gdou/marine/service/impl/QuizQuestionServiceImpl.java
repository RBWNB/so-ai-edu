package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.dto.AiGenerateDTO;
import com.gdou.marine.entity.KbDocument;
import com.gdou.marine.entity.QuizQuestion;
import com.gdou.marine.entity.Species;
import com.gdou.marine.mapper.KbDocumentMapper;
import com.gdou.marine.mapper.QuizQuestionMapper;
import com.gdou.marine.mapper.SpeciesMapper;
import com.gdou.marine.service.QuizQuestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题库 Service 实现
 */
@Service
public class QuizQuestionServiceImpl extends ServiceImpl<QuizQuestionMapper, QuizQuestion> implements QuizQuestionService {

    private static final Logger log = LoggerFactory.getLogger(QuizQuestionServiceImpl.class);

    private final ZhipuAiServiceImpl zhipuAiService;
    private final KbDocumentMapper kbDocumentMapper;
    private final SpeciesMapper speciesMapper;

    public QuizQuestionServiceImpl(ZhipuAiServiceImpl zhipuAiService,
                                   KbDocumentMapper kbDocumentMapper,
                                   SpeciesMapper speciesMapper) {
        this.zhipuAiService = zhipuAiService;
        this.kbDocumentMapper = kbDocumentMapper;
        this.speciesMapper = speciesMapper;
    }

    @Override
    public List<QuizQuestion> generateByAi(AiGenerateDTO dto, Long userId) {
        String sourceType = dto.getSourceType() != null ? dto.getSourceType() : "kb";
        String contentTitle;
        String content;

        if ("species".equals(sourceType)) {
            // 1a. 获取海洋百科物种内容
            Species species = speciesMapper.selectById(dto.getSpeciesId());
            if (species == null) {
                throw new RuntimeException("海洋百科物种不存在，ID=" + dto.getSpeciesId());
            }

            contentTitle = species.getChineseName();
            content = buildSpeciesContent(species);
        } else {
            // 1b. 获取知识库文档内容
            KbDocument doc = kbDocumentMapper.selectById(dto.getDocumentId());
            if (doc == null) {
                throw new RuntimeException("知识库文档不存在，ID=" + dto.getDocumentId());
            }

            content = doc.getContent();
            if (content == null || content.trim().isEmpty()) {
                throw new RuntimeException("知识库文档内容为空，无法生成题目");
            }
            contentTitle = doc.getTitle();
        }

        // 截取过长内容（防止token超限）
        String truncatedContent = content;
        if (content.length() > 8000) {
            truncatedContent = content.substring(0, 8000) + "\n\n...（内容已截断）";
        }

        String typeDesc = switch (dto.getQuestionType() != null ? dto.getQuestionType() : "mixed") {
            case "single" -> "单选题";
            case "multiple" -> "多选题";
            case "judge" -> "判断题";
            default -> "单选题、多选题、判断题混合";
        };

        String diffDesc = switch (dto.getDifficulty() != null ? dto.getDifficulty() : "normal") {
            case "easy" -> "简单";
            case "hard" -> "困难";
            default -> "普通";
        };

        int count = dto.getCount() != null ? Math.min(Math.max(dto.getCount(), 1), 10) : 5;

        // 2. 构建 AI 提示词
        String systemPrompt = "你是一名海洋知识教育专家，严格根据用户提供的知识库内容生成题目，不要编造内容。"
                + "请严格只返回JSON数组，不要返回额外说明或Markdown代码块。"
                + "每项包含字段："
                + "questionType (single/multiple/judge), "
                + "stem (题干), "
                + "options (选项数组，每个元素包含label和text字段，如{\"label\":\"A\",\"text\":\"...\"}), "
                + "answer (正确答案，单选题用A/B/C/D这种格式，多选题用A,B,C这种格式，判断题用正确或错误), "
                + "explanation (解析), "
                + "difficulty (easy/normal/hard)";

        String sourceLabel = "species".equals(sourceType) ? "海洋百科" : "知识库";
        String userPrompt = String.format("""
                请根据以下%s内容，生成%d道%s（难度：%s）。

                要求：
                1. 题目必须严格基于给定内容，不要编造内容
                2. 单选题（single）提供4个选项，只有一个正确答案，用A/B/C/D表示
                3. 多选题（multiple）提供4-5个选项，至少2个正确答案，用A/B/C/D/E表示
                4. 判断题（judge）判断对错，答案为正确或错误
                5. 每道题需包含解析（explanation）
                6. 正确选项字母用英文大写

                %s标题：%s
                %s内容：
                %s
                """, sourceLabel, count, typeDesc, diffDesc, sourceLabel, contentTitle, sourceLabel, truncatedContent);

        log.info("AI生成题目请求：sourceType={}, documentId={}, speciesId={}, count={}, type={}, difficulty={}",
                sourceType, dto.getDocumentId(), dto.getSpeciesId(), count, dto.getQuestionType(), dto.getDifficulty());

        // 3. 调用智谱 AI
        String aiResponse;
        try {
            aiResponse = zhipuAiService.callAI(userPrompt, systemPrompt);
        } catch (Exception e) {
            log.error("AI生成题目失败", e);
            throw new RuntimeException("AI生成题目失败：" + e.getMessage());
        }

        if (aiResponse == null || aiResponse.trim().isEmpty()) {
            throw new RuntimeException("AI返回结果为空");
        }

        log.debug("AI原始响应：{}", aiResponse);

        // 4. 解析 AI 响应
        KbDocument dummyDoc = new KbDocument();
        dummyDoc.setTitle(contentTitle);
        return parseQuestions(aiResponse, dummyDoc, userId);
    }

    /**
     * 将海洋百科物种的各字段拼接成纯文本内容
     */
    private String buildSpeciesContent(Species species) {
        StringBuilder sb = new StringBuilder();
        sb.append("中文名：").append(nullToEmpty(species.getChineseName())).append("\n");
        sb.append("学名：").append(nullToEmpty(species.getScientificName())).append("\n");
        sb.append("别名：").append(nullToEmpty(species.getAliasNames())).append("\n");
        sb.append("分类：")
                .append(nullToEmpty(species.getKingdom())).append(" / ")
                .append(nullToEmpty(species.getPhylum())).append(" / ")
                .append(nullToEmpty(species.getClassName())).append(" / ")
                .append(nullToEmpty(species.getOrderName())).append(" / ")
                .append(nullToEmpty(species.getFamilyName())).append(" / ")
                .append(nullToEmpty(species.getGenusName())).append("\n");
        sb.append("保护 status：").append(nullToEmpty(species.getConservationStatus())).append("\n");
        sb.append("栖息地：").append(nullToEmpty(species.getHabitat())).append("\n");
        sb.append("分布区域：").append(nullToEmpty(species.getDistributionArea())).append("\n");
        sb.append("形态特征：").append(nullToEmpty(species.getMorphologyDesc())).append("\n");
        sb.append("生活习性：").append(nullToEmpty(species.getHabitDesc())).append("\n");
        sb.append("趣味知识：").append(nullToEmpty(species.getFunFact())).append("\n");
        return sb.toString();
    }

    private String nullToEmpty(String s) {
        return s != null ? s : "";
    }

    /**
     * 解析AI返回的JSON字符串为题目列表
     */
    private List<QuizQuestion> parseQuestions(String aiResponse, KbDocument doc, Long userId) {
        // 尝试提取JSON数组（去掉可能的markdown代码标记）
        String jsonStr = aiResponse.trim();
        if (jsonStr.startsWith("```")) {
            // 去掉代码块标记
            jsonStr = jsonStr.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();
        }

        // 查找第一个 [ 和最后一个 ]
        int start = jsonStr.indexOf('[');
        int end = jsonStr.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start) {
            jsonStr = jsonStr.substring(start, end + 1);
        }

        List<QuizQuestion> questions = new ArrayList<>();

        try {
            // 使用简单的手工解析方式（避免引入JSON依赖冲突）
            // 按题目对象分割
            List<String> objects = splitJsonObjects(jsonStr);
            for (String obj : objects) {
                QuizQuestion q = parseSingleQuestion(obj, doc, userId);
                if (q != null) {
                    questions.add(q);
                }
            }
        } catch (Exception e) {
            log.error("解析AI生成的题目JSON失败，尝试使用正则二次提取", e);
            // 尝试使用正则逐题提取
            questions.addAll(extractByRegex(aiResponse, doc, userId));
        }

        return questions;
    }

    /**
     * 解析单个题目JSON对象
     */
    private QuizQuestion parseSingleQuestion(String jsonChunk, KbDocument doc, Long userId) {
        try {
            QuizQuestion q = new QuizQuestion();
            q.setCreatedBy(userId);
            q.setCreatedByAi((byte) 1);
            q.setStatus((byte) 1);

            // 提取 questionType
            q.setQuestionType(extractJsonString(jsonChunk, "questionType"));
            if (q.getQuestionType() == null) {
                q.setQuestionType("single");
            }

            // 提取 stem
            q.setStem(extractJsonString(jsonChunk, "stem"));

            // 提取 options
            String optionsArrayStr = extractJsonArray(jsonChunk, "options");
            if (optionsArrayStr != null) {
                q.setOptionsJson(optionsArrayStr);
            } else {
                // 判断题可能没有options
                if ("judge".equals(q.getQuestionType())) {
                    q.setOptionsJson("[{\"label\":\"A\",\"text\":\"正确\"},{\"label\":\"B\",\"text\":\"错误\"}]");
                }
            }

            // 提取 answer
            String answer = extractJsonString(jsonChunk, "answer");
            if (answer != null) {
                if ("judge".equals(q.getQuestionType())) {
                    if (answer.contains("正确") || answer.contains("对") || "A".equals(answer)) {
                        q.setAnswerJson("\"正确\"");
                    } else {
                        q.setAnswerJson("\"错误\"");
                    }
                } else {
                    q.setAnswerJson("\"" + answer + "\"");
                }
            }

            // 提取 explanation
            q.setExplanation(extractJsonString(jsonChunk, "explanation"));

            // 提取 difficulty
            String diff = extractJsonString(jsonChunk, "difficulty");
            q.setDifficulty(diff != null ? diff : "normal");

            // 设置知识点
            q.setKnowledgePoints("[\"" + doc.getTitle() + "\"]");

            if (q.getStem() == null || q.getStem().isEmpty()) {
                return null;
            }
            return q;
        } catch (Exception e) {
            log.warn("解析单个题目失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 用正则表达式逐题提取（当JSON解析失败时的备用方案）
     */
    private List<QuizQuestion> extractByRegex(String text, KbDocument doc, Long userId) {
        List<QuizQuestion> list = new ArrayList<>();

        // 按题号或"stem"分割
        Pattern pattern = Pattern.compile("\\{[^{}]*\"stem\"\\s*:\\s*\"([^\"]+)\"[^{}]*\\}", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            try {
                String block = matcher.group();
                QuizQuestion q = new QuizQuestion();
                q.setCreatedBy(userId);
                q.setCreatedByAi((byte) 1);
                q.setStatus((byte) 1);
                q.setKnowledgePoints("[\"" + doc.getTitle() + "\"]");

                // stem
                q.setStem(extractJsonString(block, "stem"));

                // questionType
                String type = extractJsonString(block, "questionType");
                q.setQuestionType(type != null ? type : "single");

                // options
                String opts = extractJsonArray(block, "options");
                if (opts != null) {
                    q.setOptionsJson(opts);
                }

                // answer
                String ans = extractJsonString(block, "answer");
                if (ans != null) {
                    q.setAnswerJson("\"" + ans + "\"");
                }

                // explanation
                q.setExplanation(extractJsonString(block, "explanation"));

                // difficulty
                String diff = extractJsonString(block, "difficulty");
                q.setDifficulty(diff != null ? diff : "normal");

                if (q.getStem() != null && !q.getStem().isEmpty()) {
                    list.add(q);
                }
            } catch (Exception e) {
                log.warn("正则提取题目失败: {}", e.getMessage());
            }
        }
        return list;
    }

    /**
     * 分割JSON数组为单个对象字符串
     */
    private List<String> splitJsonObjects(String jsonArray) {
        List<String> objects = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < jsonArray.length(); i++) {
            char c = jsonArray.charAt(i);
            if (c == '{') {
                if (depth == 0) {
                    start = i;
                }
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    objects.add(jsonArray.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return objects;
    }

    /**
     * 从JSON对象中提取指定字段的字符串值
     */
    private String extractJsonString(String json, String fieldName) {
        // 处理字符串值："fieldName":"value" 或 "fieldName": "value"
        Pattern p = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*\"([^\"]*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1).replace("\\\"", "\"").replace("\\n", "\n");
        }
        return null;
    }

    /**
     * 从JSON对象中提取指定字段的数组值
     */
    private String extractJsonArray(String json, String fieldName) {
        Pattern p = Pattern.compile("\"" + fieldName + "\"\\s*:\\s*(\\[[^\\]]*\\])", Pattern.DOTALL);
        Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
