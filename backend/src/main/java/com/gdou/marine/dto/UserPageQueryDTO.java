package com.gdou.marine.dto;

import lombok.Data;

@Data
public class UserPageQueryDTO {
    private Long current;
    private Long size;
    private String username;
    private String realName;
    private Byte status;
}
