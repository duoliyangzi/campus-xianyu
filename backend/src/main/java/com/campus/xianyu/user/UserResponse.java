package com.campus.xianyu.user;

import java.time.format.DateTimeFormatter;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String phone,
        String role,
        String studentNo,
        String college,
        String authStatus,
        String authRemark,
        String status,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static UserResponse from(AppUser user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getRole(),
                user.getStudentNo(),
                user.getCollege(),
                user.getAuthStatus(),
                user.getAuthRemark(),
                user.getStatus(),
                user.getCreatedAt() == null ? null : user.getCreatedAt().format(FORMATTER)
        );
    }
}
