package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/16
 * @Description AI调用日志表实体类
 */
@Data
@TableName("ai_call_log")
public class AiCallLog implements Serializable {
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
     * 调用场景：rag/chat/vision/quiz/tts
     */
    @TableField("scene")
    private String scene;

    /**
     * AI提供商
     */
    @TableField("provider")
    private String provider;

    /**
     * 模型名称
     */
    @TableField("model_name")
    private String modelName;

    /**
     * 提示词Token数
     */
    @TableField("prompt_tokens")
    private Integer promptTokens;

    /**
     * 生成Token数
     */
    @TableField("completion_tokens")
    private Integer completionTokens;

    /**
     * 调用耗时（毫秒）
     */
    @TableField("latency_ms")
    private Long latencyMs;

    /**
     * 调用状态：SUCCESS/FAIL
     */
    @TableField("status")
    private String status;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
