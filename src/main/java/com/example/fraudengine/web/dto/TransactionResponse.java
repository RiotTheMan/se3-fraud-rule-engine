package com.example.fraudengine.web.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record TransactionResponse(
        Long id,
        String transactionId,
        String customerId,
        BigDecimal amount,
        String currency,
        String merchantName,
        String merchantCategory,
        String countryCode,
        OffsetDateTime transactionAt,
        OffsetDateTime createdAt
) {
}
