package com.campus.xianyu.wanted;

import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

public record WantedResponse(
        Long id,
        Long buyerId,
        String itemName,
        BigDecimal budget,
        String expectCondition,
        String description,
        Long campusId,
        String status,
        String createdAt,
        PublicUserResponse publisher
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static WantedResponse from(Wanted wanted, AppUser publisher) {
        return new WantedResponse(
                wanted.getId(),
                wanted.getBuyerId(),
                wanted.getItemName(),
                wanted.getBudget(),
                wanted.getExpectCondition(),
                wanted.getDescription(),
                wanted.getCampusId(),
                wanted.getStatus(),
                wanted.getCreatedAt() == null ? null : wanted.getCreatedAt().format(FORMATTER),
                publisher == null ? null : PublicUserResponse.from(publisher)
        );
    }
}
