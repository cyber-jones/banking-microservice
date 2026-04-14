package com.banking.notification.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Notification Entity — persists every notification that was sent.
 *
 * TEACHING POINT — Audit Trail:
 * Storing notifications in the DB provides an audit trail:
 *   - When was the customer notified?
 *   - Did the notification succeed or fail?
 *   - Can we retry failed notifications?
 */
@Entity
@Table(name = "notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.SENT;

    @Column(name = "account_number")
    private String accountNumber;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum NotificationType {
        ACCOUNT_CREATED, ACCOUNT_CLOSED,
        DEPOSIT_RECEIVED, WITHDRAWAL_PROCESSED,
        TRANSFER_SENT, TRANSFER_RECEIVED,
        LOW_BALANCE_ALERT
    }

    public enum NotificationStatus {
        SENT, FAILED, PENDING
    }
}
