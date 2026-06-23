package com.example.fraudengine.web.dto;

import java.time.OffsetDateTime;

public record FraudFlagResponse(
        Long id,
        String transactionId,
        String customerId,
        String ruleName,
        String severity,
        String status,
        String reason,
        OffsetDateTime createdAt
) {
}
