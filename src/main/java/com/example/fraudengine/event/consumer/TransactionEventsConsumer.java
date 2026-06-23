package com.example.fraudengine.event.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import com.example.fraudengine.event.dto.TransactionCategorizedEvent;
import com.example.fraudengine.service.TransactionIngestionService;

/**
 * Kafka consumer for transaction events; the consumer group ID and topic are injected
 * from config so the same binary works across environments.
 */
@Slf4j
@Component
@KafkaListener(
        id = "${kafka-listener-id}",
        topics = "#{'${kafka-listener-topics}'.split(',')}"
)
@RequiredArgsConstructor
public class TransactionEventsConsumer {

    private final TransactionIngestionService transactionIngestionService;

    @KafkaHandler
    public void handleTransactionCategorized(
            @Payload TransactionCategorizedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.debug("Received TransactionCategorizedEvent: transactionId={}, partition={}, offset={}, topic={}",
                event.getTransactionId(), partition, offset, topic);

        try {
            transactionIngestionService.ingest(event);
        } catch (Exception e) {
            log.error("Failed to process TransactionCategorizedEvent transactionId={}: {}",
                    event.getTransactionId(), e.getMessage(), e);
            throw e; // re-throw to trigger Kafka retry / DLQ
        }
    }

    /**
     * Default handler for unrecognised message types. Logs and discards to avoid blocking
     * the consumer. In production, these would be routed to a separate dead-letter topic.
     */
    @KafkaHandler(isDefault = true)
    public void handleUnknown(
            Object payload,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {

        log.warn("Received unrecognised message type={} on topic={}, partition={}, offset={}. Discarding.",
                payload == null ? "null" : payload.getClass().getSimpleName(),
                topic, partition, offset);
    }
}
