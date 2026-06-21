package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 积分兑换记录表实体类
 */
@Data
@TableName("point_exchange_order")
public class PointExchangeOrder implements Serializable {
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
     * 商品ID
     */
    @TableField("item_id")
    private Long itemId;

    /**
     * 消耗积分
     */
    @TableField("points_cost")
    private Integer pointsCost;

    /**
     * 订单状态
     */
    @TableField("order_status")
    private String orderStatus;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
