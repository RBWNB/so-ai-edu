package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 物种媒体图库表实体类
 */
@Data
@TableName("species_media")
public class SpeciesMedia implements Serializable {
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
     * 媒体资源ID
     */
    @TableField("media_id")
    private Long mediaId;

    /**
     * 媒体角色：cover/gallery/video/audio
     */
    @TableField("media_role")
    private String mediaRole;

    /**
     * 排序序号
     */
    @TableField("sort_order")
    private Integer sortOrder;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
