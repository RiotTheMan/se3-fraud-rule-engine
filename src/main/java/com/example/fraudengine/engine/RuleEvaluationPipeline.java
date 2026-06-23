package com.example.fraudengine.engine;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudRule;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.repository.TransactionRepository;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Orchestrates full evaluation of all active rules against a transaction.
 *
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleEvaluationPipeline {

    private final RuleRegistry ruleRegistry;
    private final TransactionRepository transactionRepository;

    /**
     * Evaluates all enabled rules for the given transaction in parallel on virtual threads.
     *
     * @param transaction the transaction to evaluate
     * @param customer    the owning customer
     * @return list of results from all evaluated rules
     */
    public List<RuleResult> evaluate(Transaction transaction, Customer customer) {
        List<FraudRuleStrategy> activeRules = ruleRegistry.getActiveRules();

        if (activeRules.isEmpty()) {
            log.warn("No active rules found in registry; skipping evaluation for transaction={}",
                    transaction.getTransactionId());
            return List.of();
        }

        // Build evaluation tasks — one per active rule
        List<Callable<RuleResult>> tasks = activeRules.stream()
                .map(rule -> (Callable<RuleResult>) () -> evaluateRule(rule, transaction, customer))
                .toList();

        List<RuleResult> results = new ArrayList<>(tasks.size());

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<RuleResult>> futures = executor.invokeAll(tasks);
            for (Future<RuleResult> future : futures) {
                try {
                    results.add(future.get());
                } catch (ExecutionException e) {
                    log.error("Rule evaluation threw an unexpected exception: {}", e.getCause().getMessage(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rule evaluation was interrupted for transaction={}", transaction.getTransactionId());
        }

        log.debug("Evaluated {} rules for transaction={}; flagged={}",
                results.size(),
                transaction.getTransactionId(),
                results.stream().filter(RuleResult::isFlagged).count());

        return results;
    }

    private RuleResult evaluateRule(FraudRuleStrategy rule, Transaction transaction, Customer customer) {
        FraudRule config = ruleRegistry.getConfig(rule.getRuleName());
        if (config == null || !Boolean.TRUE.equals(config.getEnabled())) {
            return RuleResult.skipped(rule.getRuleName(), "Rule disabled or not configured");
        }

        int windowMinutes = config.getWindowMinutes() != null ? config.getWindowMinutes() : 60;
        OffsetDateTime windowStart = transaction.getTransactionAt().minusMinutes(windowMinutes);
        List<Transaction> recentTransactions = transactionRepository
                .findByCustomerAndTransactionAtBetween(customer, windowStart, transaction.getTransactionAt());

        EvaluationContext context = new EvaluationContext(transaction, customer, recentTransactions, config);

        try {
            return rule.evaluate(context);
        } catch (Exception e) {
            log.error("Rule {} threw an exception during evaluation: {}", rule.getRuleName(), e.getMessage(), e);
            return RuleResult.skipped(rule.getRuleName(), "Rule evaluation failed: " + e.getMessage());
        }
    }
}
