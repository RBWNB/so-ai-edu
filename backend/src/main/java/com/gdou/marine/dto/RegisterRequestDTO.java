package com.gdou.marine.dto;

import lombok.Data;

@Data
public class RegisterRequestDTO {
    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    /** 注册意向角色：STUDENT 或 VISITOR，默认 VISITOR */
    private String applyRole;
}
