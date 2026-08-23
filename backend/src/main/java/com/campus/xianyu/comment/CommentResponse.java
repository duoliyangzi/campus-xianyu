package com.campus.xianyu.comment;

import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import java.time.format.DateTimeFormatter;

public record CommentResponse(
        Long id,
        Long productId,
        Long userId,
        String content,
        String createdAt,
        PublicUserResponse user
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static CommentResponse from(Comment comment, AppUser user) {
        return new CommentResponse(
                comment.getId(),
                comment.getProductId(),
                comment.getUserId(),
                comment.getContent(),
                comment.getCreatedAt() == null ? null : comment.getCreatedAt().format(FORMATTER),
                user == null ? null : PublicUserResponse.from(user)
        );
    }
}
