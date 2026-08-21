package com.campus.xianyu.product;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record ProductResponse(
        Long id,
        Long sellerId,
        String title,
        BigDecimal price,
        Long categoryId,
        String conditionLevel,
        Long campusId,
        String tradeMethod,
        String description,
        String coverUrl,
        String status,
        String auditRemark,
        Integer viewCount,
        String createdAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getPrice(),
                product.getCategoryId(),
                product.getConditionLevel(),
                product.getCampusId(),
                product.getTradeMethod(),
                product.getDescription(),
                product.getCoverUrl(),
                product.getStatus(),
                product.getAuditRemark(),
                product.getViewCount(),
                product.getCreatedAt() == null ? null : product.getCreatedAt().format(FORMATTER)
        );
    }
}