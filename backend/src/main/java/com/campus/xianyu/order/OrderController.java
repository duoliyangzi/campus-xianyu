package com.campus.xianyu.order;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.product.Product;
import com.campus.xianyu.product.ProductImageRepository;
import com.campus.xianyu.product.ProductRepository;
import com.campus.xianyu.product.ProductResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.PublicUserResponse;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
@RequestMapping("/api/orders")
public class OrderController {
    private static final Set<String> STATUSES = Set.of("PENDING_CHAT", "PENDING_TRADE", "COMPLETED");
    private static final DateTimeFormatter ORDER_NO_FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final OrderRepository orderRepository;

    public OrderController(
            TokenService tokenService,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductImageRepository productImageRepository,
            OrderRepository orderRepository
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productImageRepository = productImageRepository;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    @Transactional
    public ApiResponse<OrderResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody OrderCreateRequest request
    ) {
        AppUser buyer = requireLogin(authorization);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!"PUBLISHED".equals(product.getStatus())) {
            throw new IllegalArgumentException("只能购买已发布商品");
        }
        if (product.getSellerId().equals(buyer.getId())) {
            throw new IllegalArgumentException("不能购买自己的商品");
        }

        TradeOrder order = new TradeOrder();
        order.setOrderNo(generateOrderNo());
        order.setProductId(product.getId());
        order.setBuyerId(buyer.getId());
        order.setSellerId(product.getSellerId());
        order.setStatus("PENDING_CHAT");
        order.setMeetTime(request.meetTime());
        order.setMeetLocation(request.meetLocation());
        order.setRemark(request.remark());
        order.setConversationId(request.conversationId());
        TradeOrder saved = orderRepository.save(order);

        AppUser seller = userRepository.findById(product.getSellerId()).orElseThrow();
        return ApiResponse.ok("订单创建成功", toResponse(saved, product, buyer, seller));
    }

    @GetMapping
    public ApiResponse<List<OrderResponse>> list(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AppUser user = requireLogin(authorization);
        List<TradeOrder> orders = orderRepository.findByBuyerIdOrSellerIdOrderByCreatedAtDesc(user.getId(), user.getId());
        return ApiResponse.ok(toResponses(orders));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> detail(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        AppUser user = requireLogin(authorization);
        TradeOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        requireParticipant(order, user.getId());
        Product product = productRepository.findById(order.getProductId()).orElseThrow();
        AppUser buyer = userRepository.findById(order.getBuyerId()).orElseThrow();
        AppUser seller = userRepository.findById(order.getSellerId()).orElseThrow();
        return ApiResponse.ok(toResponse(order, product, buyer, seller));
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ApiResponse<OrderResponse> updateStatus(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request
    ) {
        AppUser user = requireLogin(authorization);
        if (!STATUSES.contains(request.status())) {
            throw new IllegalArgumentException("订单状态不合法");
        }

        TradeOrder order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        requireParticipant(order, user.getId());
        validateStatusTransition(order.getStatus(), request.status());

        order.setStatus(request.status());
        if (request.meetTime() != null) {
            order.setMeetTime(request.meetTime());
        }
        if (request.meetLocation() != null && !request.meetLocation().isBlank()) {
            order.setMeetLocation(request.meetLocation().trim());
        }
        if (request.remark() != null && !request.remark().isBlank()) {
            order.setRemark(request.remark().trim());
        }
        TradeOrder saved = orderRepository.save(order);

        Product product = productRepository.findById(saved.getProductId()).orElseThrow();
        if ("COMPLETED".equals(request.status())) {
            product.setStatus("OFF_SHELF");
            productRepository.save(product);
        }
        AppUser buyer = userRepository.findById(saved.getBuyerId()).orElseThrow();
        AppUser seller = userRepository.findById(saved.getSellerId()).orElseThrow();
        return ApiResponse.ok("订单状态已更新", toResponse(saved, product, buyer, seller));
    }

    private void validateStatusTransition(String current, String next) {
        if (current.equals(next)) {
            return;
        }
        if ("PENDING_CHAT".equals(current) && "PENDING_TRADE".equals(next)) {
            return;
        }
        if ("PENDING_TRADE".equals(current) && "COMPLETED".equals(next)) {
            return;
        }
        if ("PENDING_CHAT".equals(current) && "COMPLETED".equals(next)) {
            return;
        }
        throw new IllegalArgumentException("不允许的状态流转");
    }

    private String generateOrderNo() {
        return "O" + LocalDateTime.now().format(ORDER_NO_FORMAT) + (int) (Math.random() * 900 + 100);
    }

    private List<OrderResponse> toResponses(List<TradeOrder> orders) {
        Map<Long, Product> products = productRepository.findAllById(
                orders.stream().map(TradeOrder::getProductId).distinct().toList()
        ).stream().collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, AppUser> users = userRepository.findAllById(
                orders.stream().flatMap(order -> java.util.stream.Stream.of(order.getBuyerId(), order.getSellerId())).distinct().toList()
        ).stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));

        return orders.stream()
                .map(order -> toResponse(
                        order,
                        products.get(order.getProductId()),
                        users.get(order.getBuyerId()),
                        users.get(order.getSellerId())
                ))
                .toList();
    }

    private OrderResponse toResponse(TradeOrder order, Product product, AppUser buyer, AppUser seller) {
        List<String> imageUrls = productImageRepository.findByProductIdOrderBySortOrderAscIdAsc(product.getId())
                .stream()
                .map(image -> image.getUrl())
                .toList();
        return OrderResponse.from(
                order,
                ProductResponse.from(product, seller, imageUrls),
                PublicUserResponse.from(buyer),
                PublicUserResponse.from(seller)
        );
    }

    private void requireParticipant(TradeOrder order, Long userId) {
        if (!order.getBuyerId().equals(userId) && !order.getSellerId().equals(userId)) {
            throw new IllegalArgumentException("无权访问该订单");
        }
    }

    private AppUser requireLogin(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
