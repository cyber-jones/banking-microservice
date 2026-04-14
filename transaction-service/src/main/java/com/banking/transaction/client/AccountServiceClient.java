package com.banking.transaction.client;

import com.banking.transaction.dto.AccountClientDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * AccountServiceClient — FeignClient for calling Account Service.
 *
 * TEACHING POINT — Service-to-Service Communication:
 * Transaction Service needs to:
 *  1. Verify the account exists before creating a transaction
 *  2. Update the account balance after a transaction
 *
 * With Feign + Eureka, this is transparent:
 *  - Eureka resolves "account-service" to the actual host:port
 *  - Feign handles the HTTP call, serialisation, and errors
 *  - Spring Cloud LoadBalancer distributes across multiple instances
 *
 * IMPORTANT: In a real system, updating balance via REST call is risky
 * (partial failure). Production systems use SAGA pattern or 2-phase commit.
 * For teaching purposes, this direct Feign call is clear and understandable.
 */
@FeignClient(name = "account-service", path = "/api/v1/accounts")
public interface AccountServiceClient {

    @GetMapping("/number/{accountNumber}")
    AccountClientDto.AccountResponse getAccountByNumber(@PathVariable String accountNumber);

    @PutMapping("/{id}/balance")
    AccountClientDto.AccountResponse updateBalance(
            @PathVariable Long id,
            @RequestBody AccountClientDto.BalanceUpdateRequest request);
}
