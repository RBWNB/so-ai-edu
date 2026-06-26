package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 媒体资源表实体类
 */
@Data
@TableName("media_asset")
public class MediaAsset implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 存储提供商：qiniu/local/minio
     */
    @TableField("provider")
    private String provider;

    /**
     * 存储桶
     */
    @TableField("bucket")
    private String bucket;

    /**
     * 对象Key
     */
    @TableField("object_key")
    private String objectKey;

    /**
     * 资源URL
     */
    @TableField("url")
    private String url;

    /**
     * MIME类型
     */
    @TableField("mime_type")
    private String mimeType;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 原始文件名
     */
    @TableField("original_name")
    private String originalName;

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
}
