package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 学习画像表实体类
 */
@Data
@TableName("user_learning_profile")
public class UserLearningProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 等级
     */
    @TableField("level")
    private Integer level;

    /**
     * 总答题数
     */
    @TableField("total_questions")
    private Integer totalQuestions;

    /**
     * 正确答题数
     */
    @TableField("correct_count")
    private Integer correctCount;

    /**
     * 正确率
     */
    @TableField("correct_rate")
    private BigDecimal correctRate;

    /**
     * 薄弱知识点JSON
     */
    @TableField("weak_points")
    private String weakPoints;

    /**
     * 偏好分类JSON
     */
    @TableField("preferred_categories")
    private String preferredCategories;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
