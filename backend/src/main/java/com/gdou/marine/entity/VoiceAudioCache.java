package com.gdou.marine.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 语音缓存表实体类
 */
@Data
@TableName("voice_audio_cache")
public class VoiceAudioCache implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 文本的 MD5 哈希
     */
    @TableField("text_hash")
    private String textHash;

    /**
     * 原始文本
     */
    @TableField("source_text")
    private String sourceText;

    /**
     * 音色类型
     */
    @TableField("voice_type")
    private String voiceType;

    /**
     * 关联媒体资源ID（七牛云）
     */
    @TableField("audio_media_id")
    private Long audioMediaId;

    /**
     * TTS 引擎
     */
    @TableField("tts_engine")
    private String ttsEngine;

    /**
     * 音频文件 URL（七牛云）
     */
    @TableField(exist = false)
    private String audioUrl;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
