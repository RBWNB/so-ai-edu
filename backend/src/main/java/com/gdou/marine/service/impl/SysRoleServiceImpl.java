package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.dto.RolePageQueryDTO;
import com.gdou.marine.dto.RoleSaveDTO;
import com.gdou.marine.dto.StatusUpdateDTO;
import com.gdou.marine.entity.SysRole;
import com.gdou.marine.mapper.SysRoleMapper;
import com.gdou.marine.service.SysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    private static final byte STATUS_DISABLED = 0;
    private static final byte STATUS_ENABLED = 1;

    @Override
    public Page<SysRole> pageRoles(RolePageQueryDTO queryDTO) {
        long current = 1L;
        long size = 10L;
        if (queryDTO != null) {
            if (queryDTO.getCurrent() != null && queryDTO.getCurrent() > 0) {
                current = queryDTO.getCurrent();
            }
            if (queryDTO.getSize() != null && queryDTO.getSize() > 0) {
                size = queryDTO.getSize();
            }
        }

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getRoleName())) {
                wrapper.like(SysRole::getRoleName, queryDTO.getRoleName().trim());
            }
            if (StringUtils.hasText(queryDTO.getRoleCode())) {
                wrapper.like(SysRole::getRoleCode, queryDTO.getRoleCode().trim().toUpperCase());
            }
            if (queryDTO.getStatus() != null) {
                wrapper.eq(SysRole::getStatus, queryDTO.getStatus());
            }
        }
        wrapper.orderByAsc(SysRole::getId);
        return this.page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysRole createRole(RoleSaveDTO dto) {
        validateRoleSaveDTO(dto, true);
        String roleCode = dto.getRoleCode().trim().toUpperCase();
        String roleName = dto.getRoleName().trim();

        boolean codeExists = this.lambdaQuery().eq(SysRole::getRoleCode, roleCode).exists();
        if (codeExists) {
            throw new IllegalArgumentException("Role code already exists");
        }

        boolean nameExists = this.lambdaQuery().eq(SysRole::getRoleName, roleName).exists();
        if (nameExists) {
            throw new IllegalArgumentException("Role name already exists");
        }

        SysRole role = new SysRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null);
        role.setStatus(dto.getStatus() == null ? STATUS_ENABLED : dto.getStatus());
        this.save(role);
        return role;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(Long roleId, RoleSaveDTO dto) {
        validateRoleId(roleId);
        validateRoleSaveDTO(dto, false);
        ensureRoleExists(roleId);

        String roleCode = dto.getRoleCode().trim().toUpperCase();
        String roleName = dto.getRoleName().trim();

        boolean codeExists = this.lambdaQuery()
                .eq(SysRole::getRoleCode, roleCode)
                .ne(SysRole::getId, roleId)
                .exists();
        if (codeExists) {
            throw new IllegalArgumentException("Role code already exists");
        }

        boolean nameExists = this.lambdaQuery()
                .eq(SysRole::getRoleName, roleName)
                .ne(SysRole::getId, roleId)
                .exists();
        if (nameExists) {
            throw new IllegalArgumentException("Role name already exists");
        }

        boolean updated = this.lambdaUpdate()
                .eq(SysRole::getId, roleId)
                .set(SysRole::getRoleCode, roleCode)
                .set(SysRole::getRoleName, roleName)
                .set(SysRole::getDescription, StringUtils.hasText(dto.getDescription()) ? dto.getDescription().trim() : null)
                .set(dto.getStatus() != null, SysRole::getStatus, dto.getStatus())
                .update();
        if (!updated) {
            throw new IllegalStateException("Failed to update role");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRoleLogical(Long roleId) {
        validateRoleId(roleId);
        SysRole role = ensureRoleExists(roleId);
        if ("ADMIN".equalsIgnoreCase(role.getRoleCode())) {
            throw new IllegalArgumentException("ADMIN role cannot be deleted");
        }

        boolean removed = this.removeById(roleId);
        if (!removed) {
            throw new IllegalStateException("Failed to delete role");
        }
    }

    @Override
    public void updateRoleStatus(Long roleId, StatusUpdateDTO dto) {
        validateRoleId(roleId);
        if (dto == null || dto.getStatus() == null) {
            throw new IllegalArgumentException("Role status is required");
        }
        if (dto.getStatus() != STATUS_DISABLED && dto.getStatus() != STATUS_ENABLED) {
            throw new IllegalArgumentException("Role status must be 0 or 1");
        }

        SysRole role = ensureRoleExists(roleId);
        if ("ADMIN".equalsIgnoreCase(role.getRoleCode()) && dto.getStatus() == STATUS_DISABLED) {
            throw new IllegalArgumentException("ADMIN role cannot be disabled");
        }

        boolean updated = this.lambdaUpdate()
                .eq(SysRole::getId, roleId)
                .set(SysRole::getStatus, dto.getStatus())
                .update();
        if (!updated) {
            throw new IllegalStateException("Failed to update role status");
        }
    }

    @Override
    public List<SysRole> listEnabledRoles() {
        return this.lambdaQuery()
                .eq(SysRole::getStatus, STATUS_ENABLED)
                .orderByAsc(SysRole::getId)
                .list();
    }

    private void validateRoleId(Long roleId) {
        if (roleId == null || roleId <= 0) {
            throw new IllegalArgumentException("Invalid role id");
        }
    }

    private void validateRoleSaveDTO(RoleSaveDTO dto, boolean requireStatus) {
        if (dto == null
                || !StringUtils.hasText(dto.getRoleCode())
                || !StringUtils.hasText(dto.getRoleName())) {
            throw new IllegalArgumentException("Role code and role name are required");
        }
        if (requireStatus && dto.getStatus() != null
                && dto.getStatus() != STATUS_DISABLED
                && dto.getStatus() != STATUS_ENABLED) {
            throw new IllegalArgumentException("Role status must be 0 or 1");
        }
        if (dto.getStatus() != null
                && dto.getStatus() != STATUS_DISABLED
                && dto.getStatus() != STATUS_ENABLED) {
            throw new IllegalArgumentException("Role status must be 0 or 1");
        }
    }

    private SysRole ensureRoleExists(Long roleId) {
        SysRole role = this.getById(roleId);
        if (role == null) {
            throw new IllegalArgumentException("Role not found");
        }
        return role;
    }
}
