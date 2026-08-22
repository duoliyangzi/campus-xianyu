package com.campus.xianyu.user;

import java.time.format.DateTimeFormatter;

public record PublicUserResponse(
        Long id,
        String nickname,
        String avatarUrl,
        String college,
        String authStatus,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static PublicUserResponse from(AppUser user) {
        return new PublicUserResponse(
                user.getId(),
                user.getNickname() == null || user.getNickname().isBlank() ? "校园用户" : user.getNickname(),
                user.getAvatarUrl(),
                user.getCollege(),
                user.getAuthStatus(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().format(FORMATTER)
        );
    }
}
