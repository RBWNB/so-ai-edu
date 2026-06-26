package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 竞技模式答题记录实体
 */
@Data
@TableName("competition_record")
public class CompetitionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("accuracy")
    private BigDecimal accuracy;

    @TableField("total_questions")
    private Integer totalQuestions;

    @TableField("correct_count")
    private Integer correctCount;

    @TableField("total_time_ms")
    private Long totalTimeMs;

    @TableField("avg_time_ms")
    private Long avgTimeMs;

    @TableField("tier")
    private String tier;

    @TableField("rank_score")
    private Integer rankScore;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
