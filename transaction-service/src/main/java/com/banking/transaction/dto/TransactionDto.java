package com.banking.transaction.dto;

import com.banking.transaction.model.Transaction;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionDto {

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DepositRequest {
        @NotBlank(message = "Account number is required")
        private String accountNumber;

        @NotNull @DecimalMin(value = "0.01", message = "Deposit must be positive")
        private BigDecimal amount;

        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class WithdrawalRequest {
        @NotBlank
        private String accountNumber;

        @NotNull @DecimalMin(value = "0.01")
        private BigDecimal amount;

        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TransferRequest {
        @NotBlank
        private String fromAccountNumber;

        @NotBlank
        private String toAccountNumber;

        @NotNull @DecimalMin(value = "10.00", message = "Minimum transfer amount is $10.00")
        private BigDecimal amount;

        private String description;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class Response {
        private Long id;
        private String transactionRef;
        private String accountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private Transaction.TransactionType type;
        private Transaction.TransactionStatus status;
        private String description;
        private BigDecimal balanceAfter;
        private LocalDateTime createdAt;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AccountSummary {
        private String accountNumber;
        private BigDecimal totalDeposits;
        private BigDecimal totalWithdrawals;
        private long transactionCount;
        private BigDecimal currentBalance;
    }
}
