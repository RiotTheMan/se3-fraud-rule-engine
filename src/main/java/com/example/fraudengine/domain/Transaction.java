package com.example.fraudengine.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Represents a financial transaction ingested from the categorisation Kafka topic.
 *
 * The {@code idempotencyKey} field (UNIQUE constraint) is the primary guard against
 * duplicate processing. Kafka at-least-once delivery can cause redelivery; attempting to
 * insert a duplicate key will throw a constraint violation which we catch in the service
 * layer and treat as a no-op.
 *
 * Geographic coordinates are stored as plain doubles rather than PostGIS geometry to
 * avoid a PostGIS dependency. The {@link com.example.fraudengine.engine.rules.GeographicAnomalyRule}
 * uses the Haversine formula on these values for distance calculations.
 */
@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_id", nullable = false, unique = true, length = 100)
    private String transactionId;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "ZAR";

    @Column(name = "merchant_name")
    private String merchantName;

    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;

    @Column(name = "country_code", length = 3)
    private String countryCode;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "transaction_at", nullable = false)
    private OffsetDateTime transactionAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
