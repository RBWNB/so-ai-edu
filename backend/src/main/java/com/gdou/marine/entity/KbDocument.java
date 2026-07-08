package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 知识库文档表实体类
 */
@Data
@TableName("kb_document")
public class KbDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 文档标题
     */
    @TableField("title")
    private String title;

    /**
     * 文档内容
     */
    @TableField("content")
    private String content;

    /**
     * 来源
     */
    @TableField("source")
    private String source;

    /**
     * 来源类型：manual/species/ecosystem/upload
     */
    @TableField("source_type")
    private String sourceType;

    /**
     * 关联物种ID（当source_type=species时使用）
     */
    @TableField("species_id")
    private Long speciesId;

    /**
     * 状态：0草稿，1已发布
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建人ID
     */
    @TableField("created_by")
    private Long createdBy;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
