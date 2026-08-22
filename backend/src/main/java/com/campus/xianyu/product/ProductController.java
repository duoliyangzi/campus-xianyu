package com.campus.xianyu.product;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.common.PageResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/api/products")
public class ProductController {
    private static final int MAX_IMAGE_COUNT = 9;

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;

    public ProductController(
            TokenService tokenService,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<ProductResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String conditionLevel,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Long sellerId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        validatePage(page, size);
        if (minPrice != null && minPrice.signum() < 0) {
            throw new IllegalArgumentException("最低价格不能小于0");
        }
        if (maxPrice != null && maxPrice.signum() < 0) {
            throw new IllegalArgumentException("最高价格不能小于0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new IllegalArgumentException("最低价格不能高于最高价格");
        }

        Specification<Product> specification = (root, query, builder) -> builder.equal(root.get("status"), "PUBLISHED");
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("title")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
            ));
        }
        if (categoryId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("categoryId"), categoryId));
        }
        if (campusId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("campusId"), campusId));
        }
        if (conditionLevel != null && !conditionLevel.isBlank()) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("conditionLevel"), conditionLevel));
        }
        if (minPrice != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("price"), minPrice));
        }
        if (maxPrice != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("price"), maxPrice));
        }
        if (sellerId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("sellerId"), sellerId));
        }

        Page<Product> products = productRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<ProductResponse> content = toResponses(products.getContent());
        return ApiResponse.ok(PageResponse.from(products, content));
    }

    @PostMapping
    @Transactional
    public ApiResponse<ProductResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody ProductRequest request
    ) {
        AppUser user = requireApprovedStudent(authorization);
        List<String> imageUrls = normalizeImageUrls(request);
        Product product = new Product();
        fillProduct(product, request, imageUrls);
        product.setSellerId(user.getId());
        product.setStatus("PENDING");
        product.setViewCount(0);
        Product savedProduct = productRepository.save(product);
        saveProductImages(savedProduct.getId(), imageUrls);
        return ApiResponse.ok("商品已提交审核", ProductResponse.from(savedProduct, user, imageUrls));
    }

    @GetMapping("/mine")
    public ApiResponse<List<ProductResponse>> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AppUser user = currentUser(authorization);
        List<ProductResponse> products = toResponses(productRepository.findBySellerIdOrderByCreatedAtDesc(user.getId()));
        return ApiResponse.ok(products);
    }

    @GetMapping("/{id}")
    public ApiResponse<ProductResponse> detail(@PathVariable Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!"PUBLISHED".equals(product.getStatus())) {
            throw new IllegalArgumentException("商品不存在或尚未发布");
        }
        AppUser seller = userRepository.findById(product.getSellerId())
                .orElseThrow(() -> new IllegalArgumentException("卖家不存在"));
        return ApiResponse.ok(ProductResponse.from(product, seller, imageUrlsFor(product.getId())));
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
        List<String> imageUrls = normalizeImageUrls(request);
        fillProduct(product, request, imageUrls);
        product.setStatus("PENDING");
        product.setAuditRemark(null);
        Product savedProduct = productRepository.save(product);
        saveProductImages(savedProduct.getId(), imageUrls);
        return ApiResponse.ok("商品已更新并重新提交审核", ProductResponse.from(savedProduct, user, imageUrls));
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
        Product savedProduct = productRepository.save(product);
        return ApiResponse.ok("商品已下架", ProductResponse.from(savedProduct, user, imageUrlsFor(savedProduct.getId())));
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
        Product savedProduct = productRepository.save(product);
        return ApiResponse.ok("商品已恢复并重新提交审核", ProductResponse.from(savedProduct, user, imageUrlsFor(savedProduct.getId())));
    }

    private void fillProduct(Product product, ProductRequest request, List<String> imageUrls) {
        product.setTitle(request.title());
        product.setPrice(request.price());
        product.setCategoryId(request.categoryId());
        product.setConditionLevel(request.conditionLevel());
        product.setCampusId(request.campusId());
        product.setTradeMethod(request.tradeMethod());
        product.setDescription(request.description());
        product.setCoverUrl(imageUrls.isEmpty() ? null : imageUrls.get(0));
    }

    private List<String> normalizeImageUrls(ProductRequest request) {
        List<String> urls = new ArrayList<>();
        if (request.imageUrls() != null) {
            request.imageUrls().forEach(url -> {
                if (url != null && !url.isBlank()) {
                    urls.add(url.trim());
                }
            });
        }
        if (urls.isEmpty() && request.coverUrl() != null && !request.coverUrl().isBlank()) {
            urls.add(request.coverUrl().trim());
        }
        List<String> distinctUrls = new ArrayList<>(new LinkedHashMap<String, Boolean>() {{
            urls.forEach(url -> put(url, Boolean.TRUE));
        }}.keySet());
        if (distinctUrls.isEmpty()) {
            throw new IllegalArgumentException("请至少上传一张商品图片");
        }
        if (distinctUrls.size() > MAX_IMAGE_COUNT) {
            throw new IllegalArgumentException("商品图片最多上传9张");
        }
        return distinctUrls;
    }

    private void saveProductImages(Long productId, List<String> imageUrls) {
        productImageRepository.deleteByProductId(productId);
        List<ProductImage> images = new ArrayList<>();
        for (int index = 0; index < imageUrls.size(); index++) {
            ProductImage image = new ProductImage();
            image.setProductId(productId);
            image.setUrl(imageUrls.get(index));
            image.setSortOrder(index);
            images.add(image);
        }
        productImageRepository.saveAll(images);
    }

    private List<String> imageUrlsFor(Long productId) {
        return productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(productId)
                .stream()
                .map(ProductImage::getUrl)
                .toList();
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

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("每页数量必须在1到50之间");
        }
    }

    private List<ProductResponse> toResponses(List<Product> products) {
        if (products.isEmpty()) {
            return List.of();
        }
        Map<Long, AppUser> sellers = userRepository.findAllById(
                        products.stream().map(Product::getSellerId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        Map<Long, List<String>> imageUrlsByProduct = productImageRepository.findByProductIdInOrderByProductIdAscSortOrderAscIdAsc(
                        products.stream().map(Product::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        ProductImage::getProductId,
                        LinkedHashMap::new,
                        Collectors.mapping(ProductImage::getUrl, Collectors.toList())
                ));
        return products.stream()
                .map(product -> ProductResponse.from(
                        product,
                        sellers.get(product.getSellerId()),
                        imageUrlsByProduct.getOrDefault(product.getId(), List.of())
                ))
                .toList();
    }
}
