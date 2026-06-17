package com.gdou.marine.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserRoleUpdateDTO {
    private List<Long> roleIds;
}
