package com.campus.xianyu.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
    private static final String JWT_HEADER = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationSeconds;

    public TokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds:86400}") long expirationSeconds
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("JWT签名密钥至少需要32个字符");
        }
        if (expirationSeconds < 300) {
            throw new IllegalArgumentException("JWT有效期不能少于300秒");
        }
        this.objectMapper = new ObjectMapper();
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.expirationSeconds = expirationSeconds;
    }

    public String issueToken(Long userId) {
        long issuedAt = Instant.now().getEpochSecond();
        String header = encode(JWT_HEADER.getBytes(StandardCharsets.UTF_8));
        try {
            byte[] payloadBytes = objectMapper.writeValueAsBytes(Map.of(
                    "sub", userId.toString(),
                    "iat", issuedAt,
                    "exp", issuedAt + expirationSeconds
            ));
            String payload = encode(payloadBytes);
            String unsignedToken = header + "." + payload;
            return unsignedToken + "." + encode(sign(unsignedToken));
        } catch (Exception exception) {
            throw new IllegalStateException("无法生成登录令牌", exception);
        }
    }

    public Optional<Long> findUserId(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorization.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        try {
            String unsignedToken = parts[0] + "." + parts[1];
            byte[] suppliedSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!java.security.MessageDigest.isEqual(sign(unsignedToken), suppliedSignature)) {
                return Optional.empty();
            }
            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            if (!payload.hasNonNull("sub") || !payload.hasNonNull("exp")) {
                return Optional.empty();
            }
            if (payload.get("exp").asLong() <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Long.valueOf(payload.get("sub").asText()));
        } catch (Exception exception) {
            return Optional.empty();
        }
    }

    private byte[] sign(String content) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(content.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }
}
