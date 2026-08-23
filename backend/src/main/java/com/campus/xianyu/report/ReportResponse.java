package com.campus.xianyu.report;

import com.campus.xianyu.user.PublicUserResponse;
import java.time.format.DateTimeFormatter;

public record ReportResponse(
        Long id,
        Long reporterId,
        PublicUserResponse reporter,
        Long reasonId,
        String reasonName,
        String targetType,
        Long targetId,
        String description,
        String status,
        String handleResult,
        String createdAt,
        String updatedAt
) {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static ReportResponse from(Report report, PublicUserResponse reporter, String reasonName) {
        return new ReportResponse(
                report.getId(),
                report.getReporterId(),
                reporter,
                report.getReasonId(),
                reasonName,
                report.getTargetType(),
                report.getTargetId(),
                report.getDescription(),
                report.getStatus(),
                report.getHandleResult(),
                report.getCreatedAt() == null ? null : report.getCreatedAt().format(FORMATTER),
                report.getUpdatedAt() == null ? null : report.getUpdatedAt().format(FORMATTER)
        );
    }
}
