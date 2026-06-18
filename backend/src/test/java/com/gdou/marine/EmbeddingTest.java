package com.gdou.marine;


import dev.langchain4j.data.embedding.Embedding;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/18
 * @Description
 */
@SpringBootTest
public class EmbeddingTest {
    @Autowired
    private EmbeddingModel embeddingModel;

    @Test
    void testEmbedding() {
        // 1. 调用向量化
        Response<Embedding> response = embeddingModel.embed("中华白海豚的栖息地");

        // 2. 打印结果
        Embedding embedding = response.content();
        float[] vector = embedding.vector();

        System.out.println("=== 向量化调用成功 ===");
        System.out.println("向量维度：" + vector.length);
        System.out.println("前5个值：");
        for (int i = 0; i < 5; i++) {
            System.out.print(vector[i] + " ");
        }
        System.out.println();
        System.out.println("Token用量：" + response.tokenUsage());
    }

}
