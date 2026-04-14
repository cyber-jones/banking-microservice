package com.banking.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * NotificationServiceApplication — consumes events from Kafka and logs notifications.
 *
 * TEACHING POINT — Pure Event Consumer:
 * This service has NO FeignClient calls — it only RECEIVES events.
 * It demonstrates the other side of event-driven architecture:
 * the consumer that reacts to domain events without being coupled
 * to the producer (Account or Transaction service).
 *
 * Real-world extension: replace the log statements with:
 *   - Email via Spring Mail (JavaMailSender)
 *   - SMS via Twilio REST API
 *   - Push notification via Firebase
 *   - Webhook to an external system
 */
@SpringBootApplication
@EnableDiscoveryClient
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
