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
 * @Description 物种地理分布点表实体类
 */
@Data
@TableName("species_distribution_point")
public class SpeciesDistributionPoint implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 物种ID
     */
    @TableField("species_id")
    private Long speciesId;

    /**
     * 纬度
     */
    @TableField("latitude")
    private BigDecimal latitude;

    /**
     * 经度
     */
    @TableField("longitude")
    private BigDecimal longitude;

    /**
     * 位置名称
     */
    @TableField("location_name")
    private String locationName;

    /**
     * 海域名称
     */
    @TableField("sea_area")
    private String seaArea;

    /**
     * 数据来源
     */
    @TableField("source")
    private String source;

    /**
     * 置信度
     */
    @TableField("confidence")
    private BigDecimal confidence;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
