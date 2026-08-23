package com.campus.xianyu.comment;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.common.PageResponse;
import com.campus.xianyu.product.Product;
import com.campus.xianyu.product.ProductRepository;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/comments")
public class CommentController {
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CommentRepository commentRepository;

    public CommentController(
            TokenService tokenService,
            UserRepository userRepository,
            ProductRepository productRepository,
            CommentRepository commentRepository
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.commentRepository = commentRepository;
    }

    @PostMapping
    @Transactional
    public ApiResponse<CommentResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody CommentRequest request
    ) {
        AppUser user = requireLogin(authorization);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));
        if (!"PUBLISHED".equals(product.getStatus())) {
            throw new IllegalArgumentException("只能对已发布商品留言");
        }

        Comment comment = new Comment();
        comment.setProductId(request.productId());
        comment.setUserId(user.getId());
        comment.setContent(request.content().trim());
        Comment saved = commentRepository.save(comment);
        return ApiResponse.ok("留言成功", CommentResponse.from(saved, user));
    }

    @GetMapping
    public ApiResponse<PageResponse<CommentResponse>> list(
            @RequestParam Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (page < 0 || size <= 0 || size > 50) {
            throw new IllegalArgumentException("分页参数不合法");
        }
        productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("商品不存在"));

        Page<Comment> comments = commentRepository.findByProductIdOrderByCreatedAtDesc(
                productId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        Map<Long, AppUser> users = userRepository.findAllById(
                comments.getContent().stream().map(Comment::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        List<CommentResponse> content = comments.getContent().stream()
                .map(comment -> CommentResponse.from(comment, users.get(comment.getUserId())))
                .toList();
        return ApiResponse.ok(PageResponse.from(comments, content));
    }

    private AppUser requireLogin(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
