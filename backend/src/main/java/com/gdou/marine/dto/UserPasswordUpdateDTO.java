package com.gdou.marine.dto;

import lombok.Data;

@Data
public class UserPasswordUpdateDTO {
    private String oldPassword;
    private String newPassword;
}
