package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答题记录表实体类
 */
@Data
@TableName("quiz_attempt")
public class QuizAttempt implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("question_id")
    private Long questionId;

    @TableField("user_answer_json")
    private String userAnswerJson;

    @TableField("is_correct")
    private Byte isCorrect;

    @TableField("time_spent_seconds")
    private Integer timeSpentSeconds;

    @TableField(value = "attempted_at", fill = FieldFill.INSERT)
    private LocalDateTime attemptedAt;
}
