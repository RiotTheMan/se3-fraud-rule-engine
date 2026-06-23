package com.example.fraudengine.engine.rules;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleFactory;
import com.example.fraudengine.engine.RuleResult;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateTransactionRuleTest {

    private DuplicateTransactionRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new DuplicateTransactionRule();
        config = FraudRuleFactory.buildConfig("DUPLICATE_TRANSACTION_RULE", true, "HIGH", null, 5);
        customer = Customer.builder().id(1L).customerId("CUST-001").fullName("Test").build();
    }

    private Transaction buildTransaction(String id, BigDecimal amount, String merchant) {
        return Transaction.builder()
                .transactionId(id)
                .amount(amount)
                .currency("ZAR")
                .merchantName(merchant)
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_duplicateMerchantAndAmount_returnsFlagged() {
        Transaction prior = buildTransaction("TX-001", new BigDecimal("250.00"), "Checkers");
        Transaction current = buildTransaction("TX-002", new BigDecimal("250.00"), "Checkers");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.reason()).contains("Checkers");
        assertThat(result.severity()).isEqualTo("HIGH");
    }

    @Test
    void evaluate_sameMerchantDifferentAmount_returnsPassed() {
        Transaction prior = buildTransaction("TX-001", new BigDecimal("250.00"), "Checkers");
        Transaction current = buildTransaction("TX-002", new BigDecimal("300.00"), "Checkers");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_differentMerchantSameAmount_returnsPassed() {
        Transaction prior = buildTransaction("TX-001", new BigDecimal("250.00"), "Shoprite");
        Transaction current = buildTransaction("TX-002", new BigDecimal("250.00"), "Checkers");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_noMerchantName_returnsSkipped() {
        Transaction current = Transaction.builder()
                .transactionId("TX-001")
                .amount(new BigDecimal("100.00"))
                .currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.SKIPPED);
    }

    @Test
    void evaluate_emptyHistory_returnsPassed() {
        Transaction current = buildTransaction("TX-001", new BigDecimal("100.00"), "PnP");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }
}
