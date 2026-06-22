package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description 用户徽章表实体类
 */
@Data
@TableName("user_badge")
public class UserBadge implements Serializable {
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
     * 徽章编码
     */
    @TableField("badge_code")
    private String badgeCode;

    /**
     * 徽章名称
     */
    @TableField("badge_name")
    private String badgeName;

    /**
     * 徽章描述
     */
    @TableField("description")
    private String description;

    /**
     * 获得时间
     */
    @TableField("earned_at")
    private LocalDateTime earnedAt;
}
