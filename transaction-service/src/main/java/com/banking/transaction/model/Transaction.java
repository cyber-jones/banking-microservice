package com.banking.transaction.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Transaction Entity — represents a single financial transaction.
 *
 * TEACHING POINT — Domain Modelling with JPA:
 * A transaction is IMMUTABLE in banking — you never edit a transaction,
 * you create correcting entries. This is reflected by no @PreUpdate
 * and no setter for amount/type after creation.
 *
 * TransactionType models the direction of money:
 *   DEPOSIT   — money coming IN to an account
 *   WITHDRAWAL — money going OUT of an account
 *   TRANSFER  — money moving between accounts
 *
 * TransactionStatus tracks the lifecycle:
 *   PENDING → COMPLETED or FAILED
 */
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_account_number", columnList = "account_number"),
        @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transaction_ref", unique = true, nullable = false)
    private String transactionRef;

    @Column(name = "account_number", nullable = false)
    private String accountNumber;

    @Column(name = "target_account_number")
    private String targetAccountNumber;  // for transfers

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.PENDING;

    private String description;

    @Column(name = "balance_after", precision = 15, scale = 2)
    private BigDecimal balanceAfter;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TransactionType {
        DEPOSIT, WITHDRAWAL, TRANSFER
    }

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, REVERSED
    }
}
