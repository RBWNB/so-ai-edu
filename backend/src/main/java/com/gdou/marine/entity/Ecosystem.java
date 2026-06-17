package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

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
