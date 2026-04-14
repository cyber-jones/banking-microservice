package com.banking.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * ApiGatewayApplication — the single entry point for all client traffic.
 *
 * TEACHING POINT — API Gateway Pattern:
 *
 * Without a gateway, clients need to know about every service:
 *   client → account-service:8081
 *   client → transaction-service:8082
 *   client → notification-service:8083
 *
 * With a gateway, clients talk to ONE address:
 *   client → api-gateway:8080 → routes to the right service
 *
 * Responsibilities of an API Gateway:
 *   1. Routing          — forward /api/v1/accounts/** to account-service
 *   2. Authentication   — validate JWT ONCE here, not on every service
 *   3. Rate Limiting    — prevent abuse (add spring-cloud-starter-circuitbreaker-reactor-resilience4j)
 *   4. Load Balancing   — distribute traffic across service instances (via Eureka lb://)
 *   5. Cross-cutting    — logging, metrics, CORS handled in one place
 *
 * Spring Cloud Gateway is reactive (WebFlux) so it handles thousands of
 * concurrent connections efficiently with a small thread pool.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
