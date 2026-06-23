package com.example.fraudengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.RuleEvaluationPipeline;
import com.example.fraudengine.engine.RuleResult;
import com.example.fraudengine.event.dto.TransactionCategorizedEvent;
import com.example.fraudengine.repository.CustomerRepository;
import com.example.fraudengine.repository.TransactionRepository;

import java.util.List;

/**
 * Ingests a {@link TransactionCategorizedEvent}: upserts the customer, persists the
 * transaction, runs the fraud rule pipeline, and persists any resulting flags — all in
 * one database transaction. Individual rule failures produce SKIPPED outcomes and do not
 * roll back the ingestion. The {@code idempotency_key} DB constraint is the safety net
 * against concurrent duplicates; the application-level check is a fast path to avoid the
 * cost of a failed INSERT on the common case.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionIngestionService {

    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;
    private final RuleEvaluationPipeline ruleEvaluationPipeline;
    private final FraudFlagService fraudFlagService;

    @Transactional
    public void ingest(TransactionCategorizedEvent event) {
        // Fast-path idempotency check
        if (transactionRepository.existsByIdempotencyKey(event.getIdempotencyKey())) {
            log.debug("Duplicate event detected for idempotencyKey={}, skipping",
                    event.getIdempotencyKey());
            return;
        }

        Customer customer = upsertCustomer(event);
        Transaction transaction = persistTransaction(event, customer);

        List<RuleResult> results = ruleEvaluationPipeline.evaluate(transaction, customer);

        fraudFlagService.persistFlags(transaction, customer, results);

        long flaggedCount = results.stream().filter(RuleResult::isFlagged).count();
        log.info("Ingested transactionId={}, customerId={}, flaggedRules={}",
                transaction.getTransactionId(),
                customer.getCustomerId(),
                flaggedCount);
    }

    private Customer upsertCustomer(TransactionCategorizedEvent event) {
        return customerRepository.findByCustomerId(event.getCustomerId())
                .orElseGet(() -> {
                    Customer newCustomer = Customer.builder()
                            .customerId(event.getCustomerId())
                            .fullName(event.getCustomerFullName() != null
                                    ? event.getCustomerFullName()
                                    : event.getCustomerId())
                            .build();
                    return customerRepository.save(newCustomer);
                });
    }

    private Transaction persistTransaction(TransactionCategorizedEvent event, Customer customer) {
        Transaction transaction = Transaction.builder()
                .transactionId(event.getTransactionId())
                .idempotencyKey(event.getIdempotencyKey())
                .customer(customer)
                .amount(event.getAmount())
                .currency(event.getCurrency() != null ? event.getCurrency() : "ZAR")
                .merchantName(event.getMerchantName())
                .merchantCategory(event.getMerchantCategory())
                .countryCode(event.getCountryCode())
                .latitude(event.getLatitude())
                .longitude(event.getLongitude())
                .transactionAt(event.getTransactionAt())
                .build();
        return transactionRepository.save(transaction);
    }
}
