package com.campus.xianyu.user;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final TokenService tokenService;
    private final UserRepository userRepository;

    public UserController(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AppUser user = currentUser(authorization);
        return ApiResponse.ok(UserResponse.from(user));
    }

    @GetMapping("/{id}/public")
    public ApiResponse<PublicUserResponse> publicProfile(@PathVariable Long id) {
        AppUser user = userRepository.findById(id)
                .filter(item -> "STUDENT".equals(item.getRole()) && "ACTIVE".equals(item.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return ApiResponse.ok(PublicUserResponse.from(user));
    }

    @PostMapping("/auth")
    public ApiResponse<UserResponse> submitAuth(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody StudentAuthRequest request
    ) {
        AppUser user = currentUser(authorization);
        if ("ADMIN".equals(user.getRole())) {
            throw new IllegalArgumentException("管理员账号无需学生认证");
        }
        user.setStudentNo(request.studentNo());
        user.setCollege(request.college());
        user.setAuthStatus("PENDING");
        user.setAuthRemark("认证已提交，等待管理员审核");
        return ApiResponse.ok("认证已提交，等待管理员审核", UserResponse.from(userRepository.save(user)));
    }

    private AppUser currentUser(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
