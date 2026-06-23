package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Category Mismatch Rule — flags transactions in high-risk merchant categories or
 * categories that are atypical for this customer's spend profile.
 *
 */
@Component
public class CategoryMismatchRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "CATEGORY_MISMATCH_RULE";

    /**
     * Categories that are always considered high-risk regardless of customer history.
     */
    private static final Set<String> HIGH_RISK_CATEGORIES = Set.of(
            "GAMBLING", "CRYPTO_EXCHANGE", "ADULT_CONTENT",
            "MONEY_TRANSFER", "WIRE_TRANSFER", "CRYPTOCURRENCY"
    );

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        String currentCategory = context.transaction().getMerchantCategory();

        if (currentCategory == null || currentCategory.isBlank()) {
            return RuleResult.skipped(RULE_NAME, "Transaction has no merchant category");
        }

        String upperCategory = currentCategory.toUpperCase();

        // Tier 1: High-risk category check
        if (HIGH_RISK_CATEGORIES.contains(upperCategory)) {
            String reason = String.format(
                    "Transaction in high-risk merchant category '%s'", currentCategory
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        // Tier 2: Profile mismatch — only if customer has a meaningful spend history
        List<String> historicalCategories = context.recentTransactions().stream()
                .map(t -> t.getMerchantCategory())
                .filter(c -> c != null && !c.isBlank())
                .map(String::toUpperCase)
                .distinct()
                .toList();

        if (historicalCategories.size() < 3) {
            // Insufficient history for profile mismatch — skip this tier
            return RuleResult.passed(RULE_NAME);
        }

        if (!historicalCategories.contains(upperCategory)) {
            String reason = String.format(
                    "Transaction in category '%s' is not in customer's spend profile (known categories: %s)",
                    currentCategory,
                    String.join(", ", historicalCategories)
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        return RuleResult.passed(RULE_NAME);
    }
}
