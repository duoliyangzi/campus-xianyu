package com.campus.xianyu.admin;

import com.campus.xianyu.aiaudit.AiAuditLogRepository;
import com.campus.xianyu.aiaudit.AiAuditResponse;
import com.campus.xianyu.aiaudit.AiAuditService;
import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.common.PageResponse;
import com.campus.xianyu.dict.Category;
import com.campus.xianyu.dict.CategoryService;
import com.campus.xianyu.product.Product;
import com.campus.xianyu.product.ProductImageRepository;
import com.campus.xianyu.product.ProductRepository;
import com.campus.xianyu.product.ProductResponse;
import com.campus.xianyu.report.Report;
import com.campus.xianyu.report.ReportHandleRequest;
import com.campus.xianyu.report.ReportReasonRepository;
import com.campus.xianyu.report.ReportRepository;
import com.campus.xianyu.report.ReportResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import com.campus.xianyu.user.UserRepository;
import com.campus.xianyu.user.UserResponse;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminCatalogController {
    private static final Set<String> REPORT_STATUSES = Set.of("RESOLVED", "REJECTED");

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final CategoryService categoryService;
    private final ReportRepository reportRepository;
    private final ReportReasonRepository reportReasonRepository;
    private final AiAuditLogRepository aiAuditLogRepository;
    private final AiAuditService aiAuditService;

    public AdminCatalogController(
            TokenService tokenService,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            CategoryService categoryService,
            ReportRepository reportRepository,
            ReportReasonRepository reportReasonRepository,
            AiAuditLogRepository aiAuditLogRepository,
            AiAuditService aiAuditService
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.categoryService = categoryService;
        this.reportRepository = reportRepository;
        this.reportReasonRepository = reportReasonRepository;
        this.aiAuditLogRepository = aiAuditLogRepository;
        this.aiAuditService = aiAuditService;
    }

    @GetMapping("/products")
    public ApiResponse<PageResponse<ProductResponse>> products(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        requireAdmin(authorization);
        validatePage(page, size);
        Specification<Product> specification = (root, query, builder) -> builder.equal(root.get("status"), status);
        Page<Product> products = productRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ProductResponse> content = toProductResponses(products.getContent());
        return ApiResponse.ok(PageResponse.from(products, content));
    }

    @PutMapping("/products/{id}/audit")
    @Transactional
    public ApiResponse<ProductResponse> auditProduct(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody ProductAuditRequest request
    ) {
        requireAdmin(authorization);
        if (!"PUBLISHED".equals(request.status()) && !"REJECTED".equals(request.status())) {
            throw new IllegalArgumentException("审核状态只能是 PUBLISHED 或 REJECTED");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!"PENDING".equals(product.getStatus())) {
            throw new IllegalArgumentException("只能审核待审核商品");
        }
        product.setStatus(request.status());
        product.setAuditRemark(request.auditRemark());
        Product saved = productRepository.save(product);
        aiAuditLogRepository.save(aiAuditService.audit(saved));
        AppUser seller = userRepository.findById(saved.getSellerId()).orElseThrow();
        List<String> imageUrls = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(saved.getId())
                .stream().map(image -> image.getUrl()).toList();
        return ApiResponse.ok("审核完成", ProductResponse.from(saved, seller, imageUrls));
    }

    @GetMapping("/reports")
    public ApiResponse<PageResponse<ReportResponse>> reports(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(defaultValue = "PENDING") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        requireAdmin(authorization);
        validatePage(page, size);
        Page<Report> reports = reportRepository.findByStatusOrderByCreatedAtDesc(
                status,
                PageRequest.of(page, size)
        );
        Map<Long, AppUser> reporters = userRepository.findAllById(
                reports.getContent().stream().map(Report::getReporterId).distinct().toList()
        ).stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<Long, String> reasonNames = reportReasonRepository.findAll().stream()
                .collect(Collectors.toMap(reason -> reason.getId(), reason -> reason.getName()));
        List<ReportResponse> content = reports.getContent().stream()
                .map(report -> ReportResponse.from(
                        report,
                        PublicUserResponse.from(reporters.get(report.getReporterId())),
                        reasonNames.get(report.getReasonId())
                ))
                .toList();
        return ApiResponse.ok(PageResponse.from(reports, content));
    }

    @PutMapping("/reports/{id}/handle")
    @Transactional
    public ApiResponse<ReportResponse> handleReport(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody ReportHandleRequest request
    ) {
        AppUser admin = requireAdmin(authorization);
        if (!REPORT_STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("处理状态只能是 RESOLVED 或 REJECTED");
        }
        Report report = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("举报不存在"));
        report.setStatus(request.status());
        report.setHandlerId(admin.getId());
        report.setHandleResult(request.handleResult());
        Report saved = reportRepository.save(report);
        if ("RESOLVED".equals(request.status()) && "PRODUCT".equals(saved.getTargetType())) {
            productRepository.findById(saved.getTargetId()).ifPresent(product -> {
                product.setStatus("OFF_SHELF");
                product.setAuditRemark("举报处理：商品已下架");
                productRepository.save(product);
            });
        }
        AppUser reporter = userRepository.findById(saved.getReporterId()).orElseThrow();
        String reasonName = reportReasonRepository.findById(saved.getReasonId()).map(reason -> reason.getName()).orElse("");
        return ApiResponse.ok("处理完成", ReportResponse.from(saved, PublicUserResponse.from(reporter), reasonName));
    }

    @GetMapping("/users")
    public ApiResponse<PageResponse<UserResponse>> users(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        requireAdmin(authorization);
        validatePage(page, size);
        Page<AppUser> users = userRepository.searchStudents(keyword, PageRequest.of(page, size));
        List<UserResponse> content = users.getContent().stream().map(UserResponse::from).toList();
        return ApiResponse.ok(PageResponse.from(users, content));
    }

    @PutMapping("/users/{id}/status")
    @Transactional
    public ApiResponse<UserResponse> updateUserStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody UserStatusRequest request
    ) {
        requireAdmin(authorization);
        if (!"ACTIVE".equals(request.status()) && !"BANNED".equals(request.status())) {
            throw new IllegalArgumentException("用户状态只能是 ACTIVE 或 BANNED");
        }
        AppUser user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("只能管理学生账号");
        }
        user.setStatus(request.status());
        return ApiResponse.ok("用户状态已更新", UserResponse.from(userRepository.save(user)));
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> listCategories(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        return ApiResponse.ok(categoryService.listForAdmin());
    }

    @PostMapping("/categories")
    @Transactional
    public ApiResponse<Category> createCategory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CategoryRequest request
    ) {
        requireAdmin(authorization);
        return ApiResponse.ok("分类创建成功", categoryService.create(request.name()));
    }

    @PutMapping("/categories/{id}")
    @Transactional
    public ApiResponse<Category> updateCategory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request
    ) {
        requireAdmin(authorization);
        return ApiResponse.ok("分类更新成功", categoryService.update(id, request.name()));
    }

    @DeleteMapping("/categories/{id}")
    @Transactional
    public ApiResponse<Void> deleteCategory(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        requireAdmin(authorization);
        categoryService.delete(id);
        return ApiResponse.ok("分类已删除", null);
    }

    @GetMapping("/ai-audit/products/{productId}")
    public ApiResponse<List<AiAuditResponse>> aiAuditLogs(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long productId
    ) {
        requireAdmin(authorization);
        List<AiAuditResponse> logs = aiAuditLogRepository.findByProductIdOrderByCreatedAtDesc(productId)
                .stream()
                .map(AiAuditResponse::from)
                .toList();
        return ApiResponse.ok(logs);
    }

    private List<ProductResponse> toProductResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, AppUser> sellers = userRepository.findAllById(
                products.stream().map(Product::getSellerId).distinct().toList()
        ).stream().collect(Collectors.toMap(AppUser::getId, Function.identity(), (a, b) -> a, LinkedHashMap::new));
        Map<Long, List<String>> imageUrlsByProduct = productImageRepository
                .findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        image -> image.getProductId(),
                        LinkedHashMap::new,
                        Collectors.mapping(image -> image.getUrl(), Collectors.toList())
                ));
        return products.stream()
                .map(product -> ProductResponse.from(
                        product,
                        sellers.get(product.getSellerId()),
                        imageUrlsByProduct.getOrDefault(product.getId(), List.of())
                ))
                .toList();
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

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("每页数量必须在1到50之间");
        }
    }
}
