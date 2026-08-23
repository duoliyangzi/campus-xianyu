package com.campus.xianyu.aiaudit;

import java.time.format.DateTimeFormatter;

public record AiAuditResponse(
        Long id,
        Long productId,
        String title,
        String contentSnap,
        String riskLevel,
        String suggestion,
        String reason,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static AiAuditResponse from(AiAuditLog log) {
        return new AiAuditResponse(
                log.getId(),
                log.getProductId(),
                log.getTitle(),
                log.getContentSnap(),
                log.getRiskLevel(),
                log.getSuggestion(),
                log.getReason(),
                log.getCreatedAt() == null ? null : log.getCreatedAt().format(FORMATTER)
        );
    }
}
