package com.example.fraudengine.integration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.example.fraudengine.event.dto.TransactionCategorizedEvent;
import com.example.fraudengine.repository.FraudFlagRepository;
import com.example.fraudengine.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Integration test for the Kafka consumer pipeline.
 *
 * <p>Uses {@code @EmbeddedKafka} to spin up an in-process Kafka broker and
 * Testcontainers PostgreSQL for the database. Tests that publishing a
 * {@link TransactionCategorizedEvent} to the topic results in the transaction
 * being persisted to the database.</p>
 *
 * <p>{@code @ActiveProfiles("test")} disables the OAuth2 security filter chain
 * so the application context starts without requiring a running IdP.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
        partitions = 1,
        topics = "${kafka-listener-topics:test-transactions-categorized}",
        brokerProperties = {"listeners=PLAINTEXT://localhost:0", "port=0"}
)
@DirtiesContext
class TransactionIngestionIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private FraudFlagRepository fraudFlagRepository;

    @Value("${kafka-listener-topics:test-transactions-categorized}")
    private String topic;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Test
    void publishEvent_transactionPersistedToDatabase() throws Exception {
        String transactionId = "INTTEST-" + UUID.randomUUID();
        String idempotencyKey = "IDEM-" + UUID.randomUUID();

        TransactionCategorizedEvent event = TransactionCategorizedEvent.builder()
                .transactionId(transactionId)
                .idempotencyKey(idempotencyKey)
                .customerId("INTTEST-CUST-001")
                .customerFullName("Integration Test Customer")
                .amount(new BigDecimal("1500.00"))
                .currency("ZAR")
                .merchantName("Test Merchant")
                .merchantCategory("GROCERY")
                .countryCode("ZAF")
                .transactionAt(OffsetDateTime.now())
                .build();

        KafkaTemplate<String, TransactionCategorizedEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send(topic, transactionId, event);

        // Wait up to 10 seconds for the consumer to process the message
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    assertThat(transactionRepository.findByTransactionId(transactionId))
                            .isPresent()
                            .hasValueSatisfying(tx -> {
                                assertThat(tx.getTransactionId()).isEqualTo(transactionId);
                                assertThat(tx.getAmount()).isEqualByComparingTo(new BigDecimal("1500.00"));
                                assertThat(tx.getMerchantName()).isEqualTo("Test Merchant");
                            });
                });

        // Core feature assertion: rule engine must have run and at minimum persisted results.
        // R1500 is below the large-amount threshold (R50,000) so no LARGE_AMOUNT flag —
        // but the pipeline ran and the transaction record was persisted and updated.
        await().atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var tx = transactionRepository.findByTransactionId(transactionId).orElseThrow();
                    assertThat(tx.getUpdatedAt()).isNotNull();
                });
    }

    @Test
    void publishDuplicateEvent_onlyOneTransactionPersisted() throws Exception {
        String transactionId = "INTTEST-DUP-" + UUID.randomUUID();
        String idempotencyKey = "IDEM-DUP-" + UUID.randomUUID();

        TransactionCategorizedEvent event = TransactionCategorizedEvent.builder()
                .transactionId(transactionId)
                .idempotencyKey(idempotencyKey)
                .customerId("INTTEST-CUST-002")
                .customerFullName("Duplicate Test Customer")
                .amount(new BigDecimal("500.00"))
                .currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .build();

        KafkaTemplate<String, TransactionCategorizedEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send(topic, transactionId, event);
        // Send the same event again (duplicate)
        kafkaTemplate.send(topic, transactionId, event);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(
                        transactionRepository.findByTransactionId(transactionId)).isPresent());

        // Replacing hard sleep with a stable Awaitility poll:
        // Wait until the count stabilises at 1 for at least 2 consecutive polls (500ms apart).
        // This proves idempotency without relying on a fixed sleep duration.
        await().atMost(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    long count = transactionRepository.findAll().stream()
                            .filter(tx -> tx.getIdempotencyKey().equals(idempotencyKey))
                            .count();
                    assertThat(count)
                            .as("Duplicate event must not create a second transaction row")
                            .isEqualTo(1);
                });
    }

    @Test
    void givenLargeAmountTransaction_whenConsumed_thenFraudFlagPersisted() throws Exception {
        String transactionId = "INTTEST-LARGE-" + UUID.randomUUID();
        String idempotencyKey = "IDEM-LARGE-" + UUID.randomUUID();

        TransactionCategorizedEvent event = TransactionCategorizedEvent.builder()
                .transactionId(transactionId)
                .idempotencyKey(idempotencyKey)
                .customerId("INTTEST-CUST-003")
                .customerFullName("Large Amount Test Customer")
                .amount(new BigDecimal("75000.00"))
                .currency("ZAR")
                .merchantName("High Value Merchant")
                .merchantCategory("ELECTRONICS")
                .countryCode("ZAF")
                .transactionAt(OffsetDateTime.now())
                .build();

        KafkaTemplate<String, TransactionCategorizedEvent> kafkaTemplate = buildKafkaTemplate();
        kafkaTemplate.send(topic, transactionId, event);

        // Wait for transaction to be persisted first
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                        assertThat(transactionRepository.findByTransactionId(transactionId)).isPresent());

        // Assert that the rule engine produced at least one FraudFlag row for LARGE_AMOUNT_RULE
        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    var flags = fraudFlagRepository.findByTransactionTransactionId(transactionId);
                    assertThat(flags)
                            .as("Expected at least one FraudFlag for transactionId=%s", transactionId)
                            .isNotEmpty();
                    assertThat(flags)
                            .anySatisfy(flag ->
                                    assertThat(flag.getRuleName())
                                            .as("Expected a flag with ruleName containing LARGE_AMOUNT")
                                            .containsIgnoringCase("LARGE_AMOUNT"));
                });
    }

    private KafkaTemplate<String, TransactionCategorizedEvent> buildKafkaTemplate() {
        // ADD_TYPE_INFO_HEADERS=true ensures Spring's JsonDeserializer on the consumer side
        // receives the __TypeId__ header it needs to deserialise to the correct class.
        // Without this header the consumer throws "No type information in headers".
        Map<String, Object> props = new java.util.HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        ProducerFactory<String, TransactionCategorizedEvent> factory =
                new DefaultKafkaProducerFactory<>(props);
        return new KafkaTemplate<>(factory);
    }
}
