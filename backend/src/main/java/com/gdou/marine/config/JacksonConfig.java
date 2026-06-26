package com.gdou.marine.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigInteger;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/26
 * @Description Jackson 配置 — Long / BigInteger 序列化为 String，避免前端 JS 精度丢失（雪花 ID）
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer idToStringCustomizer() {
        return builder -> {
            // 雪花 ID 最常见的 Java 类型
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(long.class, ToStringSerializer.instance);
            // MySQL JDBC 对 BIGINT UNSIGNED 可能返回 BigInteger
            builder.serializerByType(BigInteger.class, ToStringSerializer.instance);
        };
    }
}
