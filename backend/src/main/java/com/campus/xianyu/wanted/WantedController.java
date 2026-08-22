package com.campus.xianyu.wanted;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.common.PageResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
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
@RequestMapping("/api/wanted")
public class WantedController {
    private static final List<String> CONDITIONS = List.of("NEW", "LIKE_NEW", "GOOD", "FAIR", "POOR");

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final WantedRepository wantedRepository;

    public WantedController(TokenService tokenService, UserRepository userRepository, WantedRepository wantedRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.wantedRepository = wantedRepository;
    }

    @GetMapping
    public ApiResponse<PageResponse<WantedResponse>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long campusId,
            @RequestParam(required = false) String expectCondition,
            @RequestParam(required = false) Long buyerId,
            @RequestParam(required = false) BigDecimal minBudget,
            @RequestParam(required = false) BigDecimal maxBudget,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size
    ) {
        validatePage(page, size);
        validateBudgetRange(minBudget, maxBudget);
        Specification<Wanted> specification = (root, query, builder) -> builder.equal(root.get("status"), "OPEN");
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase() + "%";
            specification = specification.and((root, query, builder) -> builder.or(
                    builder.like(builder.lower(root.get("itemName")), pattern),
                    builder.like(builder.lower(root.get("description")), pattern)
            ));
        }
        if (campusId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("campusId"), campusId));
        }
        if (expectCondition != null && !expectCondition.isBlank()) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("expectCondition"), expectCondition));
        }
        if (buyerId != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("buyerId"), buyerId));
        }
        if (minBudget != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("budget"), minBudget));
        }
        if (maxBudget != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("budget"), maxBudget));
        }

        Page<Wanted> wantedPage = wantedRepository.findAll(
                specification,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<WantedResponse> content = toResponses(wantedPage.getContent());
        return ApiResponse.ok(PageResponse.from(wantedPage, content));
    }

    @PostMapping
    @Transactional
    public ApiResponse<WantedResponse> create(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody WantedRequest request
    ) {
        AppUser user = requireApprovedStudent(authorization);
        validateCondition(request.expectCondition());
        Wanted wanted = new Wanted();
        fillWanted(wanted, request);
        wanted.setBuyerId(user.getId());
        wanted.setStatus("OPEN");
        return ApiResponse.ok("求购发布成功", WantedResponse.from(wantedRepository.save(wanted), user));
    }

    @GetMapping("/mine")
    public ApiResponse<List<WantedResponse>> mine(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AppUser user = currentUser(authorization);
        return ApiResponse.ok(toResponses(wantedRepository.findByBuyerIdOrderByCreatedAtDesc(user.getId())));
    }

    @GetMapping("/{id}")
    public ApiResponse<WantedResponse> detail(@PathVariable Long id) {
        Wanted wanted = wantedRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("求购信息不存在"));
        AppUser publisher = userRepository.findById(wanted.getBuyerId())
                .orElseThrow(() -> new IllegalArgumentException("发布者不存在"));
        return ApiResponse.ok(WantedResponse.from(wanted, publisher));
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<WantedResponse> update(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id,
            @Valid @RequestBody WantedRequest request
    ) {
        AppUser user = requireApprovedStudent(authorization);
        validateCondition(request.expectCondition());
        Wanted wanted = ownedWanted(id, user.getId());
        if (!"OPEN".equals(wanted.getStatus())) {
            throw new IllegalArgumentException("只有求购中的信息可以修改");
        }
        fillWanted(wanted, request);
        return ApiResponse.ok("求购信息已更新", WantedResponse.from(wantedRepository.save(wanted), user));
    }

    @PutMapping("/{id}/match")
    @Transactional
    public ApiResponse<WantedResponse> match(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        AppUser user = currentUser(authorization);
        Wanted wanted = ownedWanted(id, user.getId());
        if (!"OPEN".equals(wanted.getStatus())) {
            throw new IllegalArgumentException("只有求购中的信息可以标记为已匹配");
        }
        wanted.setStatus("MATCHED");
        return ApiResponse.ok("求购已标记为已匹配", WantedResponse.from(wantedRepository.save(wanted), user));
    }

    @PutMapping("/{id}/close")
    @Transactional
    public ApiResponse<WantedResponse> close(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @PathVariable Long id
    ) {
        AppUser user = currentUser(authorization);
        Wanted wanted = ownedWanted(id, user.getId());
        if ("CLOSED".equals(wanted.getStatus())) {
            throw new IllegalArgumentException("求购已经关闭");
        }
        wanted.setStatus("CLOSED");
        return ApiResponse.ok("求购已关闭", WantedResponse.from(wantedRepository.save(wanted), user));
    }

    private void fillWanted(Wanted wanted, WantedRequest request) {
        wanted.setItemName(request.itemName().trim());
        wanted.setBudget(request.budget());
        wanted.setExpectCondition(request.expectCondition());
        wanted.setDescription(request.description() == null ? null : request.description().trim());
        wanted.setCampusId(request.campusId());
    }

    private Wanted ownedWanted(Long wantedId, Long buyerId) {
        Wanted wanted = wantedRepository.findById(wantedId)
                .orElseThrow(() -> new IllegalArgumentException("求购信息不存在"));
        if (!wanted.getBuyerId().equals(buyerId)) {
            throw new IllegalArgumentException("只能操作自己发布的求购");
        }
        return wanted;
    }

    private AppUser requireApprovedStudent(String authorization) {
        AppUser user = currentUser(authorization);
        if (!"STUDENT".equals(user.getRole())) {
            throw new IllegalArgumentException("管理员不能发布求购");
        }
        if (!"APPROVED".equals(user.getAuthStatus())) {
            throw new IllegalArgumentException("发布求购需要先通过学生实名认证");
        }
        return user;
    }

    private AppUser currentUser(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private void validateCondition(String condition) {
        if (!CONDITIONS.contains(condition)) {
            throw new IllegalArgumentException("期望成色不合法");
        }
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不能小于0");
        }
        if (size < 1 || size > 50) {
            throw new IllegalArgumentException("每页数量必须在1到50之间");
        }
    }

    private void validateBudgetRange(BigDecimal minBudget, BigDecimal maxBudget) {
        if (minBudget != null && minBudget.signum() < 0) {
            throw new IllegalArgumentException("最低预算不能小于0");
        }
        if (maxBudget != null && maxBudget.signum() < 0) {
            throw new IllegalArgumentException("最高预算不能小于0");
        }
        if (minBudget != null && maxBudget != null && minBudget.compareTo(maxBudget) > 0) {
            throw new IllegalArgumentException("最低预算不能高于最高预算");
        }
    }

    private List<WantedResponse> toResponses(List<Wanted> wantedItems) {
        Map<Long, AppUser> publishers = userRepository.findAllById(
                        wantedItems.stream().map(Wanted::getBuyerId).distinct().toList())
                .stream().collect(Collectors.toMap(AppUser::getId, Function.identity()));
        return wantedItems.stream()
                .map(wanted -> WantedResponse.from(wanted, publishers.get(wanted.getBuyerId())))
                .toList();
    }
}
