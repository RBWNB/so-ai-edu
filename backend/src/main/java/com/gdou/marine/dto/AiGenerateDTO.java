package com.gdou.marine.dto;

import lombok.Data;

/**
 * AI生成题目请求 DTO
 */
@Data
public class AiGenerateDTO {
    /**
     * 知识库文档ID
     */
    private Long documentId;

    /**
     * 题目数量
     */
    private Integer count;

    /**
     * 题目类型：single单选 / multiple多选 / judge判断 / mixed混合
     */
    private String questionType;

    /**
     * 难度：easy简单 / normal普通 / hard困难
     */
    private String difficulty;
}
