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

class LargeAmountRuleTest {

    private LargeAmountRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new LargeAmountRule();
        config = FraudRuleFactory.buildConfig("LARGE_AMOUNT_RULE", true, "HIGH",
                new BigDecimal("50000.00"), null);
        customer = Customer.builder()
                .id(1L)
                .customerId("CUST-001")
                .fullName("Test Customer")
                .build();
    }

    private Transaction buildTransaction(BigDecimal amount) {
        return Transaction.builder()
                .transactionId("TX-001")
                .amount(amount)
                .currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_amountBelowThreshold_returnsPassed() {
        Transaction tx = buildTransaction(new BigDecimal("25000.00"));
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_amountAtThreshold_returnsPassed() {
        Transaction tx = buildTransaction(new BigDecimal("50000.00"));
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
    }

    @Test
    void evaluate_amountExceedsThreshold_returnsFlagged() {
        Transaction tx = buildTransaction(new BigDecimal("75000.00"));
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.FLAGGED);
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.reason()).contains("75000");
        assertThat(result.severity()).isEqualTo("HIGH");
    }

    @Test
    void evaluate_usesDefaultThresholdWhenConfigNull() {
        FraudRule configNoThreshold = FraudRuleFactory.buildConfig(
                "LARGE_AMOUNT_RULE", true, "HIGH", null, null);
        // Default threshold is 50000 — 49999 should pass
        Transaction tx = buildTransaction(new BigDecimal("49999.99"));
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), configNoThreshold);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
    }

    @Test
    void getRuleName_returnsExpectedConstant() {
        assertThat(rule.getRuleName()).isEqualTo("LARGE_AMOUNT_RULE");
    }
}
