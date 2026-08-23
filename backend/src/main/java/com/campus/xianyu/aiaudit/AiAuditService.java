package com.campus.xianyu.aiaudit;

import com.campus.xianyu.product.Product;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiAuditService {
    private static final List<String> HIGH_RISK_KEYWORDS = List.of("诈骗", "假货", "违禁", "枪支", "毒品", "色情");
    private static final List<String> MEDIUM_RISK_KEYWORDS = List.of("虚假", "高仿", "刷单", "加微信", "转账");

    public AiAuditLog audit(Product product) {
        String title = product.getTitle() == null ? "" : product.getTitle().trim();
        String description = product.getDescription() == null ? "" : product.getDescription().trim();
        String titleLower = title.toLowerCase();
        String descriptionLower = description.toLowerCase();
        String riskLevel = "NONE";
        String suggestion = "PASS";
        String reason = "标题与描述未发现明显风险关键词";

        for (String keyword : HIGH_RISK_KEYWORDS) {
            if (titleLower.contains(keyword.toLowerCase())) {
                riskLevel = "HIGH";
                suggestion = "REJECT";
                reason = "标题包含高风险关键词：" + keyword;
                break;
            }
            if (descriptionLower.contains(keyword.toLowerCase())) {
                riskLevel = "HIGH";
                suggestion = "REJECT";
                reason = "描述包含高风险关键词：" + keyword;
                break;
            }
        }
        if ("NONE".equals(riskLevel)) {
            for (String keyword : MEDIUM_RISK_KEYWORDS) {
                if (titleLower.contains(keyword.toLowerCase())) {
                    riskLevel = "MEDIUM";
                    suggestion = "REVIEW";
                    reason = "标题包含需人工复核关键词：" + keyword;
                    break;
                }
                if (descriptionLower.contains(keyword.toLowerCase())) {
                    riskLevel = "MEDIUM";
                    suggestion = "REVIEW";
                    reason = "描述包含需人工复核关键词：" + keyword;
                    break;
                }
            }
        }
        if ("NONE".equals(riskLevel) && title.length() < 2) {
            riskLevel = "LOW";
            suggestion = "REVIEW";
            reason = "商品标题过短，建议人工复核";
        } else if ("NONE".equals(riskLevel) && description.length() < 10) {
            riskLevel = "LOW";
            suggestion = "REVIEW";
            reason = "商品描述过短，建议人工复核";
        }

        AiAuditLog log = new AiAuditLog();
        log.setProductId(product.getId());
        log.setTitle(title);
        log.setContentSnap(description);
        log.setRiskLevel(riskLevel);
        log.setSuggestion(suggestion);
        log.setReason(reason);
        log.setRawResponse("keyword-rule-audit");
        return log;
    }
}
