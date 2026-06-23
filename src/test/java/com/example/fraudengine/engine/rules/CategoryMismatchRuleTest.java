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

class CategoryMismatchRuleTest {

    private CategoryMismatchRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new CategoryMismatchRule();
        config = FraudRuleFactory.buildConfig("CATEGORY_MISMATCH_RULE", true, "LOW", null, null);
        customer = Customer.builder().id(1L).customerId("CUST-001").fullName("Test").build();
    }

    private Transaction buildTransaction(String id, String category) {
        return Transaction.builder()
                .transactionId(id)
                .amount(new BigDecimal("100.00"))
                .currency("ZAR")
                .merchantCategory(category)
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_highRiskCategory_returnsFlagged() {
        Transaction current = buildTransaction("TX-001", "GAMBLING");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.reason()).contains("GAMBLING");
    }

    @Test
    void evaluate_cryptoCategory_returnsFlagged() {
        Transaction current = buildTransaction("TX-001", "CRYPTO_EXCHANGE");
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
    }

    @Test
    void evaluate_knownCategoryWithHistory_returnsPassed() {
        List<Transaction> history = List.of(
                buildTransaction("TX-01", "GROCERY"),
                buildTransaction("TX-02", "PETROL"),
                buildTransaction("TX-03", "RESTAURANT")
        );
        Transaction current = buildTransaction("TX-CURRENT", "GROCERY");
        EvaluationContext ctx = new EvaluationContext(current, customer, history, config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_newCategoryWithSufficientHistory_returnsFlagged() {
        List<Transaction> history = List.of(
                buildTransaction("TX-01", "GROCERY"),
                buildTransaction("TX-02", "PETROL"),
                buildTransaction("TX-03", "RESTAURANT")
        );
        Transaction current = buildTransaction("TX-CURRENT", "JEWELLERY");
        EvaluationContext ctx = new EvaluationContext(current, customer, history, config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.reason()).contains("JEWELLERY");
    }

    @Test
    void evaluate_newCategoryWithInsufficientHistory_returnsPassed() {
        // Only 2 categories in history — below threshold of 3
        List<Transaction> history = List.of(
                buildTransaction("TX-01", "GROCERY"),
                buildTransaction("TX-02", "PETROL")
        );
        Transaction current = buildTransaction("TX-CURRENT", "ELECTRONICS");
        EvaluationContext ctx = new EvaluationContext(current, customer, history, config);

        RuleResult result = rule.evaluate(ctx);

        // Should pass because insufficient history
        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_noCategory_returnsSkipped() {
        Transaction current = Transaction.builder()
                .transactionId("TX-001")
                .amount(new BigDecimal("100.00"))
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.SKIPPED);
    }
}
