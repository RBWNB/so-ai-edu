package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 海洋生态系统表实体类
 */
@Getter
@Setter
@TableName("marine_ecosystem")
public class Ecosystem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("typical_species")
    private String typicalSpecies;

    @TableField("threats")
    private String threats;

    @TableField("protection_advice")
    private String protectionAdvice;

    @TableField("cover_media_id")
    private Long coverMediaId;

    /** 过渡方案：直连图片URL，后续迁移到 media_asset 表 */
    @TableField("image_url")
    private String imageUrl;

    @TableField("status")
    private Byte status;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
