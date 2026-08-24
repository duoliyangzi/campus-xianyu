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
        String[] parts = token.split("\\.");
        String signature = parts[2];
        int changedIndex = signature.length() / 2;
        char changedCharacter = signature.charAt(changedIndex) == 'a' ? 'b' : 'a';
        String tamperedSignature = signature.substring(0, changedIndex)
                + changedCharacter
                + signature.substring(changedIndex + 1);
        String tampered = parts[0] + "." + parts[1] + "." + tamperedSignature;

        assertThat(tokenService.findUserId("Bearer " + tampered)).isEmpty();
    }

    @Test
    void malformedOrMissingTokenIsRejected() {
        assertThat(tokenService.findUserId(null)).isEmpty();
        assertThat(tokenService.findUserId("Bearer not-a-jwt")).isEmpty();
    }
}
