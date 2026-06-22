package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 学习任务表实体类
 */
@Data
@TableName("learning_task")
public class LearningTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 任务标题
     */
    @TableField("title")
    private String title;

    /**
     * 任务描述
     */
    @TableField("description")
    private String description;

    /**
     * 任务类型：daily_quiz/read_species/ask_ai/upload_observation
     */
    @TableField("task_type")
    private String taskType;

    /**
     * 目标值
     */
    @TableField("target_value")
    private Integer targetValue;

    /**
     * 奖励积分
     */
    @TableField("reward_points")
    private Integer rewardPoints;

    /**
     * 开始时间
     */
    @TableField("start_time")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @TableField("end_time")
    private LocalDateTime endTime;

    /**
     * 状态：1启用，0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
