package com.example.fraudengine.event.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Kafka event payload received from the transaction categorisation service.
 *
 * <p>Published to topic {@code ${environment}-transactions-categorized}. The event carries
 * a categorised transaction with the merchant category populated. The {@code idempotencyKey}
 * is used to prevent duplicate processing at the database layer.</p>
 *
 * <p>{@code @JsonIgnoreProperties(ignoreUnknown = true)} ensures forward-compatibility —
 * future fields added by the upstream producer do not break this consumer.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TransactionCategorizedEvent {

    private String transactionId;
    private String idempotencyKey;
    private String customerId;
    private String customerFullName;
    private BigDecimal amount;
    private String currency;
    private String merchantName;
    private String merchantCategory;
    private String countryCode;
    private Double latitude;
    private Double longitude;
    private OffsetDateTime transactionAt;
}
