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
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UnusualHourRuleTest {

    private UnusualHourRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new UnusualHourRule();
        config = FraudRuleFactory.buildConfig("UNUSUAL_HOUR_RULE", true, "MEDIUM", null, null);
        customer = Customer.builder()
                .id(1L)
                .customerId("CUST-001")
                .fullName("Test Customer")
                .build();
    }

    private Transaction buildTransactionAtHourSAST(int hourSAST) {
        // SAST = UTC+2, so subtract 2 hours to get UTC equivalent
        OffsetDateTime txTime = OffsetDateTime.of(2024, 1, 15, hourSAST, 30, 0, 0, ZoneOffset.ofHours(2));
        return Transaction.builder()
                .transactionId("TX-001")
                .amount(new BigDecimal("500.00"))
                .currency("ZAR")
                .transactionAt(txTime)
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_midnightSAST_returnsFlagged() {
        Transaction tx = buildTransactionAtHourSAST(0);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.FLAGGED);
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.severity()).isEqualTo("MEDIUM");
    }

    @Test
    void evaluate_3amSAST_returnsFlagged() {
        Transaction tx = buildTransactionAtHourSAST(3);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
    }

    @Test
    void evaluate_5amSAST_returnsFlagged() {
        Transaction tx = buildTransactionAtHourSAST(5);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
    }

    @Test
    void evaluate_6amSAST_returnsPassed() {
        Transaction tx = buildTransactionAtHourSAST(6);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_middaySAST_returnsPassed() {
        Transaction tx = buildTransactionAtHourSAST(12);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_11pmSAST_returnsPassed() {
        Transaction tx = buildTransactionAtHourSAST(23);
        EvaluationContext ctx = new EvaluationContext(tx, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void getRuleName_returnsExpectedConstant() {
        assertThat(rule.getRuleName()).isEqualTo("UNUSUAL_HOUR_RULE");
    }
}
