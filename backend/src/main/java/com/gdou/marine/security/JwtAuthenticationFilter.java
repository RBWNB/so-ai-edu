package com.gdou.marine.security;

import com.gdou.marine.entity.SysRole;
import com.gdou.marine.entity.SysUserRole;
import com.gdou.marine.service.impl.SysRoleServiceImpl;
import com.gdou.marine.service.impl.SysUserRoleServiceImpl;
import com.gdou.marine.utils.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final SysUserRoleServiceImpl sysUserRoleService;
    private final SysRoleServiceImpl sysRoleService;
    private final TokenBlacklist tokenBlacklist;

    public JwtAuthenticationFilter(JwtUtils jwtUtils,
                                   SysUserRoleServiceImpl sysUserRoleService,
                                   SysRoleServiceImpl sysRoleService,
                                   TokenBlacklist tokenBlacklist) {
        this.jwtUtils = jwtUtils;
        this.sysUserRoleService = sysUserRoleService;
        this.sysRoleService = sysRoleService;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token) && !tokenBlacklist.isBlacklisted(token) && jwtUtils.validateToken(token)) {
            Long userId = jwtUtils.getUserIdFromToken(token);
            List<GrantedAuthority> authorities = loadAuthorities(userId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    private List<GrantedAuthority> loadAuthorities(Long userId) {
        List<Long> roleIds = sysUserRoleService.lambdaQuery()
                .eq(SysUserRole::getUserId, userId)
                .list()
                .stream()
                .map(SysUserRole::getRoleId)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 仅加载角色编码 → hasRole('ADMIN') / hasRole('MANAGER') / hasRole('VISITOR')
        return sysRoleService.listByIds(roleIds)
                .stream()
                .map(SysRole::getRoleCode)
                .filter(StringUtils::hasText)
                .distinct()
                .map(roleCode -> new SimpleGrantedAuthority("ROLE_" + roleCode))
                .collect(Collectors.toList());
    }

    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
