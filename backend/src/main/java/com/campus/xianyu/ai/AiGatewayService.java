package com.campus.xianyu.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Service
public class AiGatewayService {
    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public AiGatewayService(AiProperties properties) {
        this.properties = properties;
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofMillis(Math.max(1000, properties.getConnectTimeoutMs())))
                .build();
    }

    public AiChatResponse chat(String message, String mode, Long userId, java.util.List<AiChatHistoryItem> history) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("AI 助手未启用");
        }
        try {
            String body = buildPayload(message, mode, userId, history);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getBaseUrl()) + "/v1/chat"))
                    .timeout(Duration.ofMillis(Math.max(5000, properties.getReadTimeoutMs())))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (userId != null) {
                builder.header("X-User-Id", String.valueOf(userId));
            }
            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 429) {
                throw new IllegalArgumentException("AI 调用过于频繁，请稍后再试");
            }
            if (response.statusCode() >= 400) {
                String detail = extractDetail(response.body());
                throw new IllegalArgumentException(detail == null ? "AI 服务暂时不可用" : detail);
            }
            return toResponse(objectMapper.readTree(response.body()));
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法连接 AI 服务，请确认 ai-service 已在 8000 端口启动");
        }
    }

    public StreamingResponseBody streamChat(
            String message,
            String mode,
            Long userId,
            java.util.List<AiChatHistoryItem> history,
            Consumer<AiChatResponse> onFinal
    ) {
        if (!properties.isEnabled()) {
            throw new IllegalArgumentException("AI 助手未启用");
        }
        try {
            String body = buildPayload(message, mode, userId, history);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getBaseUrl()) + "/v1/chat/stream"))
                    .timeout(Duration.ofMillis(Math.max(30000, properties.getReadTimeoutMs() * 2L)))
                    .header("Content-Type", "application/json")
                    .header("Accept", "text/event-stream")
                    .POST(HttpRequest.BodyPublishers.ofString(body));
            if (userId != null) {
                builder.header("X-User-Id", String.valueOf(userId));
            }
            HttpResponse<InputStream> response =
                    httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 429) {
                throw new IllegalArgumentException("AI 调用过于频繁，请稍后再试");
            }
            if (response.statusCode() >= 400) {
                String detail = extractDetail(new String(response.body().readAllBytes(), StandardCharsets.UTF_8));
                throw new IllegalArgumentException(detail == null ? "AI 服务暂时不可用" : detail);
            }
            InputStream upstream = response.body();
            return (OutputStream out) -> {
                // 小缓冲逐字节转发；解析侧必须按完整 UTF-8 字符解码，避免中文被切成 �
                StringBuilder eventName = new StringBuilder();
                StringBuilder dataBuf = new StringBuilder();
                StringBuilder lineBuf = new StringBuilder();
                try (InputStream in = upstream) {
                    byte[] buf = new byte[256];
                    byte[] pending = new byte[0];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        out.flush();
                        byte[] merged = new byte[pending.length + n];
                        System.arraycopy(pending, 0, merged, 0, pending.length);
                        System.arraycopy(buf, 0, merged, pending.length, n);
                        int completeLen = utf8CompleteLength(merged);
                        if (completeLen < 0) {
                            pending = merged;
                            continue;
                        }
                        pending = new byte[merged.length - completeLen];
                        if (pending.length > 0) {
                            System.arraycopy(merged, completeLen, pending, 0, pending.length);
                        }
                        String chunk = new String(merged, 0, completeLen, StandardCharsets.UTF_8);
                        for (int i = 0; i < chunk.length(); i++) {
                            char c = chunk.charAt(i);
                            if (c == '\n') {
                                String line = lineBuf.toString();
                                lineBuf.setLength(0);
                                if (line.endsWith("\r")) {
                                    line = line.substring(0, line.length() - 1);
                                }
                                if (line.startsWith("event:")) {
                                    eventName.setLength(0);
                                    eventName.append(line.substring(6).trim());
                                } else if (line.startsWith("data:")) {
                                    if (dataBuf.length() > 0) {
                                        dataBuf.append('\n');
                                    }
                                    dataBuf.append(line.substring(5).trim());
                                } else if (line.isEmpty()) {
                                    if ("final".contentEquals(eventName) && dataBuf.length() > 0 && onFinal != null) {
                                        try {
                                            AiChatResponse parsed = toResponse(objectMapper.readTree(dataBuf.toString()));
                                            onFinal.accept(parsed);
                                        } catch (Exception ignored) {
                                            // ignore parse failure for persistence
                                        }
                                    }
                                    eventName.setLength(0);
                                    dataBuf.setLength(0);
                                }
                            } else {
                                lineBuf.append(c);
                            }
                        }
                    }
                }
            };
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("无法连接 AI 流式服务，请确认 ai-service 已启动");
        }
    }

    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("enabled", properties.isEnabled());
        result.put("baseUrl", properties.getBaseUrl());
        if (!properties.isEnabled()) {
            result.put("status", "disabled");
            return result;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimSlash(properties.getBaseUrl()) + "/health"))
                    .timeout(Duration.ofMillis(Math.max(1000, properties.getConnectTimeoutMs())))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                result.put("status", "down");
                return result;
            }
            JsonNode node = objectMapper.readTree(response.body());
            result.put("status", node.path("status").asText("unknown"));
            result.put("llmEnabled", node.path("llm_enabled").asBoolean(false));
            result.put("model", node.path("model").isNull() ? null : node.path("model").asText(null));
            result.put("rateLimitBackend", node.path("rate_limit_backend").asText("unknown"));
            result.put("redis", node.path("redis").asText(node.path("context_backend").asText("unknown")));
            if (node.has("rag")) {
                result.put("rag", objectMapper.convertValue(node.path("rag"), Map.class));
            }
            result.put("streaming", node.path("streaming").asBoolean(false));
            return result;
        } catch (Exception ex) {
            result.put("status", "down");
            return result;
        }
    }

    private String buildPayload(String message, String mode, Long userId, java.util.List<AiChatHistoryItem> history)
            throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("message", message);
        payload.put("mode", mode == null || mode.isBlank() ? "auto" : mode);
        if (userId != null) {
            payload.put("user_id", String.valueOf(userId));
        }
        if (history != null && !history.isEmpty()) {
            payload.put("history", history);
        }
        return objectMapper.writeValueAsString(payload);
    }

    private AiChatResponse toResponse(JsonNode node) {
        List<Map<String, Object>> citations = new ArrayList<>();
        JsonNode citationNode = node.path("citations");
        if (citationNode.isArray()) {
            for (JsonNode item : citationNode) {
                citations.add(objectMapper.convertValue(item, Map.class));
            }
        }
        List<String> trace = new ArrayList<>();
        JsonNode traceNode = node.path("agent_trace");
        if (traceNode.isArray()) {
            for (JsonNode item : traceNode) {
                trace.add(item.asText());
            }
        }
        Map<String, Object> listing = node.path("listing").isNull() || node.path("listing").isMissingNode()
                ? null
                : objectMapper.convertValue(node.path("listing"), Map.class);
        Map<String, Object> wanted = node.path("wanted").isNull() || node.path("wanted").isMissingNode()
                ? null
                : objectMapper.convertValue(node.path("wanted"), Map.class);
        Map<String, Object> risk = node.path("risk").isNull() || node.path("risk").isMissingNode()
                ? null
                : objectMapper.convertValue(node.path("risk"), Map.class);
        List<Map<String, Object>> products = new ArrayList<>();
        JsonNode productsNode = node.path("products");
        if (productsNode.isArray()) {
            for (JsonNode item : productsNode) {
                products.add(objectMapper.convertValue(item, Map.class));
            }
        }
        return new AiChatResponse(
                node.path("answer").asText(""),
                node.path("intent").asText("support"),
                citations,
                listing,
                wanted,
                risk,
                products,
                trace,
                node.path("llm_enabled").asBoolean(false),
                node.path("rate_limit_backend").asText(
                        node.path("context_backend").asText("memory")
                )
        );
    }

    private String extractDetail(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.has("detail")) {
                JsonNode detail = node.get("detail");
                if (detail.isTextual()) {
                    return detail.asText();
                }
                if (detail.isArray() && !detail.isEmpty()) {
                    JsonNode first = detail.get(0);
                    if (first.has("msg")) {
                        return first.get("msg").asText();
                    }
                    return first.toString();
                }
                return detail.toString();
            }
            if (node.has("message")) {
                return node.get("message").asText();
            }
        } catch (Exception ignored) {
            // ignore
        }
        return null;
    }

    private String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "http://127.0.0.1:8000";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    /** 返回可完整解码为 UTF-8 的前缀长度；末尾不完整多字节序列留给下次拼接。 */
    private static int utf8CompleteLength(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return 0;
        }
        int i = bytes.length;
        int trail = 0;
        while (i > 0 && (bytes[i - 1] & 0xC0) == 0x80) {
            trail++;
            i--;
            if (trail > 3) {
                return bytes.length;
            }
        }
        if (i == 0) {
            return bytes.length;
        }
        int b = bytes[i - 1] & 0xFF;
        int need;
        if (b < 0x80) {
            need = 1;
        } else if (b >= 0xC2 && b <= 0xDF) {
            need = 2;
        } else if (b >= 0xE0 && b <= 0xEF) {
            need = 3;
        } else if (b >= 0xF0 && b <= 0xF4) {
            need = 4;
        } else {
            return bytes.length;
        }
        int have = 1 + trail;
        if (have < need) {
            return i - 1;
        }
        return bytes.length;
    }
}
