package com.campus.xianyu.product;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public ProductController(TokenService tokenService, UserRepository userRepository, ProductRepository productRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @PostMapping
    @Transactional
    public ApiResponse<ProductResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ProductRequest request
    ) {
        AppUser user = requireApprovedStudent(authorization);
        Product product = new Product();
        fillProduct(product, request);
        product.setSellerId(user.getId());
        product.setStatus("PENDING");
        product.setViewCount(0);
        return ApiResponse.ok("商品已提交审核", ProductResponse.from(productRepository.save(product)));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ProductResponse>> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AppUser user = currentUser(authorization);
        List<ProductResponse> products = productRepository.findBySellerIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(ProductResponse::from)
                .toList();
        return ApiResponse.ok(products);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        return ApiResponse.ok(ProductResponse.from(product));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<ProductResponse> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request
    ) {
        AppUser user = requireApprovedStudent(authorization);
        Product product = ownedProduct(id, user.getId());
        if ("OFF_SHELF".equals(product.getStatus())) {
            throw new IllegalArgumentException("已下架商品不能修改");
        }
        fillProduct(product, request);
        product.setStatus("PENDING");
        product.setAuditRemark(null);
        return ApiResponse.ok("商品已更新并重新提交审核", ProductResponse.from(productRepository.save(product)));
    }

    @PutMapping("/{id}/off-shelf")
    @Transactional
    public ApiResponse<ProductResponse> offShelf(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        AppUser user = currentUser(authorization);
        Product product = ownedProduct(id, user.getId());
        product.setStatus("OFF_SHELF");
        return ApiResponse.ok("商品已下架", ProductResponse.from(productRepository.save(product)));
    }


    @PutMapping("/{id}/restore")
    @Transactional
    public ApiResponse<ProductResponse> restore(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        AppUser user = requireApprovedStudent(authorization);
        Product product = ownedProduct(id, user.getId());
        if (!"OFF_SHELF".equals(product.getStatus())) {
            throw new IllegalArgumentException("只有已下架商品可以恢复上架");
        }
        product.setStatus("PENDING");
        product.setAuditRemark(null);
        return ApiResponse.ok("商品已恢复并重新提交审核", ProductResponse.from(productRepository.save(product)));
    }
    private void fillProduct(Product product, ProductRequest request) {
        product.setTitle(request.title());
        product.setPrice(request.price());
        product.setCategoryId(request.categoryId());
        product.setConditionLevel(request.conditionLevel());
        product.setCampusId(request.campusId());
        product.setTradeMethod(request.tradeMethod());
        product.setDescription(request.description());
        product.setCoverUrl(request.coverUrl());
    }

    private Product ownedProduct(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!product.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("只能操作自己发布的商品");
        }
        return product;
    }

    private AppUser requireApprovedStudent(String authorization) {
        AppUser user = currentUser(authorization);
        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("管理员不能发布商品");
        }
        if (!"APPROVED".equals(user.getAuthStatus())) {
            throw new IllegalArgumentException("发布商品需要先通过学生实名认证");
        }
        return user;
    }

    private AppUser currentUser(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}