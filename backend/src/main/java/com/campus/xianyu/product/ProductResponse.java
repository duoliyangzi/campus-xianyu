package com.campus.xianyu.product;

import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
        List<String> imageUrls,
        String status,
        String auditRemark,
        Integer viewCount,
        String createdAt,
        PublicUserResponse seller
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ProductResponse from(Product product, AppUser seller) {
        return from(product, seller, List.of());
    }

    public static ProductResponse from(Product product, AppUser seller, List<String> imageUrls) {
        List<String> safeImageUrls = imageUrls == null ? List.of() : imageUrls;
        String coverUrl = product.getCoverUrl();
        if ((coverUrl == null || coverUrl.isBlank()) && !safeImageUrls.isEmpty()) {
            coverUrl = safeImageUrls.get(0);
        }
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
                coverUrl,
                safeImageUrls,
                product.getStatus(),
                product.getAuditRemark(),
                product.getViewCount(),
                product.getCreatedAt() == null ? null : product.getCreatedAt().format(FORMATTER),
                seller == null ? null : PublicUserResponse.from(seller)
        );
    }
}
