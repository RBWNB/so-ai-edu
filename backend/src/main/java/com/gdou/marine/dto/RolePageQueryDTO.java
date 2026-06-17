package com.gdou.marine.dto;

import lombok.Data;

@Data
public class RolePageQueryDTO {
    private Long current = 1L;
    private Long size = 10L;
    private String roleName;
    private String roleCode;
    private Byte status;
}