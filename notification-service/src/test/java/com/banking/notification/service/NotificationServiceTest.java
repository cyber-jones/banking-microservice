package com.banking.notification.service;

import com.banking.notification.event.Events;
import com.banking.notification.model.Notification;
import com.banking.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * NotificationServiceTest — verifies correct notification content per event type.
 *
 * TEACHING POINT — Testing Event-Driven Code:
 * We don't need Kafka running to test the service.
 * We call service methods directly with mock event objects,
 * then use ArgumentCaptor to verify what was saved to the repository.
 *
 * This is the key advantage of separating the @KafkaListener (consumer)
 * from the business logic (service) — the service is purely testable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    @DisplayName("should save welcome notification on ACCOUNT_CREATED event")
    void processAccountCreated_SavesWelcomeNotification() {
        // ARRANGE
        Events.AccountEvent event = Events.AccountEvent.builder()
                .eventType("ACCOUNT_CREATED")
                .accountNumber("ACC0000000001")
                .ownerName("Alice Smith")
                .email("alice@example.com")
                .build();

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        notificationService.processAccountCreated(event);

        // ASSERT — capture what was saved
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getRecipientEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getType()).isEqualTo(Notification.NotificationType.ACCOUNT_CREATED);
        assertThat(saved.getStatus()).isEqualTo(Notification.NotificationStatus.SENT);
        assertThat(saved.getMessage()).contains("Alice Smith");
        assertThat(saved.getMessage()).contains("ACC0000000001");
    }

    @Test
    @DisplayName("should save deposit notification on DEPOSIT_COMPLETED event")
    void processTransactionEvent_Deposit_SavesDepositNotification() {
        // ARRANGE
        Events.TransactionEvent event = Events.TransactionEvent.builder()
                .eventType("DEPOSIT_COMPLETED")
                .transactionRef("TXN-ABC123")
                .accountNumber("ACC0000000001")
                .amount(new BigDecimal("500.00"))
                .balanceAfter(new BigDecimal("5500.00"))
                .build();

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        notificationService.processTransactionEvent(event);

        // ASSERT
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(Notification.NotificationType.DEPOSIT_RECEIVED);
        assertThat(saved.getMessage()).contains("500.00");
        assertThat(saved.getMessage()).contains("ACC0000000001");
    }

    @Test
    @DisplayName("should save both transaction and low-balance notification when balance drops below 100")
    void processTransactionEvent_LowBalance_SavesTwoNotifications() {
        // ARRANGE
        Events.TransactionEvent event = Events.TransactionEvent.builder()
                .eventType("WITHDRAWAL_COMPLETED")
                .transactionRef("TXN-XYZ999")
                .accountNumber("ACC0000000001")
                .amount(new BigDecimal("50.00"))
                .balanceAfter(new BigDecimal("45.00"))  // below $100 threshold
                .build();

        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // ACT
        notificationService.processTransactionEvent(event);

        // ASSERT — two saves: one for withdrawal, one for low balance alert
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    @DisplayName("should handle unknown event type gracefully without throwing")
    void processTransactionEvent_UnknownType_DoesNotThrow() {
        Events.TransactionEvent event = Events.TransactionEvent.builder()
                .eventType("UNKNOWN_EVENT")
                .accountNumber("ACC0000000001")
                .build();

        // Should NOT throw — should just log a warning
        assertThatNoException().isThrownBy(
                () -> notificationService.processTransactionEvent(event)
        );

        verify(notificationRepository, never()).save(any());
    }
}
