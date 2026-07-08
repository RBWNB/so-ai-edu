package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 题库表实体类
 */
@Data
@TableName("quiz_question")
public class QuizQuestion implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 题目类型：single单选 / multiple多选 / judge判断
     */
    @TableField("question_type")
    private String questionType;

    /**
     * 题干
     */
    @TableField("stem")
    private String stem;

    /**
     * 选项（JSON数组）
     */
    @TableField("options_json")
    private String optionsJson;

    /**
     * 答案（JSON）
     */
    @TableField("answer_json")
    private String answerJson;

    /**
     * 解析
     */
    @TableField("explanation")
    private String explanation;

    /**
     * 难度：easy简单 / normal普通 / hard困难
     */
    @TableField("difficulty")
    private String difficulty;

    /**
     * 知识点（JSON数组）
     */
    @TableField("knowledge_points")
    private String knowledgePoints;

    /**
     * 关联物种ID
     */
    @TableField("species_id")
    private Long speciesId;

    /**
     * 来源知识库文档ID（AI从RAG知识库生成时记录原始文档，用于收藏知识库功能）
     */
    @TableField("source_document_id")
    private Long sourceDocumentId;

    /**
     * 是否AI生成：0人工/1AI
     */
    @TableField("created_by_ai")
    private Byte createdByAi;

    /**
     * 状态：0禁用 / 1启用
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
