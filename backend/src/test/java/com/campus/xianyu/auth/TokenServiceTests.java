package com.campus.xianyu.auth;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServiceTests {
    private final TokenService tokenService = new TokenService(
            "test-only-jwt-secret-with-at-least-32-characters",
            3600
    );

    @Test
    void issuedTokenCanBeVerified() {
        String token = tokenService.issueToken(42L);

        assertThat(tokenService.findUserId("Bearer " + token)).contains(42L);
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = tokenService.issueToken(42L);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("a") ? "b" : "a");

        assertThat(tokenService.findUserId("Bearer " + tampered)).isEmpty();
    }

    @Test
    void malformedOrMissingTokenIsRejected() {
        assertThat(tokenService.findUserId(null)).isEmpty();
        assertThat(tokenService.findUserId("Bearer not-a-jwt")).isEmpty();
    }
}
