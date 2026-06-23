package com.example.fraudengine.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record FraudFlagDetailResponse(
        Long id,
        TransactionSummary transaction,
        String customerId,
        String customerFullName,
        String ruleName,
        String severity,
        String status,
        String reason,
        String ruleDetails,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public record TransactionSummary(
            Long id,
            String transactionId,
            BigDecimal amount,
            String currency,
            String merchantName,
            OffsetDateTime transactionAt
    ) {
    }
}
