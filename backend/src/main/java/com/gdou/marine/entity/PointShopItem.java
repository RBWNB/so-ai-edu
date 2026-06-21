package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 积分商店商品表实体类
 */
@Data
@TableName("point_shop_item")
public class PointShopItem implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名称
     */
    @TableField("name")
    private String name;

    /**
     * 商品描述
     */
    @TableField("description")
    private String description;

    /**
     * 商品类型：badge/avatar_frame/coupon/virtual_item
     */
    @TableField("item_type")
    private String itemType;

    /**
     * 封面媒体ID
     */
    @TableField("cover_media_id")
    private Long coverMediaId;

    /**
     * 积分价格
     */
    @TableField("points_price")
    private Integer pointsPrice;

    /**
     * 库存（NULL表示无限）
     */
    @TableField("stock")
    private Integer stock;

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
