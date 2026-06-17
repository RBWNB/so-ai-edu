package com.gdou.marine.controller;

import com.gdou.marine.annotation.Log;
import com.gdou.marine.dto.RegisterRequestDTO;
import com.gdou.marine.entity.SysRole;
import com.gdou.marine.entity.SysUser;
import com.gdou.marine.entity.SysUserRole;
import com.gdou.marine.security.TokenBlacklist;
import com.gdou.marine.service.SysUserService;
import com.gdou.marine.service.impl.SysRoleServiceImpl;
import com.gdou.marine.service.impl.SysUserRoleServiceImpl;
import com.gdou.marine.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final SysUserService sysUserService;
    private final SysUserRoleServiceImpl sysUserRoleService;
    private final SysRoleServiceImpl sysRoleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final TokenBlacklist tokenBlacklist;
    private final HttpServletRequest request;

    public AuthController(SysUserService sysUserService,
                          SysUserRoleServiceImpl sysUserRoleService,
                          SysRoleServiceImpl sysRoleService,
                          PasswordEncoder passwordEncoder,
                          JwtUtils jwtUtils,
                          TokenBlacklist tokenBlacklist,
                          HttpServletRequest request) {
        this.sysUserService = sysUserService;
        this.sysUserRoleService = sysUserRoleService;
        this.sysRoleService = sysRoleService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.tokenBlacklist = tokenBlacklist;
        this.request = request;
    }

    @Log(module = "系统认证", description = "用户登录")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        final byte STATUS_DISABLED = 0;

        SysUser user = sysUserService.findByUsername(loginRequest.getUsername());

        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            return ResponseEntity.status(401).body("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == STATUS_DISABLED) {
            return ResponseEntity.status(403).body("账号已被禁用，请联系系统管理员");
        }

        List<String> roles = getUserRoles(user.getId());
        String token = jwtUtils.generateToken(user.getId(), user.getUsername());

        sysUserService.updateLastLoginTime(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("message", "登录成功");
        response.put("userId", user.getId());
        response.put("username", user.getUsername());
        response.put("token", token);
        response.put("roles", roles);

        return ResponseEntity.ok(response);
    }

    private List<String> getUserRoles(Long userId) {
        List<Long> roleIds = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        return sysRoleService.listByIds(roleIds)
                .stream()
                .map(SysRole::getRoleCode)
                .collect(Collectors.toList());
    }

    @Log(module = "系统认证", description = "用户注册")
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequestDTO registerRequest) {
        try {
            sysUserService.registerUser(registerRequest);
            return ResponseEntity.ok("Register successful, waiting for approval");
        } catch (IllegalArgumentException ex) {
            if (ex.getMessage() != null && ex.getMessage().contains("exists")) {
                return ResponseEntity.status(409).body(ex.getMessage());
            }
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @Log(module = "系统认证", description = "用户登出")
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        String bearer = request.getHeader("Authorization");
        if (bearer != null && bearer.startsWith("Bearer ")) {
            String token = bearer.substring(7);
            tokenBlacklist.blacklist(token);
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Logout successful"));
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}