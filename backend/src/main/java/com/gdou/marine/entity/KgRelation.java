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
 * @Description 知识图谱关系表实体类
 */
@Data
@TableName("kg_relation")
public class KgRelation implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 源节点ID
     */
    @TableField("source_node_id")
    private Long sourceNodeId;

    /**
     * 目标节点ID
     */
    @TableField("target_node_id")
    private Long targetNodeId;

    /**
     * 关系类型：belongs_to/lives_in/eats/threatened_by/protected_by/related_to
     */
    @TableField("relation_type")
    private String relationType;

    /**
     * 关系权重
     */
    @TableField("weight")
    private BigDecimal weight;

    /**
     * 关系描述
     */
    @TableField("description")
    private String description;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
