package com.campus.xianyu.ai;

import com.campus.xianyu.auth.TokenService;
import com.campus.xianyu.common.ApiResponse;
import com.campus.xianyu.user.AppUser;
import com.campus.xianyu.user.UserRepository;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final AiGatewayService aiGatewayService;
    private final AiChatHistoryService historyService;

    public AiController(
            TokenService tokenService,
            UserRepository userRepository,
            AiGatewayService aiGatewayService,
            AiChatHistoryService historyService
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.aiGatewayService = aiGatewayService;
        this.historyService = historyService;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(aiGatewayService.health());
    }

    @GetMapping("/chat/history")
    public ApiResponse<List<Map<String, Object>>> history(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(value = "limit", defaultValue = "50") int limit
    ) {
        AppUser user = requireLogin(authorization);
        return ApiResponse.ok(historyService.listRecentMessages(user.getId(), limit));
    }

    @DeleteMapping("/chat/history")
    public ApiResponse<Map<String, Object>> clearHistory(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        AppUser user = requireLogin(authorization);
        historyService.clearUserHistory(user.getId());
        return ApiResponse.ok(Map.of("cleared", true));
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiChatRequest request
    ) {
        AppUser user = requireLogin(authorization);
        String mode = request.mode() == null || request.mode().isBlank() ? "auto" : request.mode().trim();
        String message = request.message().trim();
        log.info("ai_chat userId={} mode={} messageLen={} stream=false", user.getId(), mode, message.length());
        historyService.appendUserMessage(user.getId(), message);
        AiChatResponse response = aiGatewayService.chat(message, mode, user.getId(), request.history());
        historyService.appendAssistantMessage(user.getId(), response);
        log.info("ai_chat_ok userId={} intent={}", user.getId(), response.intent());
        return ApiResponse.ok(response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> chatStream(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody AiChatRequest request
    ) {
        AppUser user = requireLogin(authorization);
        String mode = request.mode() == null || request.mode().isBlank() ? "auto" : request.mode().trim();
        String message = request.message().trim();
        log.info("ai_chat userId={} mode={} messageLen={} stream=true", user.getId(), mode, message.length());
        historyService.appendUserMessage(user.getId(), message);
        AtomicReference<AiChatResponse> finalRef = new AtomicReference<>();
        StreamingResponseBody body = aiGatewayService.streamChat(
                message,
                mode,
                user.getId(),
                request.history(),
                response -> {
                    finalRef.set(response);
                    historyService.appendAssistantMessage(user.getId(), response);
                    log.info("ai_chat_stream_final userId={} intent={}", user.getId(), response.intent());
                }
        );
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "event-stream", StandardCharsets.UTF_8))
                .header("Cache-Control", "no-cache")
                .header("X-Accel-Buffering", "no")
                .body(body);
    }

    private AppUser requireLogin(String authorization) {
        Long userId = tokenService.findUserId(authorization)
                .orElseThrow(() -> new IllegalArgumentException("请先登录"));
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }
}
