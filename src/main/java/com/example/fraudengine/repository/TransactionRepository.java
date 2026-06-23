package com.example.fraudengine.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.Transaction;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByTransactionId(String transactionId);

    boolean existsByIdempotencyKey(String idempotencyKey);

    List<Transaction> findByCustomerAndTransactionAtBetween(
            Customer customer,
            OffsetDateTime from,
            OffsetDateTime to
    );

    Page<Transaction> findByCustomer(Customer customer, Pageable pageable);
}
