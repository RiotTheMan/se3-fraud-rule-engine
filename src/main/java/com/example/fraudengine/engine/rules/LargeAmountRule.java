package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.math.BigDecimal;

/**
 * Large Amount Rule — flags a transaction whose amount exceeds the configured threshold.
 *
 * <p>The {@code threshold} in the database is the maximum single-transaction amount in ZAR
 * (or the transaction's native currency — currency conversion is out of scope for this
 * service and handled upstream).</p>
 *
 */
@Component
public class LargeAmountRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "LARGE_AMOUNT_RULE";
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("50000.00");

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        BigDecimal threshold = context.ruleConfig().getThreshold() != null
                ? context.ruleConfig().getThreshold()
                : DEFAULT_THRESHOLD;

        if (context.transaction().getAmount().compareTo(threshold) > 0) {
            String reason = String.format(
                    "Transaction amount %.2f %s exceeds threshold %.2f",
                    context.transaction().getAmount(),
                    context.transaction().getCurrency(),
                    threshold
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        return RuleResult.passed(RULE_NAME);
    }
}
