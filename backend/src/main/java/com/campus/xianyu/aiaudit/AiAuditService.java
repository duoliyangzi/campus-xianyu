package com.campus.xianyu.aiaudit;

import com.campus.xianyu.ai.AiChatResponse;
import com.campus.xianyu.ai.AiGatewayService;
import com.campus.xianyu.product.Product;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AiAuditService {
    private static final List<String> HIGH_RISK_KEYWORDS = List.of("诈骗", "假货", "违禁", "枪支", "毒品", "色情");
    private static final List<String> MEDIUM_RISK_KEYWORDS = List.of("虚假", "高仿", "刷单", "加微信", "转账");

    private final AiGatewayService aiGatewayService;

    public AiAuditService(AiGatewayService aiGatewayService) {
        this.aiGatewayService = aiGatewayService;
    }

    public AiAuditLog audit(Product product) {
        String title = product.getTitle() == null ? "" : product.getTitle().trim();
        String description = product.getDescription() == null ? "" : product.getDescription().trim();

        try {
            String prompt = "请对以下待审校园二手商品做风控预审，输出风险结论。\n"
                    + "标题：" + title + "\n"
                    + "描述：" + description;
            AiChatResponse response = aiGatewayService.chat(prompt, "risk", null, List.of());
            Map<String, Object> risk = response.risk();
            if (risk != null && !risk.isEmpty()) {
                AiAuditLog log = new AiAuditLog();
                log.setProductId(product.getId());
                log.setTitle(title);
                log.setContentSnap(description);
                log.setRiskLevel(String.valueOf(risk.getOrDefault("risk_level", "NONE")));
                log.setSuggestion(String.valueOf(risk.getOrDefault("suggestion", "REVIEW")));
                log.setReason(String.valueOf(risk.getOrDefault("reason", response.answer())));
                log.setRawResponse("ai-risk-agent:" + String.join(">", response.agentTrace() == null ? List.of() : response.agentTrace()));
                return log;
            }
        } catch (Exception ignored) {
            // AI 不可用时回退规则引擎，保证审核按钮可用
        }
        return ruleBasedAudit(product, title, description);
    }

    private AiAuditLog ruleBasedAudit(Product product, String title, String description) {
        String titleLower = title.toLowerCase();
        String descriptionLower = description.toLowerCase();
        String riskLevel = "NONE";
        String suggestion = "PASS";
        String reason = "标题与描述未发现明显风险（规则兜底）";

        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (titleLower.contains(keyword.toLowerCase()) || descriptionLower.contains(keyword.toLowerCase())) {
                riskLevel = "HIGH";
                suggestion = "REJECT";
                reason = "命中高风险词（规则兜底）：" + keyword;
                break;
            }
        }
        if ("NONE".equals(riskLevel)) {
            for (String keyword : MEDIUM_RISK_KEYWORDS) {
                if (titleLower.contains(keyword.toLowerCase()) || descriptionLower.contains(keyword.toLowerCase())) {
                    riskLevel = "MEDIUM";
                    suggestion = "REVIEW";
                    reason = "命中需复核词（规则兜底）：" + keyword;
                    break;
                }
            }
        }
        if ("NONE".equals(riskLevel) && title.length() < 2) {
            riskLevel = "LOW";
            suggestion = "REVIEW";
            reason = "商品标题过短，建议人工复核（规则兜底）";
        } else if ("NONE".equals(riskLevel) && description.length() < 10) {
            riskLevel = "LOW";
            suggestion = "REVIEW";
            reason = "商品描述过短，建议人工复核（规则兜底）";
        }

        AiAuditLog log = new AiAuditLog();
        log.setProductId(product.getId());
        log.setTitle(title);
        log.setContentSnap(description);
        log.setRiskLevel(riskLevel);
        log.setSuggestion(suggestion);
        log.setReason(reason);
        log.setRawResponse("rule-fallback-audit");
        return log;
    }
}
