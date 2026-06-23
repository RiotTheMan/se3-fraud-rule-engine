package com.example.fraudengine.engine.rules;

import org.springframework.stereotype.Component;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.EvaluationContext;
import com.example.fraudengine.engine.FraudRuleStrategy;
import com.example.fraudengine.engine.RuleResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Geographic Anomaly Rule — detects physically impossible travel between two transactions.
 *
 * <p>If a customer made a transaction in Location A and the current transaction is in
 * Location B, we calculate the straight-line distance using the Haversine formula. If the
 * distance exceeds what is physically possible given the elapsed time (assuming maximum
 * commercial flight speed of ~900 km/h), the transaction is flagged.</p>
 *
 * <p>Only transactions with both latitude and longitude are considered. Transactions
 * without coordinates are excluded from the distance check.</p>
 *
 * <p>{@code threshold} in the DB is the maximum impossible speed in km/h. Default: 500 km/h
 * (well below commercial flight speed, flags improbable but not quite impossible travel).
 * The {@code window_minutes} controls how far back to look for prior geolocated transactions.</p>
 */
@Component
public class GeographicAnomalyRule implements FraudRuleStrategy {

    private static final String RULE_NAME = "GEOGRAPHIC_ANOMALY_RULE";
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double DEFAULT_MAX_SPEED_KMH = 500.0;

    @Override
    public String getRuleName() {
        return RULE_NAME;
    }

    @Override
    public RuleResult evaluate(EvaluationContext context) {
        Transaction current = context.transaction();

        if (current.getLatitude() == null || current.getLongitude() == null) {
            return RuleResult.skipped(RULE_NAME, "Current transaction has no geographic coordinates");
        }

        double maxSpeedKmh = context.ruleConfig().getThreshold() != null
                ? context.ruleConfig().getThreshold().doubleValue()
                : DEFAULT_MAX_SPEED_KMH;

        List<Transaction> geolocatedPrior = context.recentTransactions().stream()
                .filter(t -> t.getLatitude() != null && t.getLongitude() != null)
                .filter(t -> !t.getTransactionId().equals(current.getTransactionId()))
                .toList();

        for (Transaction prior : geolocatedPrior) {
            double distanceKm = haversineDistanceKm(
                    prior.getLatitude(), prior.getLongitude(),
                    current.getLatitude(), current.getLongitude()
            );

            long elapsedSeconds = Math.abs(
                    current.getTransactionAt().toEpochSecond() - prior.getTransactionAt().toEpochSecond()
            );

            if (elapsedSeconds == 0) {
                continue; // same timestamp, can't compute speed
            }

            double speedKmh = distanceKm / (elapsedSeconds / 3600.0);

            if (speedKmh > maxSpeedKmh) {
                String reason = String.format(
                        "Impossible travel detected: %.1f km in %d seconds (%.1f km/h) between transaction %s and current transaction. Max allowed: %.1f km/h",
                        distanceKm,
                        elapsedSeconds,
                        speedKmh,
                        prior.getTransactionId(),
                        maxSpeedKmh
                );
                return RuleResult.flagged(RULE_NAME, reason, context.ruleConfig().getSeverity());
            }
        }

        return RuleResult.passed(RULE_NAME);
    }

    static double haversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }
}
