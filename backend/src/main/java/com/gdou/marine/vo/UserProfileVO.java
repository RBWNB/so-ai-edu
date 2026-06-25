package com.gdou.marine.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String realName;
    private String email;
    private String phone;
    private String avatarUrl;
    private String avatarFrame;
    private String userTitle;
    private Byte status;
}
