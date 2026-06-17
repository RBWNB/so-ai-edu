package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.marine.dto.RolePageQueryDTO;
import com.gdou.marine.dto.RoleSaveDTO;
import com.gdou.marine.dto.StatusUpdateDTO;
import com.gdou.marine.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    Page<SysRole> pageRoles(RolePageQueryDTO queryDTO);

    SysRole createRole(RoleSaveDTO dto);

    void updateRole(Long roleId, RoleSaveDTO dto);

    void deleteRoleLogical(Long roleId);

    void updateRoleStatus(Long roleId, StatusUpdateDTO dto);

    List<SysRole> listEnabledRoles();
}

