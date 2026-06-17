
package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description RAG文档分块表实体类
 */
@Data
@TableName("kb_document_chunk")
public class KbDocumentChunk implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档ID
     */
    @TableField("document_id")
    private Long documentId;

    /**
     * 分块序号
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * 分块内容
     */
    @TableField("content")
    private String content;

    /**
     * Redis向量Key
     */
    @TableField("embedding_key")
    private String embeddingKey;

    /**
     * Token数量
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
