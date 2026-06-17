package com.gdou.marine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gdou.marine.dto.*;
import com.gdou.marine.entity.SysRole;
import com.gdou.marine.entity.SysUser;
import com.gdou.marine.entity.SysUserRole;
import com.gdou.marine.mapper.SysUserMapper;
import com.gdou.marine.service.SysUserService;
import com.gdou.marine.vo.UserPageItemVO;
import com.gdou.marine.vo.UserProfileVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final byte STATUS_DISABLED = 0;
    private static final byte STATUS_ENABLED = 1;

    private final PasswordEncoder passwordEncoder;
    private final SysRoleServiceImpl sysRoleService;
    private final SysUserRoleServiceImpl sysUserRoleService;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder,
                              SysRoleServiceImpl sysRoleService,
                              SysUserRoleServiceImpl sysUserRoleService) {
        this.passwordEncoder = passwordEncoder;
        this.sysRoleService = sysRoleService;
        this.sysUserRoleService = sysUserRoleService;
    }

    @Override
    public SysUser findByUsername(String username) {
        return this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username), false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(RegisterRequestDTO registerRequest) {
        validateRegisterRequest(registerRequest);

        if (this.lambdaQuery().eq(SysUser::getUsername, registerRequest.getUsername()).exists()) {
            throw new IllegalArgumentException("用户名已存在");
        }
        if (this.lambdaQuery().eq(SysUser::getEmail, registerRequest.getEmail()).exists()) {
            throw new IllegalArgumentException("邮箱已被注册");
        }
        if (this.lambdaQuery().eq(SysUser::getPhone, registerRequest.getPhone()).exists()) {
            throw new IllegalArgumentException("手机号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(registerRequest.getUsername());
        user.setPasswordHash(passwordEncoder.encode(registerRequest.getPassword()));
        user.setRealName(registerRequest.getRealName());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setStatus(STATUS_ENABLED);
        this.save(user);

        // 默认分配 VISITOR 角色
        SysRole visitorRole = sysRoleService.lambdaQuery()
                .eq(SysRole::getRoleCode, "VISITOR")
                .one();

        if (visitorRole == null) {
            throw new IllegalStateException("默认角色 VISITOR 不存在，请先初始化角色数据");
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getId());
        userRole.setRoleId(visitorRole.getId());
        sysUserRoleService.save(userRole);
    }

    @Override
    public Page<UserPageItemVO> pageUsers(UserPageQueryDTO queryDTO) {
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

        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
        if (queryDTO != null) {
            if (StringUtils.hasText(queryDTO.getUsername())) {
                queryWrapper.like(SysUser::getUsername, queryDTO.getUsername());
            }
            if (StringUtils.hasText(queryDTO.getRealName())) {
                queryWrapper.like(SysUser::getRealName, queryDTO.getRealName());
            }
            if (queryDTO.getStatus() != null) {
                queryWrapper.eq(SysUser::getStatus, queryDTO.getStatus());
            }
        }
        queryWrapper.orderByDesc(SysUser::getCreatedAt);

        Page<SysUser> userPage = this.page(new Page<>(current, size), queryWrapper);

        List<Long> userIds = userPage.getRecords().stream()
                .map(SysUser::getId)
                .collect(Collectors.toList());

        Map<Long, List<SysRole>> userRolesMap = new java.util.HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUserRole> userRoles = sysUserRoleService.lambdaQuery()
                    .in(SysUserRole::getUserId, userIds)
                    .list();

            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .distinct()
                    .collect(Collectors.toList());

            List<SysRole> allRoles = roleIds.isEmpty()
                    ? Collections.emptyList()
                    : sysRoleService.listByIds(roleIds);

            for (Long userId : userIds) {
                List<SysRole> roles = new java.util.ArrayList<>();
                for (SysUserRole ur : userRoles) {
                    if (ur.getUserId().equals(userId)) {
                        for (SysRole role : allRoles) {
                            if (role.getId().equals(ur.getRoleId())) {
                                roles.add(role);
                            }
                        }
                    }
                }
                userRolesMap.put(userId, roles);
            }
        }

        List<UserPageItemVO> userItems = userPage.getRecords().stream()
                .map(user -> toUserPageItemVO(user, userRolesMap.getOrDefault(user.getId(), Collections.emptyList())))
                .collect(Collectors.toList());

        Page<UserPageItemVO> resultPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        resultPage.setRecords(userItems);
        return resultPage;
    }

    @Override
    public void updateUserStatus(Long userId, StatusUpdateDTO updateDTO) {
        validateUserId(userId);
        if (updateDTO == null || updateDTO.getStatus() == null) {
            throw new IllegalArgumentException("用户状态不能为空");
        }

        byte status = updateDTO.getStatus();
        if (status != STATUS_DISABLED && status != STATUS_ENABLED) {
            throw new IllegalArgumentException("无效的状态值，必须为 0 或 1");
        }

        ensureUserExists(userId);

        boolean updated = this.lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(SysUser::getStatus, status)
                .update();
        if (!updated) {
            throw new IllegalStateException("更新用户状态失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserRoles(Long userId, UserRoleUpdateDTO updateDTO) {
        validateUserId(userId);
        ensureUserExists(userId);

        List<Long> newRoleIds = extractRoleIds(updateDTO);

        if (!newRoleIds.isEmpty()) {
            long validRoleCount = sysRoleService.lambdaQuery()
                    .in(SysRole::getId, newRoleIds)
                    .count();
            if (validRoleCount != newRoleIds.size()) {
                throw new IllegalArgumentException("一个或多个角色ID无效");
            }
        }

        List<Long> oldRoleIds = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        List<Long> rolesToDelete = oldRoleIds.stream()
                .filter(roleId -> !newRoleIds.contains(roleId))
                .collect(Collectors.toList());

        List<Long> rolesToAdd = newRoleIds.stream()
                .filter(roleId -> !oldRoleIds.contains(roleId))
                .collect(Collectors.toList());

        if (!rolesToDelete.isEmpty()) {
            sysUserRoleService.lambdaUpdate()
                    .eq(SysUserRole::getUserId, userId)
                    .in(SysUserRole::getRoleId, rolesToDelete)
                    .remove();
        }

        if (!rolesToAdd.isEmpty()) {
            List<SysUserRole> userRolesToAdd = rolesToAdd.stream()
                    .map(roleId -> {
                        SysUserRole userRole = new SysUserRole();
                        userRole.setUserId(userId);
                        userRole.setRoleId(roleId);
                        return userRole;
                    })
                    .collect(Collectors.toList());
            sysUserRoleService.saveBatch(userRolesToAdd);
        }
    }

    @Override
    public UserProfileVO getUserProfile(Long userId) {
        validateUserId(userId);
        SysUser user = this.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }
        return toUserProfileVO(user);
    }

    @Override
    public void updateUserProfile(Long userId, UserProfileUpdateDTO updateDTO) {
        validateUserId(userId);
        if (updateDTO == null) {
            throw new IllegalArgumentException("个人资料不能为空");
        }

        ensureUserExists(userId);

        boolean hasAnyField = false;
        if (StringUtils.hasText(updateDTO.getEmail())) {
            boolean emailExists = this.lambdaQuery()
                    .eq(SysUser::getEmail, updateDTO.getEmail())
                    .ne(SysUser::getId, userId)
                    .exists();
            if (emailExists) {
                throw new IllegalArgumentException("邮箱已被注册");
            }
            hasAnyField = true;
        }
        if (StringUtils.hasText(updateDTO.getPhone())) {
            boolean phoneExists = this.lambdaQuery()
                    .eq(SysUser::getPhone, updateDTO.getPhone())
                    .ne(SysUser::getId, userId)
                    .exists();
            if (phoneExists) {
                throw new IllegalArgumentException("手机号已被注册");
            }
            hasAnyField = true;
        }
        if (StringUtils.hasText(updateDTO.getRealName())) {
            hasAnyField = true;
        }

        if (!hasAnyField) {
            throw new IllegalArgumentException("至少需要提供一个可更新的字段");
        }

        boolean updated = this.lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(StringUtils.hasText(updateDTO.getRealName()), SysUser::getRealName, updateDTO.getRealName())
                .set(StringUtils.hasText(updateDTO.getEmail()), SysUser::getEmail, updateDTO.getEmail())
                .set(StringUtils.hasText(updateDTO.getPhone()), SysUser::getPhone, updateDTO.getPhone())
                .update();

        if (!updated) {
            throw new IllegalStateException("更新个人资料失败");
        }
    }

    @Override
    public void updateLastLoginTime(Long userId) {
        validateUserId(userId);
        ensureUserExists(userId);
        this.lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(SysUser::getLastLoginTime, LocalDateTime.now())
                .update();
    }

    @Override
    public void updatePassword(Long userId, UserPasswordUpdateDTO dto) {
        validateUserId(userId);
        if (dto == null
                || !StringUtils.hasText(dto.getOldPassword())
                || !StringUtils.hasText(dto.getNewPassword())) {
            throw new IllegalArgumentException("旧密码和新密码不能为空");
        }
        if (dto.getNewPassword().length() < 6) {
            throw new IllegalArgumentException("新密码长度不能少于6位");
        }

        SysUser user = this.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("用户不存在");
        }

        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }

        if (passwordEncoder.matches(dto.getNewPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与旧密码相同");
        }

        String newPasswordHash = passwordEncoder.encode(dto.getNewPassword());
        user.setPasswordHash(newPasswordHash);
        this.updateById(user);
    }

    @Override
    public void updateUserAvatarUrl(Long userId, String avatarUrl) {
        validateUserId(userId);
        if (!StringUtils.hasText(avatarUrl)) {
            throw new IllegalArgumentException("头像URL不能为空");
        }

        ensureUserExists(userId);

        boolean updated = this.lambdaUpdate()
                .eq(SysUser::getId, userId)
                .set(SysUser::getAvatarUrl, avatarUrl)
                .update();
        if (!updated) {
            throw new IllegalStateException("更新用户头像失败");
        }
    }

    // ==================== 私有方法 ====================

    private void validateRegisterRequest(RegisterRequestDTO registerRequest) {
        if (registerRequest == null
                || !StringUtils.hasText(registerRequest.getUsername())
                || !StringUtils.hasText(registerRequest.getPassword())
                || !StringUtils.hasText(registerRequest.getRealName())
                || !StringUtils.hasText(registerRequest.getEmail())
                || !StringUtils.hasText(registerRequest.getPhone())) {
            throw new IllegalArgumentException("缺少必要的注册字段");
        }
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("无效的用户ID");
        }
    }

    private void ensureUserExists(Long userId) {
        if (!this.lambdaQuery().eq(SysUser::getId, userId).exists()) {
            throw new IllegalArgumentException("用户不存在");
        }
    }

    private List<Long> extractRoleIds(UserRoleUpdateDTO updateDTO) {
        if (updateDTO == null || updateDTO.getRoleIds() == null) {
            return Collections.emptyList();
        }
        return updateDTO.getRoleIds().stream()
                .filter(roleId -> roleId != null && roleId > 0)
                .distinct()
                .collect(Collectors.toList());
    }

    private UserPageItemVO toUserPageItemVO(SysUser user, List<SysRole> roles) {
        UserPageItemVO userItem = new UserPageItemVO();
        userItem.setId(user.getId());
        userItem.setUsername(user.getUsername());
        userItem.setRealName(user.getRealName());
        userItem.setEmail(user.getEmail());
        userItem.setPhone(user.getPhone());
        userItem.setAvatarUrl(user.getAvatarUrl());
        userItem.setStatus(user.getStatus());
        userItem.setCreatedAt(user.getCreatedAt());
        userItem.setUpdatedAt(user.getUpdatedAt());

        if (!roles.isEmpty()) {
            userItem.setRoleIds(roles.stream().map(SysRole::getId).collect(Collectors.toList()));
            userItem.setRoles(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        } else {
            userItem.setRoleIds(Collections.emptyList());
            userItem.setRoles(Collections.emptyList());
        }

        return userItem;
    }

    private UserProfileVO toUserProfileVO(SysUser user) {
        UserProfileVO profileVO = new UserProfileVO();
        profileVO.setId(user.getId());
        profileVO.setUsername(user.getUsername());
        profileVO.setRealName(user.getRealName());
        profileVO.setEmail(user.getEmail());
        profileVO.setPhone(user.getPhone());
        profileVO.setAvatarUrl(user.getAvatarUrl());
        profileVO.setStatus(user.getStatus());
        return profileVO;
    }
}
