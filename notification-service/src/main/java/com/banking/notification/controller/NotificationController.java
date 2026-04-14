package com.banking.notification.controller;

import com.banking.notification.model.Notification;
import com.banking.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * NotificationController — read-only REST API for viewing sent notifications.
 *
 * TEACHING POINT — Read Model / CQRS Lite:
 * This controller only has GET endpoints. All writes happen via Kafka consumers.
 * This is a lightweight form of CQRS (Command Query Responsibility Segregation):
 *   Commands (writes) → happen via Kafka events
 *   Queries (reads)   → happen via REST API
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "View notifications sent to customers")
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Operation(summary = "Get all notifications")
    public ResponseEntity<List<Notification>> getAllNotifications() {
        return ResponseEntity.ok(notificationService.getAllNotifications());
    }

    @GetMapping("/account/{accountNumber}")
    @Operation(summary = "Get notifications for a specific account")
    public ResponseEntity<List<Notification>> getByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(notificationService.getNotificationsByAccount(accountNumber));
    }
}
