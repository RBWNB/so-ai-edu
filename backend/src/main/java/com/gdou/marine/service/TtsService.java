package com.gdou.marine.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gdou.marine.entity.VoiceAudioCache;
import com.gdou.marine.mapper.VoiceAudioCacheMapper;
import com.gdou.marine.utils.QiniuUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * TTS 语音合成服务（使用小米 MiMo-V2.5-TTS API + 七牛云存储）
 */
@Service
public class TtsService {

    private static final Logger log = LoggerFactory.getLogger(TtsService.class);

    // MiMo TTS API（Chat Completions 格式）
    private static final String MIMO_API_URL = "https://api.xiaomimimo.com/v1/chat/completions";

    @org.springframework.beans.factory.annotation.Value("${app.tts.api-key:}")
    private String mimoApiKey;
    private static final String MODEL = "mimo-v2.5-tts";
    private static final String DEFAULT_VOICE = "mimo_default";
    private static final String AUDIO_FORMAT = "wav";
    private static final String TTS_ENGINE = "mimo-v2.5-tts";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final VoiceAudioCacheMapper voiceAudioCacheMapper;

    public TtsService(ObjectMapper objectMapper, VoiceAudioCacheMapper voiceAudioCacheMapper) {
        this.objectMapper = objectMapper;
        this.voiceAudioCacheMapper = voiceAudioCacheMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 合成语音并返回七牛云音频 URL
     *
     * @param text      要合成的文本
     * @param voiceType 音色（null 使用默认 longxiaoxia）
     * @return 七牛云音频文件 URL
     */
    public String synthesize(String text, String voiceType) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("要合成的文本不能为空");
        }

        String cleanText = text.trim();
        String voice = voiceType != null ? voiceType : DEFAULT_VOICE;
        String textHash = md5(cleanText);

        // 1. 查缓存
        VoiceAudioCache cached = voiceAudioCacheMapper.selectOne(
                new LambdaQueryWrapper<VoiceAudioCache>()
                        .eq(VoiceAudioCache::getTextHash, textHash)
                        .eq(VoiceAudioCache::getVoiceType, voice)
                        .last("LIMIT 1")
        );
        if (cached != null) {
            String url = QiniuUtils.getFileUrl(cached.getTextHash() + "." + AUDIO_FORMAT);
            log.debug("TTS 缓存命中：textHash={}", textHash);
            return url;
        }

        // 2. 调用 DashScope CosyVoice API
        byte[] audioBytes;
        try {
            audioBytes = callMimoTts(cleanText, voice);
        } catch (Exception e) {
            log.error("MiMo TTS 调用失败", e);
            throw new RuntimeException("语音合成失败：" + e.getMessage());
        }

        // 3. 上传到七牛云
        String fileName = textHash + "." + AUDIO_FORMAT;
        String qiniuUrl;
        try {
            qiniuUrl = QiniuUtils.upload2Qiniu(audioBytes, fileName);
        } catch (Exception e) {
            log.error("七牛云上传失败", e);
            throw new RuntimeException("语音上传失败：" + e.getMessage());
        }

        // 4. 写入缓存
        try {
            VoiceAudioCache cache = new VoiceAudioCache();
            cache.setTextHash(textHash);
            cache.setSourceText(cleanText);
            cache.setVoiceType(voice);
            cache.setTtsEngine(TTS_ENGINE);
            voiceAudioCacheMapper.insert(cache);
        } catch (Exception e) {
            log.warn("TTS 缓存写入失败（不影响使用）", e);
        }

        return qiniuUrl;
    }

    /**
     * 调用 MiMo TTS API (Chat Completions 格式)
     * POST https://api.xiaomimimo.com/v1/chat/completions
     * Header: api-key: <key>
     * Body: { "model": "mimo-v2.5-tts", "messages": [...], "audio": { "format": "wav", "voice": "mimo_default" } }
     * 响应 JSON: choices[0].message.audio.data 为 base64 编码的音频
     */
    private byte[] callMimoTts(String text, String voice) throws Exception {
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", MODEL);

        ArrayNode messages = objectMapper.createArrayNode();

        // user 消息：风格控制（可选）
        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", "请用清晰、自然的普通话朗读，语速适中，语调自然。");
        messages.add(userMsg);

        // assistant 消息：待合成的文本
        ObjectNode assistantMsg = objectMapper.createObjectNode();
        assistantMsg.put("role", "assistant");
        assistantMsg.put("content", text);
        messages.add(assistantMsg);

        requestBody.set("messages", messages);

        // audio 参数
        ObjectNode audioNode = objectMapper.createObjectNode();
        audioNode.put("format", AUDIO_FORMAT);
        audioNode.put("voice", voice);
        requestBody.set("audio", audioNode);

        String requestBodyStr = objectMapper.writeValueAsString(requestBody);
        log.info("MiMo TTS 请求: model={}, voice={}, textLen={}", MODEL, voice, text.length());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(MIMO_API_URL))
                .header("Content-Type", "application/json")
                .header("api-key", mimoApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBodyStr))
                .timeout(Duration.ofSeconds(60))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("MiMo TTS 请求失败, status={}, body={}", response.statusCode(), response.body());
            throw new RuntimeException("TTS 服务返回错误，状态码: " + response.statusCode() + "，详情: " + response.body());
        }

        // 解析 JSON 响应，提取 base64 音频数据
        JsonNode root = objectMapper.readTree(response.body());
        String audioBase64 = root.path("choices").path(0).path("message").path("audio").path("data").asText();

        if (audioBase64.isEmpty()) {
            log.error("MiMo TTS 响应中未找到音频数据, body={}", response.body());
            throw new RuntimeException("TTS 响应异常：未返回音频数据");
        }

        return Base64.getDecoder().decode(audioBase64);
    }

    /**
     * 计算 MD5 哈希
     */
    private String md5(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return UUID.nameUUIDFromBytes(text.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString().replace("-", "");
        }
    }
}
