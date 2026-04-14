package com.banking.account.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Account Entity — mapped to the ACCOUNTS table via JPA/Hibernate.
 *
 * TEACHING POINT — JPA Annotations:
 *
 * @Entity       — marks this class as a JPA entity (maps to a DB table)
 * @Table        — customises the table name (optional; defaults to class name)
 * @Id           — marks the primary key field
 * @GeneratedValue — auto-generates the PK value (IDENTITY = DB auto-increment)
 * @Column       — customises column mapping (name, nullability, uniqueness, length)
 * @Enumerated   — maps Java enum to a DB column (STRING stores the enum name as text)
 *
 * TEACHING POINT — Lombok:
 * @Data         — generates getters, setters, equals, hashCode, toString
 * @Builder      — enables the Builder pattern: Account.builder().build()
 * @NoArgsConstructor / @AllArgsConstructor — generate constructors
 */
@Entity
@Table(name = "accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", unique = true, nullable = false, length = 20)
    private String accountNumber;

    @NotBlank
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @NotBlank
    @Column(unique = true, nullable = false)
    private String email;

    @NotNull
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AccountStatus status = AccountStatus.ACTIVE;

    /**
     * TEACHING POINT — @PrePersist / @PreUpdate are JPA lifecycle callbacks.
     * They fire automatically before INSERT and UPDATE operations.
     */
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum AccountType {
        CHECKING, SAVINGS, BUSINESS
    }

    public enum AccountStatus {
        ACTIVE, INACTIVE, CLOSED, FROZEN
    }
}
