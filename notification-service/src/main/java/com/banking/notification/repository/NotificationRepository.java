package com.banking.notification.repository;

import com.banking.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(String email);
    List<Notification> findByAccountNumberOrderByCreatedAtDesc(String accountNumber);
    List<Notification> findByStatus(Notification.NotificationStatus status);
}
