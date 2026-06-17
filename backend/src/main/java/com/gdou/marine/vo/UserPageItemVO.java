package com.gdou.marine.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserPageItemVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private Byte status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<Long> roleIds;
    private List<String> roles;
}
