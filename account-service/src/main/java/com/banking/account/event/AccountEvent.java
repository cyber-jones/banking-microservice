package com.banking.account.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AccountEvent — the Kafka message payload published by AccountService.
 *
 * TEACHING POINT — Event-Driven Architecture with Kafka:
 *
 * Instead of AccountService calling NotificationService directly (tight coupling),
 * it publishes an event to a Kafka topic. Any service that cares about this
 * event subscribes to the topic — completely decoupled.
 *
 * Benefits:
 *  - Services don't know about each other
 *  - If NotificationService is down, events queue up and are processed when it restarts
 *  - Easy to add new consumers without changing the producer
 *
 * This event is serialised to JSON by Kafka's JsonSerializer and
 * deserialised back to AccountEvent by the consumer.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEvent {
    private String eventType;       // "ACCOUNT_CREATED", "ACCOUNT_CLOSED", etc.
    private Long accountId;
    private String accountNumber;
    private String ownerName;
    private String email;
    private BigDecimal balance;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
