package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 用户积分账户表实体类
 */
@Data
@TableName("user_point_account")
public class UserPointAccount implements Serializable {
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
     * 可用积分
     */
    @TableField("available_points")
    private Integer availablePoints;

    /**
     * 累计获得积分
     */
    @TableField("total_earned_points")
    private Integer totalEarnedPoints;

    /**
     * 累计消费积分
     */
    @TableField("total_spent_points")
    private Integer totalSpentPoints;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
