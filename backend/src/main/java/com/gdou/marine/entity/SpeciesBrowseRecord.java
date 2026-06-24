package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author liangguize2024
 * @version 1.0
 * @date 2026/6/22
 * @Description 物种浏览记录表实体类
 */
@Data
@TableName("species_browse_record")
public class SpeciesBrowseRecord implements Serializable {
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
     * 累计浏览次数
     */
    @TableField("browse_count")
    private Integer browseCount;

    /**
     * 最后浏览时间
     */
    @TableField("last_browsed_at")
    private LocalDateTime lastBrowsedAt;

    /**
     * 首次浏览时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
