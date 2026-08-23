package com.campus.xianyu.report;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final Set<String> TARGET_TYPES = Set.of("PRODUCT", "USER", "WANTED", "COMMENT");

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final ReportReasonRepository reportReasonRepository;

    public ReportController(
            TokenService tokenService,
            UserRepository userRepository,
            ReportRepository reportRepository,
            ReportReasonRepository reportReasonRepository
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.reportRepository = reportRepository;
        this.reportReasonRepository = reportReasonRepository;
    }

    @GetMapping("/reasons")
    public ApiResponse<List<ReportReason>> reasons() {
        return ApiResponse.ok(reportReasonRepository.findByStatusOrderBySortOrderAscIdAsc("ENABLED"));
    }

    @PostMapping
    @Transactional
    public ApiResponse<ReportResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        AppUser reporter = requireLogin(authorization);
        if (!TARGET_TYPES.contains(request.targetType())) {
            throw new IllegalArgumentException("举报对象类型不合法");
        }
        ReportReason reason = reportReasonRepository.findById(request.reasonId())
                .orElseThrow(() -> new IllegalArgumentException("举报原因不存在"));

        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setReasonId(reason.getId());
        report.setTargetType(request.targetType());
        report.setTargetId(request.targetId());
        report.setDescription(request.description());
        report.setStatus("PENDING");
        Report saved = reportRepository.save(report);
        return ApiResponse.ok("举报已提交", ReportResponse.from(saved, PublicUserResponse.from(reporter), reason.getName()));
    }

    private AppUser requireLogin(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
