package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 积分流水表实体类
 */
@Data
@TableName("point_transaction")
public class PointTransaction implements Serializable {
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
     * 积分变动值
     */
    @TableField("points")
    private Integer points;

    /**
     * 业务类型：quiz/task/shop/admin
     */
    @TableField("biz_type")
    private String bizType;

    /**
     * 业务ID
     */
    @TableField("biz_id")
    private Long bizId;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
