package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.math.BigDecimal;

/**
 * Velocity Rule — flags when the transaction count in a rolling window exceeds the
 * configured threshold.
 *
 * <p>{@code threshold} = max transaction count; {@code window_minutes} = rolling window size.</p>
 */
@Component
public class VelocityRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "VELOCITY_RULE";
    private static final int DEFAULT_MAX_TX = 10;

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        int maxTransactions = context.ruleConfig().getThreshold() != null
                ? context.ruleConfig().getThreshold().intValue()
                : DEFAULT_MAX_TX;

        // recentTransactions includes all transactions in the window (excluding the current one
        // which is not yet persisted). Adding 1 for the current transaction.
        int windowCount = context.recentTransactions().size() + 1;

        if (windowCount > maxTransactions) {
            String reason = String.format(
                    "Customer %s made %d transactions in the last %d minutes (limit: %d)",
                    context.customer().getCustomerId(),
                    windowCount,
                    context.ruleConfig().getWindowMinutes() != null ? context.ruleConfig().getWindowMinutes() : 60,
                    maxTransactions
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        return RuleResult.passed(RULE_NAME);
    }
}
