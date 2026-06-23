INSERT INTO fraud_rules (rule_name, description, enabled, severity, threshold, window_minutes) VALUES
(
    'VELOCITY_RULE',
    'Flags when a customer exceeds the maximum number of transactions within a rolling time window. Detects rapid sequential transactions that may indicate card cloning or automated fraud.',
    TRUE,
    'HIGH',
    10,
    60
),
(
    'LARGE_AMOUNT_RULE',
    'Flags transactions that exceed a configured monetary threshold. Identifies unusually large single transactions that deviate from typical spend behaviour.',
    TRUE,
    'HIGH',
    50000.00,
    NULL
),
(
    'UNUSUAL_HOUR_RULE',
    'Flags transactions occurring outside normal banking hours (00:00-05:59 SAST). Night-time transactions are statistically more likely to be fraudulent.',
    TRUE,
    'MEDIUM',
    NULL,
    NULL
),
(
    'DUPLICATE_TRANSACTION_RULE',
    'Flags when the same amount is charged to the same customer by the same merchant within a short window. Duplicate charges often indicate merchant errors or double-processing fraud.',
    TRUE,
    'HIGH',
    NULL,
    5
),
(
    'GEOGRAPHIC_ANOMALY_RULE',
    'Flags when a customer makes transactions in geographically impossible locations within a short time window. Detects simultaneous card use across distant locations.',
    TRUE,
    'CRITICAL',
    500.00,
    120
),
(
    'CATEGORY_MISMATCH_RULE',
    'Flags transactions in merchant categories that are atypical for the customer based on their historical transaction profile. High-risk categories (gambling, crypto) receive elevated scrutiny.',
    TRUE,
    'LOW',
    NULL,
    NULL
);
