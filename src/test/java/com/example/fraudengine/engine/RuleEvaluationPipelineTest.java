package com.example.fraudengine.engine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.rules.LargeAmountRule;
import com.example.fraudengine.engine.rules.VelocityRule;
import com.example.fraudengine.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleEvaluationPipelineTest {

    @Mock
    private RuleRegistry ruleRegistry;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private RuleEvaluationPipeline pipeline;

    private Customer customer;
    private Transaction transaction;

    @BeforeEach
    void setUp() {
        customer = Customer.builder()
                .id(1L)
                .customerId("CUST-001")
                .fullName("Test Customer")
                .build();

        transaction = Transaction.builder()
                .id(1L)
                .transactionId("TX-001")
                .amount(new BigDecimal("100.00"))
                .currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_noActiveRules_returnsEmptyList() {
        when(ruleRegistry.getActiveRules()).thenReturn(List.of());

        List<RuleResult> results = pipeline.evaluate(transaction, customer);

        assertThat(results).isEmpty();
    }

    @Test
    void evaluate_allRulesPass_returnsAllPassed() {
        VelocityRule velocityRule = new VelocityRule();
        FraudRule velocityConfig = new FraudRule();
        velocityConfig.setRuleName("VELOCITY_RULE");
        velocityConfig.setEnabled(true);
        velocityConfig.setSeverity("HIGH");
        velocityConfig.setThreshold(new BigDecimal("100"));
        velocityConfig.setWindowMinutes(60);

        when(ruleRegistry.getActiveRules()).thenReturn(List.of(velocityRule));
        when(ruleRegistry.getConfig("VELOCITY_RULE")).thenReturn(velocityConfig);
        when(transactionRepository.findByCustomerAndTransactionAtBetween(
                any(), any(), any())).thenReturn(List.of());

        List<RuleResult> results = pipeline.evaluate(transaction, customer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).outcome()).isEqualTo(RuleResult.Outcome.PASSED);
    }

    @Test
    void evaluate_ruleFlagged_returnsFlaggedResult() {
        LargeAmountRule largeAmountRule = new LargeAmountRule();
        FraudRule config = new FraudRule();
        config.setRuleName("LARGE_AMOUNT_RULE");
        config.setEnabled(true);
        config.setSeverity("HIGH");
        config.setThreshold(new BigDecimal("50.00")); // threshold below transaction amount

        when(ruleRegistry.getActiveRules()).thenReturn(List.of(largeAmountRule));
        when(ruleRegistry.getConfig("LARGE_AMOUNT_RULE")).thenReturn(config);
        when(transactionRepository.findByCustomerAndTransactionAtBetween(
                any(), any(), any())).thenReturn(List.of());

        List<RuleResult> results = pipeline.evaluate(transaction, customer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).isFlagged()).isTrue();
    }

    @Test
    void evaluate_multipleRules_allEvaluated_fullEvaluationNoShortCircuit() {
        // Both rules active — first one flags — second should STILL run (no short-circuit)
        LargeAmountRule largeAmountRule = new LargeAmountRule();
        VelocityRule velocityRule = new VelocityRule();

        FraudRule largeConfig = new FraudRule();
        largeConfig.setRuleName("LARGE_AMOUNT_RULE");
        largeConfig.setEnabled(true);
        largeConfig.setSeverity("HIGH");
        largeConfig.setThreshold(new BigDecimal("50.00")); // will flag

        FraudRule velocityConfig = new FraudRule();
        velocityConfig.setRuleName("VELOCITY_RULE");
        velocityConfig.setEnabled(true);
        velocityConfig.setSeverity("HIGH");
        velocityConfig.setThreshold(new BigDecimal("100")); // won't flag
        velocityConfig.setWindowMinutes(60);

        when(ruleRegistry.getActiveRules()).thenReturn(List.of(largeAmountRule, velocityRule));
        when(ruleRegistry.getConfig("LARGE_AMOUNT_RULE")).thenReturn(largeConfig);
        when(ruleRegistry.getConfig("VELOCITY_RULE")).thenReturn(velocityConfig);
        when(transactionRepository.findByCustomerAndTransactionAtBetween(
                any(), any(), any())).thenReturn(List.of());

        List<RuleResult> results = pipeline.evaluate(transaction, customer);

        // Full evaluation — both rules should have produced a result
        assertThat(results).hasSize(2);
        long flaggedCount = results.stream().filter(RuleResult::isFlagged).count();
        assertThat(flaggedCount).isEqualTo(1);
    }

    @Test
    void evaluate_ruleThrowsException_resultIsSkippedWithErrorReason() {
        FraudRuleStrategy throwingRule = new FraudRuleStrategy() {
            @Override
            public String getRuleName() { return "THROWING_RULE"; }

            @Override
            public RuleResult evaluate(EvaluationContext context) {
                throw new RuntimeException("Simulated rule failure");
            }
        };

        FraudRule config = new FraudRule();
        config.setRuleName("THROWING_RULE");
        config.setEnabled(true);
        config.setSeverity("HIGH");
        config.setThreshold(new BigDecimal("100"));
        config.setWindowMinutes(60);

        when(ruleRegistry.getActiveRules()).thenReturn(List.of(throwingRule));
        when(ruleRegistry.getConfig("THROWING_RULE")).thenReturn(config);
        when(transactionRepository.findByCustomerAndTransactionAtBetween(
                any(), any(), any())).thenReturn(List.of());

        List<RuleResult> results = pipeline.evaluate(transaction, customer);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).outcome()).isEqualTo(RuleResult.Outcome.SKIPPED);
        assertThat(results.get(0).reason()).contains("Rule evaluation failed");
    }
}
