package com.banking.account.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;

/**
 * TransactionServiceClient — a declarative HTTP client using OpenFeign.
 *
 * TEACHING POINT — FeignClient:
 *
 * Instead of writing boilerplate RestTemplate code, Feign lets you declare
 * an interface that looks just like a @RestController mapping.
 * Spring generates the HTTP client implementation at runtime.
 *
 * @FeignClient(name = "transaction-service")
 *   - "transaction-service" is the service NAME registered in Eureka
 *   - Feign + Eureka = automatic load balancing (no hardcoded URLs!)
 *   - If transaction-service runs on 3 instances, Feign round-robins automatically
 *
 * Compare WITHOUT Feign (verbose):
 *   String url = "http://transaction-service/api/v1/transactions/account/" + accountNumber;
 *   ResponseEntity<List> resp = restTemplate.getForEntity(url, List.class);
 *
 * Compare WITH Feign (clean):
 *   client.getTransactionsByAccount(accountNumber);
 *
 * @EnableFeignClients on AccountServiceApplication activates all @FeignClient interfaces.
 */
@FeignClient(name = "transaction-service", path = "/api/v1/transactions")
public interface TransactionServiceClient {

    /**
     * Fetch all transactions for a given account number.
     * Maps to GET http://transaction-service/api/v1/transactions/account/{accountNumber}
     */
    @GetMapping("/account/{accountNumber}")
    List<Map<String, Object>> getTransactionsByAccount(@PathVariable String accountNumber);

    /**
     * Fetch account balance summary from transaction service perspective.
     */
    @GetMapping("/account/{accountNumber}/summary")
    Map<String, Object> getAccountSummary(@PathVariable String accountNumber);
}
