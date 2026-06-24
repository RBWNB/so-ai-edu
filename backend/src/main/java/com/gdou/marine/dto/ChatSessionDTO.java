package com.gdou.marine.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @author FlnyXx
 * @version 1.0
 * @date 2026/6/24
 * @Description 历史会话 DTO
 */
@Data
public class ChatSessionDTO {
    private String sessionId;
    private LocalDateTime startTime;
    private String title;
}
