package com.banking.notification.config;

import com.banking.notification.event.Events;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaConsumerConfig — configures consumer factories for each event type.
 *
 * TEACHING POINT — Why separate factories?
 * Each @KafkaListener deserialises a DIFFERENT Java class:
 *   account-events     → Events.AccountEvent
 *   transaction-events → Events.TransactionEvent
 *
 * We create a separate ConsumerFactory + ListenerContainerFactory for each,
 * so Jackson knows which class to deserialise into.
 *
 * TEACHING POINT — ErrorHandlingDeserializer:
 * Wraps the real deserialiser. If a message can't be deserialised
 * (malformed JSON, missing fields), it sends the error to the error handler
 * instead of crashing the entire consumer. Prevents "poison pill" messages
 * from stopping the consumer.
 *
 * TEACHING POINT — Consumer Config Properties:
 *   AUTO_OFFSET_RESET_CONFIG = "earliest"
 *     → if no committed offset exists, start from the beginning of the topic
 *     → "latest" would skip existing messages and only process new ones
 *
 *   GROUP_ID_CONFIG = "notification-group"
 *     → Kafka tracks which messages this group has processed
 *     → restart the service: picks up from where it left off
 *
 * @EnableKafka — activates @KafkaListener annotation processing
 */
@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // ─────────────────────────────────────────────────────────────
    // Account Event Consumer
    // ─────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Events.AccountEvent> accountEventConsumerFactory() {
        JsonDeserializer<Events.AccountEvent> deserializer =
                new JsonDeserializer<>(Events.AccountEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Events.AccountEvent>
    accountEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Events.AccountEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(accountEventConsumerFactory());
        // TEACHING POINT — Concurrency: how many threads to use for this listener
        factory.setConcurrency(3); // one thread per partition
        return factory;
    }

    // ─────────────────────────────────────────────────────────────
    // Transaction Event Consumer
    // ─────────────────────────────────────────────────────────────

    @Bean
    public ConsumerFactory<String, Events.TransactionEvent> transactionEventConsumerFactory() {
        JsonDeserializer<Events.TransactionEvent> deserializer =
                new JsonDeserializer<>(Events.TransactionEvent.class, false);
        deserializer.addTrustedPackages("*");

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, "notification-group");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Events.TransactionEvent>
    transactionEventListenerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Events.TransactionEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(transactionEventConsumerFactory());
        factory.setConcurrency(3);
        return factory;
    }
}
