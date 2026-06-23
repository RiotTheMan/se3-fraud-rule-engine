package com.example.fraudengine.engine;

/**
 * Strategy interface for fraud detection rules.
 *
 * <p>Each implementation encapsulates a single fraud detection algorithm. Implementations
 * are Spring beans discovered automatically by the
 * {@link RuleRegistry}, which uses the bean class names to match them against their
 * database-backed {@link com.example.fraudengine.domain.FraudRule} configuration.</p>
 *
 * <p>Rules MUST be stateless — all state is passed via {@link EvaluationContext}. This
 * allows safe concurrent evaluation on virtual threads.</p>
 */
public interface FraudRuleStrategy {

    /**
     * Returns the canonical name for this rule — must match the {@code rule_name} column
     * in the {@code fraud_rules} table.
     */
    String getRuleName();

    /**
     * Evaluates the rule against the given context.
     *
     * @param context evaluation context containing the transaction and customer history
     * @return the result of the evaluation; never null
     */
    RuleResult evaluate(EvaluationContext context);
}
