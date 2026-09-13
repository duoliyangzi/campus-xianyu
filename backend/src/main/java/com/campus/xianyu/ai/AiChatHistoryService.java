package com.campus.xianyu.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatHistoryService {
    private final AiChatSessionRepository sessionRepository;
    private final AiChatMessageRepository messageRepository;
    private final ObjectMapper objectMapper;

    public AiChatHistoryService(
            AiChatSessionRepository sessionRepository,
            AiChatMessageRepository messageRepository
    ) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
    public AiChatSession getOrCreateSession(Long userId) {
        return sessionRepository.findFirstByUserIdOrderByIdAsc(userId).orElseGet(() -> {
            AiChatSession session = new AiChatSession();
            session.setUserId(userId);
            session.setTitle("默认会话");
            return sessionRepository.save(session);
        });
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRecentMessages(Long userId, int limit) {
        return sessionRepository.findFirstByUserIdOrderByIdAsc(userId)
                .map(session -> {
                    int size = Math.max(1, Math.min(limit, 200));
                    List<AiChatMessage> rows =
                            messageRepository.findTop80BySessionIdOrderByCreatedAtDescIdDesc(session.getId());
                    if (rows.size() > size) {
                        rows = rows.subList(0, size);
                    }
                    Collections.reverse(rows);
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (AiChatMessage row : rows) {
                        result.add(toView(row));
                    }
                    return result;
                })
                .orElse(List.of());
    }

    @Transactional
    public void appendUserMessage(Long userId, String content) {
        AiChatSession session = getOrCreateSession(userId);
        AiChatMessage msg = new AiChatMessage();
        msg.setSessionId(session.getId());
        msg.setRole("user");
        msg.setContent(content == null ? "" : content);
        messageRepository.save(msg);
        sessionRepository.save(session);
    }

    @Transactional
    public void appendAssistantMessage(Long userId, AiChatResponse response) {
        if (response == null) {
            return;
        }
        AiChatSession session = getOrCreateSession(userId);
        AiChatMessage msg = new AiChatMessage();
        msg.setSessionId(session.getId());
        msg.setRole("assistant");
        msg.setContent(response.answer() == null ? "" : response.answer());
        msg.setIntent(response.intent());
        msg.setPayloadJson(buildPayload(response));
        messageRepository.save(msg);
        sessionRepository.save(session);
    }

    @Transactional
    public void clearUserHistory(Long userId) {
        List<AiChatSession> sessions = sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        for (AiChatSession session : sessions) {
            messageRepository.deleteBySessionId(session.getId());
            sessionRepository.delete(session);
        }
    }

    private String buildPayload(AiChatResponse response) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("citations", response.citations());
            payload.put("listing", response.listing());
            payload.put("wanted", response.wanted());
            payload.put("risk", response.risk());
            payload.put("products", response.products());
            payload.put("agentTrace", response.agentTrace());
            payload.put("llmEnabled", response.llmEnabled());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toView(AiChatMessage row) {
        Map<String, Object> view = new HashMap<>();
        view.put("id", "db-" + row.getId());
        view.put("role", row.getRole());
        view.put("content", row.getContent());
        view.put("intent", row.getIntent());
        if (row.getPayloadJson() != null && !row.getPayloadJson().isBlank()) {
            try {
                Map<String, Object> payload = objectMapper.readValue(row.getPayloadJson(), Map.class);
                view.putAll(payload);
            } catch (Exception ignored) {
                // ignore malformed payload
            }
        }
        return view;
    }
}
