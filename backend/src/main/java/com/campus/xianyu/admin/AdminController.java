package com.campus.xianyu.admin;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import com.campus.xianyu.user.UserResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public AdminController(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/auth-applications")
    public ApiResponse<List<UserResponse>> authApplications(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "PENDING") String status
    ) {
        requireAdmin(authorization);
        List<UserResponse> users = userRepository
                .findByRoleAndAuthStatusOrderByUpdatedAtDesc("STUDENT", status)
                .stream()
                .map(UserResponse::from)
                .toList();
        return ApiResponse.ok(users);
    }

    @PostMapping("/auth-applications/{userId}/review")
    @Transactional
    public ApiResponse<UserResponse> reviewAuth(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long userId,
            @Valid @RequestBody AuthReviewRequest request
    ) {
        requireAdmin(authorization);
        if (!"APPROVED".equals(request.authStatus()) && !"REJECTED".equals(request.authStatus())) {
            throw new IllegalArgumentException("审核状态只能是 APPROVED 或 REJECTED");
        }

        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("只能审核学生账号");
        }
        if (!"PENDING".equals(user.getAuthStatus())) {
            throw new IllegalArgumentException("只能审核待审核申请");
        }

        user.setAuthStatus(request.authStatus());
        user.setAuthRemark(request.authRemark() == null || request.authRemark().isBlank()
                ? defaultRemark(request.authStatus())
                : request.authRemark());
        return ApiResponse.ok("审核完成", UserResponse.from(userRepository.save(user)));
    }

    private AppUser requireAdmin(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!"ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("需要管理员权限");
        }
        return user;
    }

    private String defaultRemark(String authStatus) {
        return "APPROVED".equals(authStatus) ? "实名认证审核通过" : "实名认证审核未通过";
    }
}