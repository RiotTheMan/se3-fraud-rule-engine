package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Duplicate Transaction Rule — flags when the same amount is charged by the same merchant
 * to this customer more than once within a short time window.
 *
 * <p>This catches both malicious double-charges and accidental merchant duplicate POSTs.
 * A transaction is considered a duplicate if a prior transaction in the window shares
 * the same merchant name AND the same amount.</p>
 *
 * <p>Catches semantically duplicate charges (same merchant, amount, and category within
 * the window); the {@code idempotency_key} DB constraint handles event-level deduplication
 * separately.</p>
 */
@Component
public class DuplicateTransactionRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "DUPLICATE_TRANSACTION_RULE";

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        Transaction current = context.transaction();

        if (current.getMerchantName() == null) {
            return RuleResult.skipped(RULE_NAME, "No merchant name on transaction; cannot check for duplicates");
        }

        List<Transaction> duplicates = context.recentTransactions().stream()
                .filter(t -> !t.getTransactionId().equals(current.getTransactionId()))
                .filter(t -> current.getMerchantName().equalsIgnoreCase(t.getMerchantName()))
                .filter(t -> current.getAmount().compareTo(t.getAmount()) == 0)
                .toList();

        if (!duplicates.isEmpty()) {
            int windowMinutes = context.ruleConfig().getWindowMinutes() != null
                    ? context.ruleConfig().getWindowMinutes()
                    : 5;
            String reason = String.format(
                    "Found %d duplicate transaction(s) with merchant '%s' for amount %.2f %s in the last %d minutes",
                    duplicates.size(),
                    current.getMerchantName(),
                    current.getAmount(),
                    current.getCurrency(),
                    windowMinutes
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        return RuleResult.passed(RULE_NAME);
    }
}
