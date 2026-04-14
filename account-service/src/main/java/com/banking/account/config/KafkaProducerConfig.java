package com.banking.account.config;

import com.banking.account.event.AccountEvent;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * KafkaProducerConfig — configures Kafka producer for publishing events.
 *
 * TEACHING POINT — Kafka Core Concepts:
 *
 *  Topic    — a named category/channel for messages (like a database table for events)
 *  Producer — publishes messages to a topic
 *  Consumer — reads messages from a topic
 *  Partition — topics are split into partitions for parallelism and ordering
 *  Offset   — position of a message within a partition (consumers track this)
 *
 *  ProducerFactory  — creates Kafka producer instances with shared config
 *  KafkaTemplate    — Spring's high-level abstraction for sending messages
 *                     (similar to JdbcTemplate for databases)
 *
 *  Serialisation:
 *  - Key:   StringSerializer  (account number as string key)
 *  - Value: JsonSerializer    (AccountEvent object → JSON bytes)
 *
 *  TEACHING POINT — Kafka message keys:
 *  Messages with the SAME key always go to the SAME partition.
 *  We use accountNumber as key so all events for one account are ordered.
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, AccountEvent> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        // Add type info so consumer knows what class to deserialise into
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, true);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, AccountEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Declare the Kafka topic. Spring Kafka auto-creates it if it doesn't exist.
     * partitions(3) = 3 partitions for parallelism
     * replicas(1)   = 1 replica (use 3 in production)
     */
    @Bean
    public NewTopic accountEventsTopic() {
        return TopicBuilder.name("account-events")
                .partitions(3)
                .replicas(1)
                .build();
    }
}
