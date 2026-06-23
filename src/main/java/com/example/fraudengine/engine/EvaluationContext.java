package com.example.fraudengine.engine;

import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.domain.Transaction;

import java.util.List;

/**
 * Immutable snapshot of all data a rule needs to evaluate one transaction.
 *
 * <p>Passing context as a record rather than individual parameters keeps the
 * {@link FraudRuleStrategy#evaluate} signature stable as new signals are added.</p>
 *
 * @param transaction         the transaction being evaluated
 * @param customer            the owner of the transaction
 * @param recentTransactions  transactions by this customer in the rule's lookback window
 * @param ruleConfig          the database-backed configuration for this specific rule
 */
public record EvaluationContext(
        Transaction transaction,
        Customer customer,
        List<Transaction> recentTransactions,
        FraudRule ruleConfig
) {
}
