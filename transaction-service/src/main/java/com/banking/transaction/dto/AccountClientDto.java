package com.banking.transaction.dto;

import lombok.*;
import java.math.BigDecimal;

/** DTOs used when calling the Account Service via FeignClient. */
public class AccountClientDto {

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AccountResponse {
        private Long id;
        private String accountNumber;
        private String ownerName;
        private String email;
        private BigDecimal balance;
        private String accountType;
        private String status;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class BalanceUpdateRequest {
        private BigDecimal newBalance;
    }
}
