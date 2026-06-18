package com.gdou.marine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "zhipuai")
public class ZhipuAiConfig {

    private String apiKey = "REDACTED_ZHIPU_API_KEY";
    private String apiUrl = "https://open.bigmodel.cn/api/paas/v4";
    private String imageModel = "glm-4v-flash";
    private String textModel = "GLM-4.6V-Flash";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    public String getImageModel() {
        return imageModel;
    }

    public void setImageModel(String imageModel) {
        this.imageModel = imageModel;
    }

    public String getTextModel() {
        return textModel;
    }

    public void setTextModel(String textModel) {
        this.textModel = textModel;
    }
}
