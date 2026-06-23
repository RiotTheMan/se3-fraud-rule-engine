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
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VelocityRuleTest {

    private VelocityRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new VelocityRule();
        config = FraudRuleFactory.buildConfig("VELOCITY_RULE", true, "HIGH",
                new BigDecimal("5"), 60);
        customer = Customer.builder()
                .id(1L)
                .customerId("CUST-001")
                .fullName("Test Customer")
                .build();
    }

    private Transaction buildTransaction(String id) {
        return Transaction.builder()
                .transactionId(id)
                .amount(new BigDecimal("100.00"))
                .currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_belowThreshold_returnsPassed() {
        // 4 prior transactions + current = 5 (at threshold, not over)
        List<Transaction> recent = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            recent.add(buildTransaction("TX-" + i));
        }
        Transaction current = buildTransaction("TX-CURRENT");
        EvaluationContext ctx = new EvaluationContext(current, customer, recent, config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_atThreshold_returnsPassed() {
        // Exactly at threshold (5 prior + 1 current = 6 but threshold is 5 so this should be over)
        // Actually: 4 prior + 1 current = 5 = threshold, not over → PASSED
        List<Transaction> recent = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            recent.add(buildTransaction("TX-" + i));
        }
        Transaction current = buildTransaction("TX-CURRENT");
        EvaluationContext ctx = new EvaluationContext(current, customer, recent, config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
    }

    @Test
    void evaluate_exceedsThreshold_returnsFlagged() {
        // 5 prior + 1 current = 6 > threshold of 5
        List<Transaction> recent = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            recent.add(buildTransaction("TX-" + i));
        }
        Transaction current = buildTransaction("TX-CURRENT");
        EvaluationContext ctx = new EvaluationContext(current, customer, recent, config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.FLAGGED);
        assertThat(result.isFlagged()).isTrue();
        assertThat(result.reason()).contains("6");
        assertThat(result.severity()).isEqualTo("HIGH");
    }

    @Test
    void evaluate_emptyHistory_returnsPassed() {
        Transaction current = buildTransaction("TX-CURRENT");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.PASSED);
    }

    @Test
    void getRuleName_returnsExpectedConstant() {
        assertThat(rule.getRuleName()).isEqualTo("VELOCITY_RULE");
    }
}
