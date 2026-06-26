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
 * @Description 海洋物种百科表实体类
 */
@Getter
@Setter
@TableName("marine_species")
public class Species implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("chinese_name")
    private String chineseName;

    @TableField("scientific_name")
    private String scientificName;

    @TableField("alias_names")
    private String aliasNames;

    @TableField("kingdom")
    private String kingdom;

    @TableField("phylum")
    private String phylum;

    @TableField("class_name")
    private String className;

    @TableField("order_name")
    private String orderName;

    @TableField("family_name")
    private String familyName;

    @TableField("genus_name")
    private String genusName;

    @TableField("species_name")
    private String speciesName;

    @TableField("conservation_status")
    private String conservationStatus;

    @TableField("habitat")
    private String habitat;

    @TableField("distribution_area")
    private String distributionArea;

    @TableField("morphology_desc")
    private String morphologyDesc;

    @TableField("habit_desc")
    private String habitDesc;

    @TableField("fun_fact")
    private String funFact;

    @TableField("cover_media_id")
    private Long coverMediaId;

    /** 过渡方案：直连图片URL，后续迁移到 media_asset 表 */
    @TableField("image_url")
    private String imageUrl;

    /** 过渡方案：直连视频URL */
    @TableField("video_url")
    private String videoUrl;

    @TableField("data_source")
    private String dataSource;

    @TableField("status")
    private Byte status;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @TableLogic
    private Byte isDeleted;
}
