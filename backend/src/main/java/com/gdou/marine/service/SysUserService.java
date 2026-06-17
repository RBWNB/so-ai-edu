package com.gdou.marine.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gdou.marine.dto.*;
import com.gdou.marine.entity.SysUser;
import com.gdou.marine.vo.UserPageItemVO;
import com.gdou.marine.vo.UserProfileVO;

public interface SysUserService extends IService<SysUser> {

    SysUser findByUsername(String username);

    void registerUser(RegisterRequestDTO registerRequest);

    Page<UserPageItemVO> pageUsers(UserPageQueryDTO queryDTO);

    void updateUserStatus(Long userId, StatusUpdateDTO updateDTO);

    void updateUserRoles(Long userId, UserRoleUpdateDTO updateDTO);

    UserProfileVO getUserProfile(Long userId);

    void updateUserProfile(Long userId, UserProfileUpdateDTO updateDTO);

    void updateUserAvatarUrl(Long userId, String avatarUrl);

    void updateLastLoginTime(Long userId);

    void updatePassword(Long userId, UserPasswordUpdateDTO dto);
}
