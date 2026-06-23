package com.example.fraudengine.engine;

/**
 * Result of evaluating a single fraud rule against a transaction.
 *
 * @param ruleName  canonical rule name (matches {@code fraud_rules.rule_name})
 * @param outcome   {@code FLAGGED} if the rule detected fraud, {@code PASSED} if clean,
 *                  {@code SKIPPED} if the rule was disabled or not applicable
 * @param reason    human-readable explanation; populated for FLAGGED and SKIPPED outcomes
 * @param severity  severity level from the rule configuration; null when PASSED
 */
public record RuleResult(
        String ruleName,
        Outcome outcome,
        String reason,
        String severity
) {

    public enum Outcome {
        FLAGGED,
        PASSED,
        SKIPPED
    }

    public static RuleResult flagged(String ruleName, String reason, String severity) {
        return new RuleResult(ruleName, Outcome.FLAGGED, reason, severity);
    }

    public static RuleResult passed(String ruleName) {
        return new RuleResult(ruleName, Outcome.PASSED, null, null);
    }

    public static RuleResult skipped(String ruleName, String reason) {
        return new RuleResult(ruleName, Outcome.SKIPPED, reason, null);
    }

    public boolean isFlagged() {
        return outcome == Outcome.FLAGGED;
    }
}
