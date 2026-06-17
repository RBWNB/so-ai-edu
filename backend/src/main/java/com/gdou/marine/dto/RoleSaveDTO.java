package com.gdou.marine.dto;

import lombok.Data;

@Data
public class RoleSaveDTO {
    private String roleCode;
    private String roleName;
    private String description;
    private Byte status;
}