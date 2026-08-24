package com.campus.xianyu.order;

import com.campus.xianyu.product.ProductResponse;
import com.campus.xianyu.user.PublicUserResponse;
import java.time.format.DateTimeFormatter;

public record OrderResponse(
        Long id,
        String orderNo,
        Long productId,
        Long buyerId,
        Long sellerId,
        String status,
        String meetTime,
        String meetLocation,
        String remark,
        Long conversationId,
        Boolean buyerConfirmed,
        Boolean sellerConfirmed,
        String createdAt,
        String updatedAt,
        ProductResponse product,
        PublicUserResponse buyer,
        PublicUserResponse seller
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static OrderResponse from(
            TradeOrder order,
            ProductResponse product,
            PublicUserResponse buyer,
            PublicUserResponse seller
    ) {
        return new OrderResponse(
                order.getId(),
                order.getOrderNo(),
                order.getProductId(),
                order.getBuyerId(),
                order.getSellerId(),
                order.getStatus(),
                order.getMeetTime() == null ? null : order.getMeetTime().format(FORMATTER),
                order.getMeetLocation(),
                order.getRemark(),
                order.getConversationId(),
                Boolean.TRUE.equals(order.getBuyerConfirmed()),
                Boolean.TRUE.equals(order.getSellerConfirmed()),
                order.getCreatedAt() == null ? null : order.getCreatedAt().format(FORMATTER),
                order.getUpdatedAt() == null ? null : order.getUpdatedAt().format(FORMATTER),
                product,
                buyer,
                seller
        );
    }
}
