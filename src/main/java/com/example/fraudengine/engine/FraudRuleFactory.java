package com.example.fraudengine.engine;

import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.engine.rules.*;

import java.math.BigDecimal;

/**
 * Factory for programmatic creation of rule instances.
 *
 * <p>Used primarily in unit tests to construct rules with specific configurations without
 * requiring the full Spring context. In production, rules are Spring beans managed by the
 * container.</p>
 */
public class FraudRuleFactory {

    private FraudRuleFactory() {}

    public static FraudRule buildConfig(String ruleName, boolean enabled, String severity,
                                        BigDecimal threshold, Integer windowMinutes) {
        FraudRule rule = new FraudRule();
        rule.setRuleName(ruleName);
        rule.setEnabled(enabled);
        rule.setSeverity(severity);
        rule.setThreshold(threshold);
        rule.setWindowMinutes(windowMinutes);
        return rule;
    }

    public static FraudRuleStrategy velocityRule() {
        return new VelocityRule();
    }

    public static FraudRuleStrategy largeAmountRule() {
        return new LargeAmountRule();
    }

    public static FraudRuleStrategy unusualHourRule() {
        return new UnusualHourRule();
    }

    public static FraudRuleStrategy duplicateTransactionRule() {
        return new DuplicateTransactionRule();
    }

    public static FraudRuleStrategy geographicAnomalyRule() {
        return new GeographicAnomalyRule();
    }

    public static FraudRuleStrategy categoryMismatchRule() {
        return new CategoryMismatchRule();
    }
}
