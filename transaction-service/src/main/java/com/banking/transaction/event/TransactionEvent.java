package com.banking.transaction.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TransactionEvent — Kafka message published after every transaction.
 *
 * TEACHING POINT — Event Payload Design:
 * Keep event payloads self-contained. The consumer (NotificationService)
 * should not need to call back to get more data — include everything needed
 * to act on the event in the payload itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private String eventType;       // "DEPOSIT_COMPLETED", "WITHDRAWAL_COMPLETED", "TRANSFER_COMPLETED"
    private String transactionRef;
    private String accountNumber;
    private String targetAccountNumber;
    private BigDecimal amount;
    private String type;
    private BigDecimal balanceAfter;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
