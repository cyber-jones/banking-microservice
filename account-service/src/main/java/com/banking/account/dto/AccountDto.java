package com.banking.account.dto;

import com.banking.account.model.Account;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Objects (DTOs) for the Account Service.
 *
 * TEACHING POINT — Why DTOs?
 * DTOs decouple your API contract from your database model:
 *   1. You control exactly what fields are exposed (hide internal fields)
 *   2. You can validate request data with Bean Validation annotations
 *   3. Your API shape can evolve independently of the DB schema
 *
 * This file uses static inner classes — a clean pattern to group related DTOs.
 */
public class AccountDto {

    /**
     * Request DTO — what the client sends to CREATE an account.
     * Bean Validation annotations enforce input rules automatically.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {

        @NotBlank(message = "Owner name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        private String ownerName;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid format")
        private String email;

        @NotNull(message = "Initial deposit is required")
        @DecimalMin(value = "0.01", message = "Initial deposit must be at least 0.01")
        private BigDecimal initialDeposit;

        @NotNull(message = "Account type is required")
        private Account.AccountType accountType;
    }

    /**
     * Response DTO — what we send BACK to the client.
     * Notice: no password, no internal IDs exposed unnecessarily.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private String accountNumber;
        private String ownerName;
        private String email;
        private BigDecimal balance;
        private Account.AccountType accountType;
        private Account.AccountStatus status;
        private LocalDateTime createdAt;
    }

    /**
     * Request DTO — for updating account details.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {

        @Size(min = 2, max = 100)
        private String ownerName;

        @Email
        private String email;
    }

    /**
     * Auth Request DTO — for login endpoint.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "Username is required")
        private String username;

        @NotBlank(message = "Password is required")
        private String password;
    }

    /**
     * Auth Response DTO — returns the JWT token after successful login.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String tokenType;
        private String username;
        private long expiresIn;
    }

    /**
     * Register Request DTO — for creating a new user.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegisterRequest {

        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @NotBlank
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        @NotBlank
        @Email
        private String email;
    }
}
