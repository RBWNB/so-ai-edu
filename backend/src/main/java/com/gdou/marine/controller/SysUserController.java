package com.gdou.marine.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gdou.marine.annotation.Log;
import com.gdou.marine.dto.*;
import com.gdou.marine.service.SysUserService;
import com.gdou.marine.utils.QiniuUtils;
import com.gdou.marine.vo.UserPageItemVO;
import com.gdou.marine.vo.UserProfileVO;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/sys-user")
public class SysUserController {

    private final SysUserService sysUserService;

    public SysUserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @GetMapping("/page")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> pageUsers(UserPageQueryDTO queryDTO) {
        Page<UserPageItemVO> result = sysUserService.pageUsers(queryDTO);
        return ResponseEntity.ok(result);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "用户管理", description = "更新用户状态")
    public ResponseEntity<?> updateUserStatus(@PathVariable("id") Long userId,
                                              @RequestBody StatusUpdateDTO updateDTO) {
        try {
            sysUserService.updateUserStatus(userId, updateDTO);
            return ResponseEntity.ok("User status updated successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    @Log(module = "用户管理", description = "更新用户角色")
    public ResponseEntity<?> updateUserRoles(@PathVariable("id") Long userId,
                                             @RequestBody UserRoleUpdateDTO updateDTO) {
        try {
            sysUserService.updateUserRoles(userId, updateDTO);
            return ResponseEntity.ok("User roles updated successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getProfile() {
        try {
            Long userId = getCurrentUserId();
            UserProfileVO profile = sysUserService.getUserProfile(userId);
            return ResponseEntity.ok(profile);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    @Log(module = "用户管理", description = "更新个人资料")
    public ResponseEntity<?> updateProfile(@RequestBody UserProfileUpdateDTO updateDTO) {
        try {
            Long userId = getCurrentUserId();
            sysUserService.updateUserProfile(userId, updateDTO);
            return ResponseEntity.ok("Profile updated successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        }
    }

    @PutMapping("/avatar-frame")
    @PreAuthorize("isAuthenticated()")
    @Log(module = "用户管理", description = "更新头像框")
    public ResponseEntity<?> updateAvatarFrame(@RequestBody Map<String, String> body) {
        try {
            Long userId = getCurrentUserId();
            String frameCode = body.get("frameCode");
            if (frameCode == null || frameCode.isBlank()) {
                return ResponseEntity.badRequest().body("头像框编码不能为空");
            }
            sysUserService.updateAvatarFrame(userId, frameCode);
            return ResponseEntity.ok(Map.of("success", true, "avatarFrame", frameCode));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("更新头像框失败");
        }
    }

    @PutMapping("/user-title")
    @PreAuthorize("isAuthenticated()")
    @Log(module = "用户管理", description = "更新称号")
    public ResponseEntity<?> updateUserTitle(@RequestBody Map<String, String> body) {
        try {
            Long userId = getCurrentUserId();
            String title = body.get("title");
            if (title == null) {
                return ResponseEntity.badRequest().body("称号不能为空");
            }
            sysUserService.updateUserTitle(userId, title);
            return ResponseEntity.ok(Map.of("success", true, "userTitle", title));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("更新称号失败");
        }
    }

    @PutMapping("/password")
    @PreAuthorize("isAuthenticated()")
    @Log(module = "用户管理", description = "修改密码")
    public ResponseEntity<?> updatePassword(@RequestBody UserPasswordUpdateDTO updateDTO) {
        try {
            Long userId = getCurrentUserId();

            if (!StringUtils.hasText(updateDTO.getNewPassword()) || updateDTO.getNewPassword().length() < 6) {
                return ResponseEntity.badRequest().body("New password length must be at least 6");
            }

            sysUserService.updatePassword(userId, updateDTO);
            return ResponseEntity.ok("Password updated successfully");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.internalServerError().body("Failed to update password");
        }
    }

    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    @PostMapping("/upload/avatar")
    @PreAuthorize("isAuthenticated()")
    @Log(module = "用户管理", description = "上传头像 ")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body("Avatar file is required");
            }
            if (file.getSize() > MAX_AVATAR_SIZE) {
                return ResponseEntity.badRequest().body("Avatar file size must not exceed 2MB");
            }
            if (!isAllowedImage(file)) {
                return ResponseEntity.badRequest().body("Only JPG, PNG and GIF images are allowed");
            }

            Long userId = getCurrentUserId();
            String avatarUrl = uploadAvatarToQiniu(file);
            sysUserService.updateUserAvatarUrl(userId, avatarUrl);

            Map<String, Object> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            response.put("message", "Avatar uploaded successfully");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        } catch (IOException ex) {
            return ResponseEntity.internalServerError().body("Failed to read avatar file");
        } catch (IllegalStateException ex) {
            return ResponseEntity.internalServerError().body(ex.getMessage());
        } catch (RuntimeException ex) {
            return ResponseEntity.internalServerError().body("Failed to upload avatar to cloud storage");
        }
    }

    private boolean isAllowedImage(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.contains(".")) {
            return false;
        }
        String ext = originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return false;
        }
        // 文件魔数校验，防止 Content-Type 伪造
        try (InputStream in = file.getInputStream()) {
            byte[] header = new byte[8];
            int read = in.read(header);
            if (read < 4) {
                return false;
            }
            // JPEG: FF D8 FF
            if (ext.equals("jpg") || ext.equals("jpeg")) {
                return (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8;
            }
            // PNG: 89 50 4E 47
            if (ext.equals("png")) {
                return (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
            }
            // GIF: 47 49 46 38
            if (ext.equals("gif")) {
                return header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38;
            }
            return false;
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * 生成唯一文件名
     */
    private String generateFileName(MultipartFile file) {
        String extension = StringUtils.getFilenameExtension(file.getOriginalFilename());
        String fileName = "avatars/" + UUID.randomUUID().toString().replace("-", "");
        if (StringUtils.hasText(extension)) {
            fileName = fileName + "." + extension;
        }
        return fileName;
    }

    /**
     * 上传头像到七牛云
     */
    private String uploadAvatarToQiniu(MultipartFile file) throws IOException {
        String fileName = generateFileName(file);
        return QiniuUtils.upload2Qiniu(file.getBytes(), fileName);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("User is not authenticated");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        if (principal instanceof Integer id) {
            return id.longValue();
        }
        if (principal instanceof String principalText) {
            try {
                return Long.parseLong(principalText);
            } catch (NumberFormatException ignored) {
                throw new IllegalStateException("Invalid authentication principal");
            }
        }

        throw new IllegalStateException("Invalid authentication principal");
    }
}
