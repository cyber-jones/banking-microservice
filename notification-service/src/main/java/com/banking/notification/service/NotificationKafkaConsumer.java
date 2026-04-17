package com.banking.notification.service;

import com.banking.notification.event.Events;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * NotificationKafkaConsumer — listens to Kafka topics and delegates to NotificationService.
 *
 * TEACHING POINT — @KafkaListener Deep Dive:
 *
 * @KafkaListener(topics = "account-events", groupId = "notification-group")
 *
 *   topics    — the Kafka topic(s) to subscribe to
 *   groupId   — consumer group ID
 *
 * CONSUMER GROUPS explained:
 *   - All consumers with the same groupId share the topic partitions
 *   - Each message is delivered to only ONE consumer in the group
 *   - Run 3 instances of notification-service? Each handles 1/3 of the messages
 *   - Different groupId = DIFFERENT logical subscriber = gets ALL messages independently
 *
 * Example:
 *   notification-group → processes events to send emails
 *   analytics-group    → processes SAME events for reporting (different service)
 *   Both groups get every message — they're independent subscribers.
 *
 * @Payload — injects the deserialized message body
 * @Header  — injects Kafka metadata (topic, partition, offset, key)
 *
 * TEACHING POINT — Message Ordering:
 * Kafka guarantees ordering WITHIN a partition.
 * We used accountNumber as the message key → all events for one account
 * go to the same partition → received in order by the consumer.
 *
 * TEACHING POINT — Error Handling:
 * If this method throws an exception, Spring Kafka will retry (configurable).
 * For production: configure a Dead Letter Topic (DLT) for poison messages.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final NotificationService notificationService;

    /**
     * Consumes account lifecycle events (created, closed, frozen).
     */
    @KafkaListener(
            topics = "account-events",
            groupId = "notification-group",
            containerFactory = "accountEventListenerFactory"
    )
    public void consumeAccountEvent(
            @Payload Events.AccountEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received account event | topic={} partition={} offset={} type={}",
                topic, partition, offset, event.getEventType());

        try {
            switch (event.getEventType()) {
                case "ACCOUNT_CREATED" -> notificationService.processAccountCreated(event);
                case "ACCOUNT_CLOSED"  -> notificationService.processAccountClosed(event);
                case "ACCOUNT_FROZEN"  -> notificationService.processAccountEvent(event);
                default -> log.warn("Unhandled account event type: {}", event.getEventType());
            }
        } catch (Exception e) {
            // TEACHING POINT: Log the error but don't re-throw here so the consumer
            // continues processing. In production, publish to a Dead Letter Topic.
            log.error("Failed to process account event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }

    /**
     * Consumes transaction events (deposit, withdrawal, transfer).
     */
    @KafkaListener(
            topics = "transaction-events",
            groupId = "notification-group",
            containerFactory = "transactionEventListenerFactory"
    )
    public void consumeTransactionEvent(
            @Payload Events.TransactionEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received transaction event | topic={} partition={} offset={} type={}",
                topic, partition, offset, event.getEventType());

        try {
            notificationService.processTransactionEvent(event);
        } catch (Exception e) {
            log.error("Failed to process transaction event {}: {}", event.getEventType(), e.getMessage(), e);
        }
    }
}
