package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 知识图谱节点表实体类
 */
@Data
@TableName("kg_node")
public class KgNode implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 节点类型：species/ecosystem/concept/threat/protection
     */
    @TableField("node_type")
    private String nodeType;

    /**
     * 关联业务表ID
     */
    @TableField("ref_id")
    private Long refId;

    /**
     * 节点名称
     */
    @TableField("name")
    private String name;

    /**
     * 节点描述
     */
    @TableField("description")
    private String description;

    /**
     * 节点属性JSON
     */
    @TableField("properties")
    private String properties;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
