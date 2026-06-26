package com.gdou.marine.dto;

import lombok.Data;

/**
 * AI生成题目请求 DTO
 */
@Data
public class AiGenerateDTO {
    /**
     * 知识库文档ID（sourceType=kb时使用）
     */
    private Long documentId;

    /**
     * 海洋百科物种ID（sourceType=species时使用）
     */
    private Long speciesId;

    /**
     * 来源类型：kb知识库 / species海洋百科
     */
    private String sourceType;

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
