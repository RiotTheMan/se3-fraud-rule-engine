package com.example.fraudengine.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.domain.FraudFlag;
import com.example.fraudengine.domain.Transaction;

import java.util.List;

@Repository
public interface FraudFlagRepository extends JpaRepository<FraudFlag, Long> {

    List<FraudFlag> findByTransaction(Transaction transaction);

    List<FraudFlag> findByTransactionTransactionId(String transactionId);

    Page<FraudFlag> findByCustomerAndStatus(Customer customer, FraudFlag.Status status, Pageable pageable);

    Page<FraudFlag> findByStatus(FraudFlag.Status status, Pageable pageable);

    Page<FraudFlag> findAll(Pageable pageable);
}
