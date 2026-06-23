package com.example.fraudengine.web.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import com.example.fraudengine.config.JwtSecurityConfiguration;
import com.example.fraudengine.domain.Customer;
import com.example.fraudengine.errors.GlobalExceptionHandler;
import com.example.fraudengine.domain.FraudFlag;
import com.example.fraudengine.domain.Transaction;
import com.example.fraudengine.service.FraudFlagService;
import com.example.fraudengine.web.mapper.FraudFlagMapper;
import com.example.fraudengine.web.mapper.FraudFlagMapperImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller slice test for {@link FraudFlagController}.
 *
 * <p>{@code @ActiveProfiles("test")} activates the test profile, which causes
 * {@link com.example.fraudengine.config.SecurityConfig} (annotated
 * {@code @Profile("!test")}) to be excluded from the context. Security is then
 * handled by Spring Security Test's {@code @WithMockUser}.</p>
 */
@WebMvcTest(FraudFlagController.class)
@ActiveProfiles("test")
@Import({FraudFlagMapperImpl.class, JwtSecurityConfiguration.class, GlobalExceptionHandler.class, MethodSecurityTestConfig.class})
class FraudFlagControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FraudFlagService fraudFlagService;

    @MockBean
    private FraudFlagMapper fraudFlagMapper;

    @Test
    @WithMockUser(roles = "FraudEngineRead")
    void listFlags_authenticated_returns200WithPage() throws Exception {
        Customer customer = Customer.builder()
                .id(1L).customerId("CUST-001").fullName("Test Customer").build();
        Transaction transaction = Transaction.builder()
                .id(1L).transactionId("TX-001")
                .amount(new BigDecimal("500.00")).currency("ZAR")
                .transactionAt(OffsetDateTime.now())
                .customer(customer).build();
        FraudFlag flag = FraudFlag.builder()
                .id(1L)
                .transaction(transaction)
                .customer(customer)
                .ruleName("VELOCITY_RULE")
                .severity(FraudFlag.Severity.HIGH)
                .status(FraudFlag.Status.OPEN)
                .reason("Too many transactions")
                .createdAt(OffsetDateTime.now())
                .build();

        when(fraudFlagService.findAll(any())).thenReturn(
                new PageImpl<>(List.of(flag), PageRequest.of(0, 20), 1));

        // Return a mocked DTO rather than depending on real mapper
        when(fraudFlagMapper.toResponse(any(FraudFlag.class))).thenReturn(
                new com.example.fraudengine.web.dto.FraudFlagResponse(
                        1L, "TX-001", "CUST-001", "VELOCITY_RULE",
                        "HIGH", "OPEN", "Too many transactions", OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/fraud-flags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].ruleName").value("VELOCITY_RULE"))
                .andExpect(jsonPath("$.content[0].status").value("OPEN"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @WithMockUser(roles = "FraudEngineRead")
    void getFlag_existingId_returns200() throws Exception {
        Customer customer = Customer.builder()
                .id(1L).customerId("CUST-001").fullName("Test Customer").build();
        Transaction transaction = Transaction.builder()
                .id(1L).transactionId("TX-001")
                .amount(new BigDecimal("500.00")).currency("ZAR")
                .merchantName("Test Merchant")
                .transactionAt(OffsetDateTime.now())
                .customer(customer).build();
        FraudFlag flag = FraudFlag.builder()
                .id(1L)
                .transaction(transaction)
                .customer(customer)
                .ruleName("LARGE_AMOUNT_RULE")
                .severity(FraudFlag.Severity.HIGH)
                .status(FraudFlag.Status.OPEN)
                .reason("Amount exceeds threshold")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        when(fraudFlagService.findById(1L)).thenReturn(flag);
        when(fraudFlagMapper.toDetailResponse(flag)).thenReturn(
                new com.example.fraudengine.web.dto.FraudFlagDetailResponse(
                        1L,
                        new com.example.fraudengine.web.dto.FraudFlagDetailResponse.TransactionSummary(
                                1L, "TX-001", new BigDecimal("500.00"), "ZAR",
                                "Test Merchant", OffsetDateTime.now()),
                        "CUST-001", "Test Customer",
                        "LARGE_AMOUNT_RULE", "HIGH", "OPEN",
                        "Amount exceeds threshold", null,
                        OffsetDateTime.now(), OffsetDateTime.now()));

        mockMvc.perform(get("/api/v1/fraud-flags/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruleName").value("LARGE_AMOUNT_RULE"))
                .andExpect(jsonPath("$.transaction.transactionId").value("TX-001"));
    }

    @Test
    void listFlags_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/fraud-flags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "WRONG_ROLE")
    void listFlags_wrongRole_returns403() throws Exception {
        // A user is authenticated but does not hold FraudEngineRead or FraudEngineWrite.
        // The @PreAuthorize on the controller must reject this with 403.
        // This test proves RBAC is wired correctly — not just that auth exists.
        mockMvc.perform(get("/api/v1/fraud-flags")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "FraudEngineRead")
    void getFlag_nonExistentId_returns404() throws Exception {
        // Service throws EntityNotFoundException for an unknown id.
        // GlobalExceptionHandler maps this to 404 with {status, message, source, errors} body.
        when(fraudFlagService.findById(999L))
                .thenThrow(new jakarta.persistence.EntityNotFoundException(
                        "FraudFlag not found with id=999"));

        mockMvc.perform(get("/api/v1/fraud-flags/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.source").value("FRAUD_ENGINE_SERVICE"));
    }
}
