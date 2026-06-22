package com.gdou.marine.config;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.output.TokenUsage;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/18
 * @Description 通过 DashScope OpenAI 兼容接口 (/compatible-mode/v1/embeddings) 实现向量化，
 * 绕过 LangChain4j QwenEmbeddingModel 在 beta2 中的兼容性问题。
 */
public class DashScopeOpenAiEmbeddingModel implements EmbeddingModel {

    private static final Logger log = LoggerFactory.getLogger(DashScopeOpenAiEmbeddingModel.class);
    private static final String DEFAULT_BASE_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1";
    private static final Gson GSON = new Gson();

    private final String apiKey;
    private final String modelName;
    private final String baseUrl;
    private volatile Integer cachedDimension;

    public DashScopeOpenAiEmbeddingModel(String apiKey, String modelName, String baseUrl) {
        this.apiKey = apiKey;
        this.modelName = modelName;
        this.baseUrl = (baseUrl != null && !baseUrl.isBlank()) ? baseUrl : DEFAULT_BASE_URL;
    }

    @Override
    public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return Response.from(Collections.emptyList());
        }
        List<String> texts = segments.stream()
                .map(TextSegment::text)
                .toList();
        return callApi(texts);
    }

    private Response<List<Embedding>> callApi(List<String> texts) {
        String url = baseUrl + "/embeddings";
        JsonObject body = new JsonObject();
        body.addProperty("model", modelName);
        // OpenAI 兼容格式: input 是字符串或字符串数组
        if (texts.size() == 1) {
            body.addProperty("input", texts.get(0));
        } else {
            body.add("input", GSON.toJsonTree(texts));
        }

        String requestJson = GSON.toJson(body);

        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30_000);
            conn.setReadTimeout(60_000);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(GSON.toJson(body).getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            log.info("Embedding API response: HTTP {}", code);
            String responseBody;
            try (Scanner scanner = new Scanner(
                    code >= 400 ? conn.getErrorStream() : conn.getInputStream(),
                    StandardCharsets.UTF_8)) {
                scanner.useDelimiter("\\A");
                responseBody = scanner.hasNext() ? scanner.next() : "";
            }

            if (code != 200) {
                log.error("Embedding API failed: HTTP {} body={}", code, responseBody);
                throw new RuntimeException("Embedding API returned HTTP " + code + ": " + responseBody);
            }

            JsonObject json = GSON.fromJson(responseBody, JsonObject.class);
            List<Embedding> embeddings = new ArrayList<>();
            var dataArray = json.getAsJsonArray("data");
            if (dataArray != null) {
                for (var element : dataArray) {
                    JsonObject item = element.getAsJsonObject();
                    var embeddingArray = item.getAsJsonArray("embedding");
                    float[] vector = GSON.fromJson(embeddingArray, float[].class);
                    embeddings.add(Embedding.from(vector));
                }
            }

            // 提取 token usage
            TokenUsage usage = null;
            if (json.has("usage")) {
                JsonObject usageJson = json.getAsJsonObject("usage");
                Integer totalTokens = usageJson.has("total_tokens")
                        ? usageJson.get("total_tokens").getAsInt() : null;
                usage = new TokenUsage(totalTokens);
            }

            if (cachedDimension == null && !embeddings.isEmpty()) {
                cachedDimension = embeddings.get(0).dimension();
            }

            return Response.from(embeddings, usage);
        } catch (IOException e) {
            throw new RuntimeException("Failed to call embedding API", e);
        }
    }

    @Override
    public int dimension() {
        if (cachedDimension != null) {
            return cachedDimension;
        }
        // 首次调用探测一次
        Response<Embedding> response = embed("dimension probe");
        cachedDimension = response.content().dimension();
        return cachedDimension;
    }
}
