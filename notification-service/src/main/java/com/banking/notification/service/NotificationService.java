package com.banking.notification.service;

import com.banking.notification.event.Events;
import com.banking.notification.model.Notification;
import com.banking.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * NotificationService — builds and persists notifications triggered by domain events.
 *
 * TEACHING POINT — Service Responsibilities:
 * This service has two responsibilities:
 *   1. Process inbound Kafka events → create Notification records
 *   2. Provide query methods for the REST controller
 *
 * The @KafkaListener methods in the separate listener class delegate here,
 * keeping the Kafka-specific code separate from the business logic.
 * This separation makes the service easier to unit-test (no Kafka needed).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    // ─────────────────────────────────────────────────────────────
    // Account Event Handlers
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void processAccountCreated(Events.AccountEvent event) {
        log.info("Processing ACCOUNT_CREATED event for account: {}", event.getAccountNumber());

        String message = String.format(
                "Welcome to Banking App, %s! Your account %s has been created successfully.",
                event.getOwnerName(), event.getAccountNumber());

        Notification notification = Notification.builder()
                .recipientEmail(event.getEmail())
                .subject("Welcome! Your Account is Ready")
                .message(message)
                .type(Notification.NotificationType.ACCOUNT_CREATED)
                .status(Notification.NotificationStatus.SENT)
                .accountNumber(event.getAccountNumber())
                .eventType(event.getEventType())
                .build();

        notificationRepository.save(notification);
        log.info("Notification saved for account creation: {}", event.getAccountNumber());

        // In production: inject JavaMailSender and send a real email here
        simulateSendEmail(event.getEmail(), notification.getSubject(), message);
    }

    @Transactional
    public void processAccountClosed(Events.AccountEvent event) {
        log.info("Processing ACCOUNT_CLOSED event for: {}", event.getAccountNumber());

        String message = String.format(
                "Your account %s has been closed. Thank you for banking with us.",
                event.getAccountNumber());

        Notification notification = Notification.builder()
                .recipientEmail(event.getEmail())
                .subject("Account Closure Confirmation")
                .message(message)
                .type(Notification.NotificationType.ACCOUNT_CLOSED)
                .status(Notification.NotificationStatus.SENT)
                .accountNumber(event.getAccountNumber())
                .eventType(event.getEventType())
                .build();

        notificationRepository.save(notification);
        simulateSendEmail(event.getEmail(), notification.getSubject(), message);
    }


    @Transactional
    public void processAccountEvent(Events.AccountEvent event) {
        log.info("Processing ACCOUNT_FROZEN event for: {}", event.getAccountNumber());

        String message = String.format(
                "Your account %s has been frozen. Thank you for banking with us.",
                event.getAccountNumber());

        Notification notification = Notification.builder()
                .recipientEmail(event.getEmail())
                .subject("Account Freeze Confirmation")
                .message(message)
                .type(Notification.NotificationType.ACCOUNT_FROZEN)
                .status(Notification.NotificationStatus.SENT)
                .accountNumber(event.getAccountNumber())
                .eventType(event.getEventType())
                .build();

        notificationRepository.save(notification);
        simulateSendEmail(event.getEmail(), notification.getSubject(), message);
    }

    // ─────────────────────────────────────────────────────────────
    // Transaction Event Handlers
    // ─────────────────────────────────────────────────────────────

    @Transactional
    public void processTransactionEvent(Events.TransactionEvent event) {
        log.info("Processing {} event for account: {}", event.getEventType(), event.getAccountNumber());

        String subject;
        String message;
        Notification.NotificationType notificationType;

        switch (event.getEventType()) {
            case "DEPOSIT_COMPLETED" -> {
                subject = "Deposit Received — " + event.getTransactionRef();
                message = String.format("A deposit of $%.2f has been credited to account %s. New balance: $%.2f",
                        event.getAmount(), event.getAccountNumber(), event.getBalanceAfter());
                notificationType = Notification.NotificationType.DEPOSIT_RECEIVED;
            }
            case "WITHDRAWAL_COMPLETED" -> {
                subject = "Withdrawal Processed — " + event.getTransactionRef();
                message = String.format("A withdrawal of $%.2f has been debited from account %s. New balance: $%.2f",
                        event.getAmount(), event.getAccountNumber(), event.getBalanceAfter());
                notificationType = Notification.NotificationType.WITHDRAWAL_PROCESSED;
            }
            case "TRANSFER_COMPLETED" -> {
                subject = "Transfer Completed — " + event.getTransactionRef();
                message = String.format("A transfer of $%.2f from account %s to %s has been completed.",
                        event.getAmount(), event.getAccountNumber(), event.getTargetAccountNumber());
                notificationType = Notification.NotificationType.TRANSFER_SENT;
            }
            default -> {
                log.warn("Unknown transaction event type: {}", event.getEventType());
                return;
            }
        }

        // TEACHING POINT — Low balance alert:
        // We can add business logic here that goes beyond simple echo of events.
        // Check balance and send an additional alert if it falls below threshold.
        if (event.getBalanceAfter() != null &&
                event.getBalanceAfter().doubleValue() < 100.00 &&
                !"DEPOSIT_COMPLETED".equals(event.getEventType())) {
            sendLowBalanceAlert(event);
        }

        Notification notification = Notification.builder()
                .recipientEmail(event.getAccountNumber() + "@placeholder.com") // real app: look up email
                .subject(subject)
                .message(message)
                .type(notificationType)
                .status(Notification.NotificationStatus.SENT)
                .accountNumber(event.getAccountNumber())
                .eventType(event.getEventType())
                .build();

        notificationRepository.save(notification);
        log.info("Transaction notification saved: {} for {}", event.getEventType(), event.getAccountNumber());
    }

    private void sendLowBalanceAlert(Events.TransactionEvent event) {
        String alertMessage = String.format(
                "LOW BALANCE ALERT: Your account %s balance is $%.2f — below the $100 threshold.",
                event.getAccountNumber(), event.getBalanceAfter());

        Notification alert = Notification.builder()
                .recipientEmail(event.getAccountNumber() + "@placeholder.com")
                .subject("Low Balance Alert")
                .message(alertMessage)
                .type(Notification.NotificationType.LOW_BALANCE_ALERT)
                .status(Notification.NotificationStatus.SENT)
                .accountNumber(event.getAccountNumber())
                .eventType("LOW_BALANCE_ALERT")
                .build();

        notificationRepository.save(alert);
        log.warn("Low balance alert sent for account: {}", event.getAccountNumber());
    }

    // ─────────────────────────────────────────────────────────────
    // Query Methods
    // ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Notification> getNotificationsByAccount(String accountNumber) {
        return notificationRepository.findByAccountNumberOrderByCreatedAtDesc(accountNumber);
    }

    @Transactional(readOnly = true)
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    /**
     * Simulates email sending — replace with JavaMailSender in production.
     * TEACHING POINT: Using a simulation avoids needing an SMTP server for the demo.
     */
    private void simulateSendEmail(String to, String subject, String body) {
        log.info("📧 [EMAIL SIMULATION] To: {} | Subject: {} | Body: {}", to, subject, body);
    }
}
