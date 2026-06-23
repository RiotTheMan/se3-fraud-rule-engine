package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Unusual Hour Rule — flags transactions that occur between 00:00 and 05:59 SAST (UTC+2).
 *
 * <p>Night-time transactions (midnight to 6am local time) are statistically more likely to
 * be fraudulent. The rule evaluates the transaction's local timestamp in the
 * Africa/Johannesburg timezone regardless of where the transaction originated.</p>
 *
 */
@Component
public class UnusualHourRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "UNUSUAL_HOUR_RULE";
    private static final ZoneId SAST = ZoneId.of("Africa/Johannesburg");
    private static final int SUSPICIOUS_HOUR_START = 0;
    private static final int SUSPICIOUS_HOUR_END = 6;

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        OffsetDateTime txTime = context.transaction().getTransactionAt();
        ZonedDateTime txTimeSast = txTime.atZoneSameInstant(SAST);
        int hour = txTimeSast.getHour();

        if (hour >= SUSPICIOUS_HOUR_START && hour < SUSPICIOUS_HOUR_END) {
            String reason = String.format(
                    "Transaction occurred at %02d:%02d SAST (suspicious window: %02d:00–%02d:00)",
                    hour,
                    txTimeSast.getMinute(),
                    SUSPICIOUS_HOUR_START,
                    SUSPICIOUS_HOUR_END
            );
            return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
        }

        return RuleResult.passed(RULE_NAME);
    }
}
