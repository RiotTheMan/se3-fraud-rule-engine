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
import static org.assertj.core.api.Assertions.within;

class GeographicAnomalyRuleTest {

    private GeographicAnomalyRule rule;
    private FraudRule config;
    private Customer customer;

    @BeforeEach
    void setUp() {
        rule = new GeographicAnomalyRule();
        // max speed 500 km/h, 2 hour window
        config = FraudRuleFactory.buildConfig("GEOGRAPHIC_ANOMALY_RULE", true, "CRITICAL",
                new BigDecimal("500"), 120);
        customer = Customer.builder().id(1L).customerId("CUST-001").fullName("Test").build();
    }

    private Transaction buildTransaction(String id, double lat, double lon, OffsetDateTime at) {
        return Transaction.builder()
                .transactionId(id)
                .amount(new BigDecimal("100.00"))
                .currency("ZAR")
                .latitude(lat)
                .longitude(lon)
                .transactionAt(at)
                .customer(customer)
                .build();
    }

    @Test
    void evaluate_impossibleTravel_returnsFlagged() {
        // Cape Town ≈ (-33.9, 18.4), London ≈ (51.5, -0.1) — ~9700 km
        // 10 minutes apart — impossible at any speed
        OffsetDateTime t1 = OffsetDateTime.now().minusMinutes(10);
        OffsetDateTime t2 = OffsetDateTime.now();

        Transaction prior = buildTransaction("TX-001", -33.9, 18.4, t1);
        Transaction current = buildTransaction("TX-002", 51.5, -0.1, t2);

        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isTrue();
        assertThat(result.severity()).isEqualTo("CRITICAL");
        assertThat(result.reason()).contains("km");
    }

    @Test
    void evaluate_normalLocalTravel_returnsPassed() {
        // Cape Town CBD to Stellenbosch ≈ 50 km — achievable in 1 hour by car
        OffsetDateTime t1 = OffsetDateTime.now().minusHours(1);
        OffsetDateTime t2 = OffsetDateTime.now();

        Transaction prior = buildTransaction("TX-001", -33.9249, 18.4241, t1);
        Transaction current = buildTransaction("TX-002", -33.9335, 18.8601, t2);

        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void evaluate_noCoordinatesOnCurrent_returnsSkipped() {
        Transaction current = Transaction.builder()
                .transactionId("TX-002")
                .amount(new BigDecimal("100.00"))
                .transactionAt(OffsetDateTime.now())
                .customer(customer)
                .build();

        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.outcome()).isEqualTo(RuleResult.Outcome.SKIPPED);
    }

    @Test
    void evaluate_noGeolocatedHistory_returnsPassed() {
        Transaction current = buildTransaction("TX-002", -33.9, 18.4, OffsetDateTime.now());
        // Prior transaction has no coordinates
        Transaction prior = Transaction.builder()
                .transactionId("TX-001")
                .amount(new BigDecimal("100.00"))
                .transactionAt(OffsetDateTime.now().minusMinutes(5))
                .customer(customer)
                .build();

        EvaluationContext ctx = new EvaluationContext(current, customer, List.of(prior), config);

        RuleResult result = rule.evaluate(ctx);

        assertThat(result.isFlagged()).isFalse();
    }

    @Test
    void haversineDistance_capeTownToJohannesburg() {
        // Cape Town (-33.9249, 18.4241) to Johannesburg (-26.2041, 28.0473) ≈ 1270 km
        double distance = GeographicAnomalyRule.haversineDistanceKm(
                -33.9249, 18.4241, -26.2041, 28.0473);
        assertThat(distance).isCloseTo(1270.0, within(50.0));
    }
}
