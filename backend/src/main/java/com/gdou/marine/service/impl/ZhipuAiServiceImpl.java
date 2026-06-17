package com.gdou.marine.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gdou.marine.config.ZhipuAiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

@Service
public class ZhipuAiServiceImpl {

    private static final Logger log = LoggerFactory.getLogger(ZhipuAiServiceImpl.class);

    private static final String IDENTIFY_SYSTEM_PROMPT =
            "你是一个专业的海洋生物图像识别专家。"
                    + "请识别图片中的海洋生物，返回最可能的物种信息。"
                    + "请严格只返回 JSON，不要返回额外说明或 Markdown。"
                    + "JSON 格式固定为："
                    + "{\"speciesName\":\"中文名称\",\"scientificName\":\"学名\",\"confidence\":0.95,\"summary\":\"详细介绍该物种的特点，包括形态特征、生活习性、分布区域和保护 status，不少于150字\"}。"
                    + "confidence 为 0-1 之间的可信度分数；"
                    + "如果图片中不是海洋生物或无法识别，将 speciesName 设为\"无法识别\"，confidence 设为 0。";

    private static final String SPECIES_SUGGEST_PROMPT =
            "你是一个专业的海洋生物专家。根据用户提供的海洋生物中文名，推荐该物种的详细信息。"
                    + "请严格只返回 JSON，不要返回额外说明或 Markdown。"
                    + "JSON 格式固定为："
                    + "{\"scientificName\":\"学名\",\"kingdom\":\"动物界\",\"phylum\":\"脊索动物门\",\"className\":\"哺乳纲\","
                    + "\"orderName\":\"鲸偶蹄目\",\"familyName\":\"海豚科\",\"genusName\":\"驼海豚属\",\"speciesName\":\"种小名\","
                    + "\"conservationStatus\":\"VU\",\"habitat\":\"近岸浅海\",\"distributionArea\":\"南海沿岸\","
                    + "\"morphologyDesc\":\"形态特征\",\"habitDesc\":\"生活习性\"}"
                    + "。如果不确定某个字段，设为空字符串。";

    private static final String ECOSYSTEM_SUGGEST_PROMPT =
            "你是一个专业的海洋生态专家。根据用户提供的海洋生态系统名称，推荐该生态系统的详细信息。"
                    + "请严格只返回 JSON，不要返回额外说明或 Markdown。"
                    + "JSON 格式固定为："
                    + "{\"description\":\"生态系统描述\",\"typicalSpecies\":\"典型物种列表\",\"threats\":\"主要威胁\"}"
                    + "。如果不确定某个字段，设为空字符串。";

    private static final String CHAT_SYSTEM_PROMPT =
            "你是一个专业的海洋生物知识助手，擅长回答海洋生态、海洋物种识别、海洋保护、海洋环境等相关问题。"
                    + "请用中文回答用户的问题，回答应专业、准确、简洁。"
                    + "如果用户的问题与海洋生物无关，请礼貌地引导回海洋生物相关话题。";

    private static final String SPECIES_COMPLETION_SYSTEM_PROMPT =
            "你是一个专业的海洋生物鉴定与科普助手。"
                    + "当用户输入海洋生物名称时，请补全并返回该物种的形态特征与生活习性。"
                    + "请严格只返回 JSON，不要返回额外说明或 Markdown。"
                    + "JSON 格式固定为："
                    + "{\"morphology_features\":\"...\",\"living_habits\":\"...\"}。"
                    + "要求内容专业、简洁、可用于数据库字段填充；"
                    + "如果信息不确定，请明确写出“不确定”并给出保守描述。";

    public static final String PROVIDER_ZHIPU = "zhipu";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ZhipuAiConfig config;

    public ZhipuAiServiceImpl(ObjectMapper objectMapper, ZhipuAiConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build();
    }

    // ==================== 图像识别 ====================

    /**
     * 基于多模态模型识别上传图片中的海洋生物。
     */
    public ImageIdentifyResult identifyImage(MultipartFile file) throws Exception {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("请上传图片文件");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            contentType = "image/jpeg";
        }

        byte[] fileBytes = file.getBytes();
        String base64Image = Base64.getEncoder().encodeToString(fileBytes);
        String dataUrl = "data:" + contentType + ";base64," + base64Image;

        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", config.getImageModel());
        requestBody.put("stream", false);

        ArrayNode messagesNode = objectMapper.createArrayNode();
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");

        ArrayNode contentArray = objectMapper.createArrayNode();

        ObjectNode textPart = objectMapper.createObjectNode();
        textPart.put("type", "text");
        textPart.put("text", IDENTIFY_SYSTEM_PROMPT);
        contentArray.add(textPart);

        ObjectNode imagePart = objectMapper.createObjectNode();
        imagePart.put("type", "image_url");
        ObjectNode imageUrlNode = objectMapper.createObjectNode();
        imageUrlNode.put("url", dataUrl);
        imagePart.set("image_url", imageUrlNode);
        contentArray.add(imagePart);

        userMessage.set("content", contentArray);
        messagesNode.add(userMessage);
        requestBody.set("messages", messagesNode);

        String requestBodyString = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getApiUrl() + "/chat/completions"))
                .header("Authorization", "Bearer " + config.getApiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("图像识别失败, statusCode={}, responseBody={}", response.statusCode(), response.body());
            throw new RuntimeException("AI识别失败，HTTP状态码: " + response.statusCode());
        }

        String content = extractContentFromResponse(response.body());
        return parseIdentifyResult(content);
    }

    // ==================== 对话 ====================

    /**
     * 非流式 AI 对话
     */
    public String chat(String message) {
        return chat(message, PROVIDER_ZHIPU);
    }

    public String chat(String message, String provider) {
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("消息不能为空");
        }
        if (provider == null) provider = PROVIDER_ZHIPU;
        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = buildChatRequestBody(message, false, modelName);
            String requestBodyString = objectMapper.writeValueAsString(requestBody);

            HttpResponse<String> response = sendAIRequest(requestBodyString, Duration.ofSeconds(120));

            if (response.statusCode() != 200) {
                log.error("AI聊天失败, statusCode={}, body={}", response.statusCode(), response.body());
                throw new RuntimeException("AI服务暂不可用");
            }

            return extractContentFromResponse(response.body());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI聊天异常", e);
            throw new RuntimeException("AI服务暂不可用", e);
        }
    }

    /**
     * 流式 AI 对话（SSE 打字机效果）
     */
    public void chatStream(String message, SseEmitter emitter) {
        chatStream(message, emitter, PROVIDER_ZHIPU);
    }

    public void chatStream(String message, SseEmitter emitter, String provider) {
        if (!StringUtils.hasText(message)) {
            try {
                emitter.send(SseEmitter.event().name("error").data("消息不能为空"));
                emitter.complete();
            } catch (IOException ignored) {
            }
            return;
        }
        if (provider == null) provider = PROVIDER_ZHIPU;

        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = buildChatRequestBody(message, true, modelName);
            String requestBodyString = objectMapper.writeValueAsString(requestBody);

            String apiUrl = config.getApiUrl() + "/chat/completions";
            String authToken = config.getApiKey();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                    .timeout(Duration.ofSeconds(120));

            if (authToken != null && !authToken.isEmpty()) {
                builder.header("Authorization", "Bearer " + authToken);
            }

            HttpRequest request = builder.build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        try (InputStream is = response.body();
                             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

                            if (response.statusCode() != 200) {
                                StringBuilder errorBody = new StringBuilder();
                                String errLine;
                                while ((errLine = reader.readLine()) != null) {
                                    errorBody.append(errLine);
                                }
                                log.error("AI流式聊天失败, statusCode={}, body={}", response.statusCode(), errorBody);
                                emitter.completeWithError(new RuntimeException("AI服务暂不可用"));
                                return;
                            }

                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                try {
                                    JsonNode jsonNode = objectMapper.readTree(data);
                                    String delta = jsonNode.path("choices").path(0)
                                            .path("delta").path("content").asText("");
                                    String reasoning = jsonNode.path("choices").path(0)
                                            .path("delta").path("reasoning_content").asText("");
                                    if (reasoning.isEmpty()) {
                                        reasoning = jsonNode.path("choices").path(0)
                                                .path("delta").path("reasoning").asText("");
                                    }
                                    if (!delta.isEmpty() || !reasoning.isEmpty()) {
                                        ObjectNode deltaJson = objectMapper.createObjectNode();
                                        if (!delta.isEmpty()) deltaJson.put("delta", delta);
                                        if (!reasoning.isEmpty()) deltaJson.put("reasoning", reasoning);
                                        emitter.send(objectMapper.writeValueAsString(deltaJson));
                                    }
                                } catch (Exception e) {
                                    log.warn("解析流式数据失败: {}", data, e);
                                }
                            }
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    })
                    .exceptionally(ex -> {
                        emitter.completeWithError(ex);
                        return null;
                    });
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    }

    // ==================== 物种智能推荐 ====================

    /**
     * 根据中文名 AI 推荐物种信息（用于新增物种时的智能填写）
     */
    public String suggestSpeciesInfo(String chineseName) {
        return suggestSpeciesInfo(chineseName, PROVIDER_ZHIPU);
    }

    public String suggestSpeciesInfo(String chineseName, String provider) {
        if (!StringUtils.hasText(chineseName)) {
            throw new IllegalArgumentException("中文名不能为空");
        }
        if (provider == null) provider = PROVIDER_ZHIPU;
        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            ArrayNode messagesNode = objectMapper.createArrayNode();
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", SPECIES_SUGGEST_PROMPT);
            messagesNode.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", "请推荐海洋生物【" + chineseName.trim() + "】的完整物种信息");
            messagesNode.add(userMessage);

            requestBody.set("messages", messagesNode);

            HttpResponse<String> response = sendAIRequest(objectMapper.writeValueAsString(requestBody), Duration.ofSeconds(60));

            if (response.statusCode() != 200) {
                log.error("AI推荐物种失败, chineseName={}, statusCode={}", chineseName, response.statusCode());
                throw new RuntimeException("AI服务暂不可用");
            }

            return extractContentFromResponse(response.body());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI推荐物种异常", e);
            throw new RuntimeException("AI推荐失败", e);
        }
    }

    // ==================== 生态系统智能推荐 ====================

    /**
     * 根据生态系统名称 AI 推荐详细信息
     */
    public String suggestEcosystemInfo(String ecosystemName) {
        return suggestEcosystemInfo(ecosystemName, PROVIDER_ZHIPU);
    }

    public String suggestEcosystemInfo(String ecosystemName, String provider) {
        if (!StringUtils.hasText(ecosystemName)) {
            throw new IllegalArgumentException("名称不能为空");
        }
        if (provider == null) provider = PROVIDER_ZHIPU;
        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            ArrayNode messagesNode = objectMapper.createArrayNode();
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", ECOSYSTEM_SUGGEST_PROMPT);
            messagesNode.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", "请推荐海洋生态系统【" + ecosystemName.trim() + "】的详细信息");
            messagesNode.add(userMessage);

            requestBody.set("messages", messagesNode);

            HttpResponse<String> response = sendAIRequest(objectMapper.writeValueAsString(requestBody), Duration.ofSeconds(60));

            if (response.statusCode() != 200) {
                log.error("AI推荐生态系统失败, name={}, statusCode={}", ecosystemName, response.statusCode());
                throw new RuntimeException("AI服务暂不可用");
            }

            return extractContentFromResponse(response.body());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI推荐生态系统异常", e);
            throw new RuntimeException("AI推荐失败", e);
        }
    }

    // ==================== 物种信息补全 ====================

    /**
     * 传入海洋生物名称，返回"形态特征 + 生活习性"补全结果。
     */
    public SpeciesCompletionResult completeSpeciesInfo(String speciesName) {
        return completeSpeciesInfo(speciesName, PROVIDER_ZHIPU);
    }

    public SpeciesCompletionResult completeSpeciesInfo(String speciesName, String provider) {
        if (!StringUtils.hasText(speciesName)) {
            throw new IllegalArgumentException("物种名称不能为空");
        }
        if (provider == null) provider = PROVIDER_ZHIPU;

        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            ArrayNode messagesNode = objectMapper.createArrayNode();
            ObjectNode systemMessage = objectMapper.createObjectNode();
            systemMessage.put("role", "system");
            systemMessage.put("content", SPECIES_COMPLETION_SYSTEM_PROMPT);
            messagesNode.add(systemMessage);

            ObjectNode userMessage = objectMapper.createObjectNode();
            userMessage.put("role", "user");
            userMessage.put("content", "请补全海洋生物" + speciesName.trim() + "的形态特征和生活习性。"
                    + "只输出 JSON：{\"morphology_features\":\"...\",\"living_habits\":\"...\"}");
            messagesNode.add(userMessage);

            requestBody.set("messages", messagesNode);

            HttpResponse<String> response = sendAIRequest(objectMapper.writeValueAsString(requestBody), Duration.ofSeconds(60));

            if (response.statusCode() != 200) {
                log.error("AI补全物种信息失败, speciesName={}, statusCode={}", speciesName, response.statusCode());
                throw new RuntimeException("调用AI失败，HTTP状态码: " + response.statusCode());
            }

            String content = extractContentFromResponse(response.body());
            SpeciesCompletionResult result = parseCompletionResult(content);
            result.setSpeciesName(speciesName.trim());
            return result;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI补全物种信息异常, speciesName={}", speciesName, e);
            throw new RuntimeException("调用AI失败，请稍后重试", e);
        }
    }

    // ==================== 私有工具方法 ====================

    private String resolveModelName(boolean isImage) {
        return isImage ? config.getImageModel() : config.getTextModel();
    }

    private HttpResponse<String> sendAIRequest(String requestBodyString, Duration timeout) throws Exception {
        String apiUrl = config.getApiUrl() + "/chat/completions";
        String authToken = config.getApiKey();

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                .timeout(timeout);

        if (authToken != null && !authToken.isEmpty()) {
            builder.header("Authorization", "Bearer " + authToken);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private ObjectNode buildChatRequestBody(String message, boolean stream, String modelName) {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", modelName);
        requestBody.put("stream", stream);

        ArrayNode messagesNode = objectMapper.createArrayNode();

        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", CHAT_SYSTEM_PROMPT);
        messagesNode.add(systemMessage);

        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", message);
        messagesNode.add(userMessage);

        requestBody.set("messages", messagesNode);
        return requestBody;
    }

    private String callAI(String userInput, String systemPrompt) {
        if (!StringUtils.hasText(userInput)) {
            throw new IllegalArgumentException("输入不能为空");
        }
        try {
            String modelName = resolveModelName(false);
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("model", modelName);
            requestBody.put("stream", false);

            ArrayNode messagesNode = objectMapper.createArrayNode();
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messagesNode.add(sysMsg);

            ObjectNode usrMsg = objectMapper.createObjectNode();
            usrMsg.put("role", "user");
            usrMsg.put("content", userInput);
            messagesNode.add(usrMsg);

            requestBody.set("messages", messagesNode);

            HttpResponse<String> response = sendAIRequest(objectMapper.writeValueAsString(requestBody), Duration.ofSeconds(60));

            if (response.statusCode() != 200) {
                log.error("AI调用失败, statusCode={}", response.statusCode());
                throw new RuntimeException("AI服务暂不可用");
            }

            return extractContentFromResponse(response.body());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI调用异常", e);
            throw new RuntimeException("AI调用失败", e);
        }
    }

    private String extractContentFromResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");

        if (contentNode.isMissingNode() || contentNode.isNull()) {
            log.error("AI响应异常：未返回内容, responseBody={}", responseBody);
            throw new RuntimeException("AI响应异常：未返回内容");
        }

        String content = contentNode.asText();
        if (!StringUtils.hasText(content)) {
            throw new RuntimeException("AI响应异常：返回内容为空");
        }
        return content;
    }

    private ImageIdentifyResult parseIdentifyResult(String rawContent) {
        try {
            String cleaned = removeMarkdownCodeFence(rawContent);
            JsonNode jsonNode = objectMapper.readTree(cleaned);

            ImageIdentifyResult result = new ImageIdentifyResult();
            result.setSpeciesName(jsonNode.path("speciesName").asText(""));
            result.setScientificName(jsonNode.path("scientificName").asText(""));
            result.setConfidence(jsonNode.path("confidence").asDouble(0.0));
            result.setSummary(jsonNode.path("summary").asText(""));
            result.setRawText(rawContent);
            return result;
        } catch (Exception e) {
            log.warn("解析识别结果失败，使用原文兜底。rawContent={}", rawContent, e);
            ImageIdentifyResult result = new ImageIdentifyResult();
            result.setSpeciesName(rawContent);
            result.setRawText(rawContent);
            return result;
        }
    }

    private SpeciesCompletionResult parseCompletionResult(String rawContent) {
        try {
            String cleaned = removeMarkdownCodeFence(rawContent);
            JsonNode jsonNode = objectMapper.readTree(cleaned);

            String morphologyFeatures = jsonNode.path("morphology_features").asText("");
            String livingHabits = jsonNode.path("living_habits").asText("");

            if (!StringUtils.hasText(morphologyFeatures) && !StringUtils.hasText(livingHabits)) {
                throw new RuntimeException("AI返回格式不符合预期：缺少 morphology_features/living_habits");
            }

            return new SpeciesCompletionResult(morphologyFeatures, livingHabits, rawContent);
        } catch (Exception e) {
            log.warn("解析AI返回内容失败，返回原文兜底。rawContent={}", rawContent, e);
            return new SpeciesCompletionResult("", rawContent, rawContent);
        }
    }

    private String removeMarkdownCodeFence(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    // ==================== 内部类 ====================

    public static class ImageIdentifyResult {
        private String speciesName;
        private String scientificName;
        private double confidence;
        private String summary;
        private String rawText;

        public ImageIdentifyResult() {}

        public String getSpeciesName() { return speciesName; }
        public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }
        public String getScientificName() { return scientificName; }
        public void setScientificName(String scientificName) { this.scientificName = scientificName; }
        public double getConfidence() { return confidence; }
        public void setConfidence(double confidence) { this.confidence = confidence; }
        public String getSummary() { return summary; }
        public void setSummary(String summary) { this.summary = summary; }
        public String getRawText() { return rawText; }
        public void setRawText(String rawText) { this.rawText = rawText; }
    }

    public static class SpeciesCompletionResult {
        private String speciesName;
        private String morphologyFeatures;
        private String livingHabits;
        private String rawContent;

        public SpeciesCompletionResult() {}

        public SpeciesCompletionResult(String morphologyFeatures, String livingHabits, String rawContent) {
            this.morphologyFeatures = morphologyFeatures;
            this.livingHabits = livingHabits;
            this.rawContent = rawContent;
        }

        public String getSpeciesName() { return speciesName; }
        public void setSpeciesName(String speciesName) { this.speciesName = speciesName; }
        public String getMorphologyFeatures() { return morphologyFeatures; }
        public void setMorphologyFeatures(String morphologyFeatures) { this.morphologyFeatures = morphologyFeatures; }
        public String getLivingHabits() { return livingHabits; }
        public void setLivingHabits(String livingHabits) { this.livingHabits = livingHabits; }
        public String getRawContent() { return rawContent; }
        public void setRawContent(String rawContent) { this.rawContent = rawContent; }
    }
}
