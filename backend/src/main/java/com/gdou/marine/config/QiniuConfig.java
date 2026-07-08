package com.gdou.marine.config;

import com.gdou.marine.utils.QiniuUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * @author FlnyXx
 * @Description 从环境变量注入七牛云配置到静态工具类
 */
@Configuration
public class QiniuConfig {

    @Value("${app.qiniu.access-key}")
    private String accessKey;

    @Value("${app.qiniu.secret-key}")
    private String secretKey;

    @Value("${app.qiniu.bucket}")
    private String bucket;

    @Value("${app.qiniu.domain}")
    private String domain;

    @PostConstruct
    public void init() {
        QiniuUtils.accessKey = accessKey;
        QiniuUtils.secretKey = secretKey;
        QiniuUtils.bucket = bucket;
        QiniuUtils.domainOfBucket = domain;
    }
}
