package com.gdou.marine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.dto.RolePageQueryDTO;
import com.gdou.marine.dto.RoleSaveDTO;
import com.gdou.marine.dto.StatusUpdateDTO;
import com.gdou.marine.entity.SysRole;
import com.gdou.marine.service.SysRoleService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/sys-role")
public class SysRoleController {

    private final SysRoleService sysRoleService;

    public SysRoleController(SysRoleService sysRoleService) {
        this.sysRoleService = sysRoleService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pageRoles(RolePageQueryDTO queryDTO) {
        Page<SysRole> page = sysRoleService.pageRoles(queryDTO);
        return ResponseEntity.ok(page);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "角色管理", description = "新增角色")
    public ResponseEntity<?> createRole(@RequestBody RoleSaveDTO dto) {
        try {
            SysRole role = sysRoleService.createRole(dto);
            return ResponseEntity.ok(role);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "角色管理", description = "修改角色")
    public ResponseEntity<?> updateRole(@PathVariable("id") Long roleId,
                                        @RequestBody RoleSaveDTO dto) {
        try {
            sysRoleService.updateRole(roleId, dto);
            return ResponseEntity.ok(Map.of("message", "Role updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "角色管理", description = "删除角色")
    public ResponseEntity<?> deleteRole(@PathVariable("id") Long roleId) {
        try {
            sysRoleService.deleteRoleLogical(roleId);
            return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "角色管理", description = "更新角色状态")
    public ResponseEntity<?> updateRoleStatus(@PathVariable("id") Long roleId,
                                              @RequestBody StatusUpdateDTO dto) {
        try {
            sysRoleService.updateRoleStatus(roleId, dto);
            return ResponseEntity.ok(Map.of("message", "Role status updated successfully"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @GetMapping("/enabled-list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> enabledRoleList() {
        List<SysRole> roles = sysRoleService.listEnabledRoles();
        return ResponseEntity.ok(roles);
    }
}

