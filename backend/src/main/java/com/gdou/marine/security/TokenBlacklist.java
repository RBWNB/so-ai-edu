package com.gdou.marine.security;

import com.gdou.marine.utils.JwtUtils;
import io.jsonwebtoken.Claims;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于 ConcurrentHashMap 的 JWT 黑名单。
 * 存储已登出 Token 及其过期时间，定期清理过期条目。
 */
@Component
public class TokenBlacklist {

    /** token -> 过期时间戳(ms) */
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();
    private final JwtUtils jwtUtils;

    public TokenBlacklist(JwtUtils jwtUtils) {
        this.jwtUtils = jwtUtils;
    }

    /** 将 Token 加入黑名单，记录其 JWT 过期时间 */
    public void blacklist(String token) {
        try {
            Claims claims = jwtUtils.parseToken(token);
            Date expiration = claims.getExpiration();
            if (expiration != null) {
                blacklist.put(token, expiration.getTime());
            }
        } catch (Exception ignored) {
            // Token 已失效则无需加入黑名单
        }
    }

    /** 检查 Token 是否在黑名单中 */
    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }

    /** 每分钟清理已过期的黑名单条目 */
    @Scheduled(fixedRate = 60_000)
    public void evictExpired() {
        long now = System.currentTimeMillis();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
