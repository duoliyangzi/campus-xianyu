package com.campus.xianyu.ai;

import java.util.List;
import java.util.Map;

public record AiChatResponse(
        String answer,
        String intent,
        List<Map<String, Object>> citations,
        Map<String, Object> listing,
        Map<String, Object> wanted,
        Map<String, Object> risk,
        List<Map<String, Object>> products,
        List<String> agentTrace,
        Boolean llmEnabled,
        String rateLimitBackend
) {
}
