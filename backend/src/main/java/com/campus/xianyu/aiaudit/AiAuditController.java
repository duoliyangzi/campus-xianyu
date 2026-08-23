package com.campus.xianyu.aiaudit;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.product.Product;
import com.campus.xianyu.product.ProductRepository;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-audit")
public class AiAuditController {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final AiAuditLogRepository aiAuditLogRepository;
    private final AiAuditService aiAuditService;

    public AiAuditController(
            TokenService tokenService,
            UserRepository userRepository,
            ProductRepository productRepository,
            AiAuditLogRepository aiAuditLogRepository,
            AiAuditService aiAuditService
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.aiAuditLogRepository = aiAuditLogRepository;
        this.aiAuditService = aiAuditService;
    }

    @PostMapping("/products/{productId}")
    @Transactional
    public ApiResponse<AiAuditResponse> auditProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long productId
    ) {
        requireAdmin(authorization);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        AiAuditLog log = aiAuditService.audit(product);
        AiAuditLog saved = aiAuditLogRepository.save(log);
        return ApiResponse.ok("关键词审核完成", AiAuditResponse.from(saved));
    }

    @GetMapping("/products/{productId}")
    public ApiResponse<List<AiAuditResponse>> listByProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long productId
    ) {
        requireAdmin(authorization);
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        List<AiAuditResponse> logs = aiAuditLogRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(AiAuditResponse::from)
                .toList();
        return ApiResponse.ok(logs);
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
}
