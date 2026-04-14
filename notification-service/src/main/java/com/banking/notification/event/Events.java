package com.banking.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event POJOs consumed from Kafka topics.
 *
 * TEACHING POINT — Consumer-Side Event Classes:
 * The notification service defines its OWN copies of these event classes.
 * This is intentional — we do NOT share a JAR between services.
 *
 * Benefits of NOT sharing:
 *   - Services can evolve independently
 *   - Adding a field to the producer doesn't break consumers
 *   - Each service owns its own domain model
 *
 * The only contract is the JSON structure on the wire.
 * Jackson's JsonIgnoreProperties(ignoreUnknown=true) handles extra fields gracefully.
 */
public class Events {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountEvent {
        private String eventType;
        private Long accountId;
        private String accountNumber;
        private String ownerName;
        private String email;
        private BigDecimal balance;
        private LocalDateTime timestamp;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionEvent {
        private String eventType;
        private String transactionRef;
        private String accountNumber;
        private String targetAccountNumber;
        private BigDecimal amount;
        private String type;
        private BigDecimal balanceAfter;
        private LocalDateTime timestamp;
    }
}
