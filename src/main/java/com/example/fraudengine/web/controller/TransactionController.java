package com.example.fraudengine.web.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.repository.TransactionRepository;
import com.example.fraudengine.service.FraudFlagService;
import com.example.fraudengine.web.dto.FraudFlagResponse;
import com.example.fraudengine.web.dto.TransactionResponse;
import com.example.fraudengine.web.mapper.FraudFlagMapper;
import com.example.fraudengine.web.mapper.TransactionMapper;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final FraudFlagService fraudFlagService;
    private final FraudFlagMapper fraudFlagMapper;

    @GetMapping("/{transactionId}")
    @PreAuthorize("hasAnyRole(@jwtSecurityConfiguration.getReadRole(), @jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Transaction not found: " + transactionId));
        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/{transactionId}/flags")
    @PreAuthorize("hasAnyRole(@jwtSecurityConfiguration.getReadRole(), @jwtSecurityConfiguration.getWriteRole())")
    public ResponseEntity<List<FraudFlagResponse>> getFlagsForTransaction(@PathVariable String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException(
                        "Transaction not found: " + transactionId));
        List<FraudFlagResponse> flags = fraudFlagService.findByTransaction(transaction.getId())
                .stream()
                .map(fraudFlagMapper::toResponse)
                .toList();
        return ResponseEntity.ok(flags);
    }
}
