package com.asset.common;

import com.asset.entity.SysUser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端 Token 会话管理（替代前端 localStorage 裸奔）
 */
@Component
public class TokenStore {

    private final Map<String, SysUser> sessions = new ConcurrentHashMap<>();

    public String createToken(SysUser user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        sessions.put(token, user);
        return token;
    }

    public SysUser getUser(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        return sessions.get(token);
    }

    public void removeToken(String token) {
        if (token != null) {
            sessions.remove(token);
        }
    }
}
