package com.example.fraudengine.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudFlag;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.engine.RuleResult;
import com.example.fraudengine.repository.FraudFlagRepository;
import com.example.fraudengine.repository.TransactionRepository;

import java.util.List;

/**
 * Manages the lifecycle of {@link FraudFlag} entities.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FraudFlagService {

    private final FraudFlagRepository fraudFlagRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Persists a {@link FraudFlag} for each {@link RuleResult} with outcome FLAGGED.
     */
    @Transactional
    public void persistFlags(Transaction transaction, Customer customer, List<RuleResult> results) {
        List<FraudFlag> flags = results.stream()
                .filter(RuleResult::isFlagged)
                .map(result -> FraudFlag.builder()
                        .transaction(transaction)
                        .customer(customer)
                        .ruleName(result.ruleName())
                        .severity(parseSeverity(result.severity()))
                        .reason(result.reason())
                        .build())
                .toList();

        if (!flags.isEmpty()) {
            fraudFlagRepository.saveAll(flags);
            log.info("Persisted {} fraud flag(s) for transactionId={}",
                    flags.size(), transaction.getTransactionId());
        }
    }

    @Transactional(readOnly = true)
    public Page<FraudFlag> findAll(Pageable pageable) {
        return fraudFlagRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public FraudFlag findById(Long id) {
        return fraudFlagRepository.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "FraudFlag not found with id=" + id));
    }

    @Transactional(readOnly = true)
    public List<FraudFlag> findByTransaction(Long transactionId) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Transaction not found with id=" + transactionId));
        return fraudFlagRepository.findByTransaction(transaction);
    }

    private FraudFlag.Severity parseSeverity(String severity) {
        if (severity == null) return FraudFlag.Severity.MEDIUM;
        try {
            return FraudFlag.Severity.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FraudFlag.Severity.MEDIUM;
        }
    }
}
