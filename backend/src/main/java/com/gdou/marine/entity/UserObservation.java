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
 * @Description 用户观察分享表实体类
 */
@Data
@TableName("user_observation")
public class UserObservation implements Serializable {
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
     * 物种ID
     */
    @TableField("species_id")
    private Long speciesId;

    /**
     * 观察标题
     */
    @TableField("title")
    private String title;

    /**
     * 观察描述
     */
    @TableField("description")
    private String description;

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
     * 观察时间
     */
    @TableField("observed_at")
    private LocalDateTime observedAt;

    /**
     * 照片媒体ID
     */
    @TableField("photo_media_id")
    private Long photoMediaId;

    /**
     * 是否AI识别：0否，1是
     */
    @TableField("ai_identified")
    private Byte aiIdentified;

    /**
     * AI识别置信度
     */
    @TableField("ai_confidence")
    private BigDecimal aiConfidence;

    /**
     * 状态：0隐藏，1可见，2待审核
     */
    @TableField("status")
    private Byte status;

    /**
     * 审核备注（下架原因等）
     */
    @TableField("audit_remark")
    private String auditRemark;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
