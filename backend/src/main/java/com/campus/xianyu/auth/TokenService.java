package com.campus.xianyu.auth;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private final Map<String, Long> tokenUserIds = new ConcurrentHashMap<>();

    public String issueToken(Long userId) {
        String token = UUID.randomUUID().toString();
        tokenUserIds.put(token, userId);
        return token;
    }

    public Optional<Long> findUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        return Optional.ofNullable(tokenUserIds.get(authorization.substring(7)));
    }
}
