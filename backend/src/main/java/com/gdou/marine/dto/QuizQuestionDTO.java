package com.gdou.marine.dto;

import lombok.Data;

/**
 * 题目创建/编辑 DTO
 */
@Data
public class QuizQuestionDTO {
    private Long id;
    private String questionType;
    private String stem;
    private String optionsJson;
    private String answerJson;
    private String explanation;
    private String difficulty;
    private String knowledgePoints;
    private Long speciesId;
    private Byte status;
}
