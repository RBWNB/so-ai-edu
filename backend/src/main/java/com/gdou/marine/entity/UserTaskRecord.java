package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 用户任务完成记录表实体类
 */
@Data
@TableName("user_task_record")
public class UserTaskRecord implements Serializable {
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
     * 任务ID
     */
    @TableField("task_id")
    private Long taskId;

    /**
     * 当前进度值
     */
    @TableField("progress_value")
    private Integer progressValue;

    /**
     * 是否完成：0未完成，1已完成
     */
    @TableField("completed")
    private Byte completed;

    /**
     * 完成时间
     */
    @TableField("completed_at")
    private LocalDateTime completedAt;

    /**
     * 奖励是否领取：0未领取，1已领取
     */
    @TableField("reward_claimed")
    private Byte rewardClaimed;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
