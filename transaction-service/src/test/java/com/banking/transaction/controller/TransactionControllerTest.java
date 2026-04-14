package com.banking.transaction.controller;

import com.banking.transaction.dto.TransactionDto;
import com.banking.transaction.model.Transaction;
import com.banking.transaction.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * TransactionControllerTest — tests HTTP layer in isolation.
 *
 * TEACHING POINT — @WebMvcTest with Security disabled:
 * We import our permissive SecurityConfig so all requests pass through.
 * This keeps tests focused on HTTP behaviour, not auth logic.
 */
@WebMvcTest(TransactionController.class)
@DisplayName("TransactionController Web Layer Tests")
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    @Test
    @DisplayName("POST /deposit - should return 200 with transaction response")
    void deposit_ValidRequest_Returns200() throws Exception {
        TransactionDto.DepositRequest request = TransactionDto.DepositRequest.builder()
                .accountNumber("ACC0000000001")
                .amount(new BigDecimal("1000.00"))
                .description("Salary")
                .build();

        TransactionDto.Response mockResponse = TransactionDto.Response.builder()
                .id(1L)
                .transactionRef("TXN-ABC123")
                .accountNumber("ACC0000000001")
                .amount(new BigDecimal("1000.00"))
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .balanceAfter(new BigDecimal("6000.00"))
                .createdAt(LocalDateTime.now())
                .build();

        when(transactionService.deposit(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionRef").value("TXN-ABC123"))
                .andExpect(jsonPath("$.amount").value(1000.00))
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("POST /deposit - missing amount should return 400")
    void deposit_MissingAmount_Returns400() throws Exception {
        String invalidJson = """
                {
                  "accountNumber": "ACC0000000001"
                }
                """;

        mockMvc.perform(post("/api/v1/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /withdraw - insufficient funds returns 400")
    void withdraw_InsufficientFunds_Returns400() throws Exception {
        TransactionDto.WithdrawalRequest request = TransactionDto.WithdrawalRequest.builder()
                .accountNumber("ACC0000000001")
                .amount(new BigDecimal("99999.00"))
                .build();

        when(transactionService.withdraw(any()))
                .thenThrow(new IllegalStateException("Insufficient funds"));

        mockMvc.perform(post("/api/v1/transactions/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
