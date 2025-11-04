package com.ratings.query.config;

import com.ratings.shared.events.RatingSubmittedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for the query service.
 * Configures consumers for rating events with proper error handling and deserialization.
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${spring.kafka.listener.concurrency:3}")
    private Integer concurrency;

    @Value("${spring.kafka.listener.poll-timeout:3000}")
    private Long pollTimeout;

    /**
     * Creates the consumer factory for rating events.
     *
     * @return the consumer factory
     */
    @Bean
    public ConsumerFactory<String, RatingSubmittedEvent> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic Kafka consumer configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        
        // Deserialization configuration
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // JSON deserializer configuration
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ratings.shared.events");
        configProps.put(JsonDeserializer.TYPE_MAPPINGS, "RatingSubmittedEvent:com.ratings.shared.events.RatingSubmittedEvent");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RatingSubmittedEvent.class.getName());
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        // Consumer behavior configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual acknowledgment
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutes
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 30000); // 30 seconds
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 10000); // 10 seconds
        
        // Retry and error handling
        configProps.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.RECONNECT_BACKOFF_MS_CONFIG, 1000);
        configProps.put(ConsumerConfig.RECONNECT_BACKOFF_MAX_MS_CONFIG, 10000);

        logger.info("Configured Kafka consumer with bootstrap servers: {}, group ID: {}", 
                   bootstrapServers, groupId);

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Creates the Kafka listener container factory.
     *
     * @return the listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RatingSubmittedEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RatingSubmittedEvent> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(concurrency);
        
        // Configure container properties
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProperties.setPollTimeout(pollTimeout);
        containerProperties.setMissingTopicsFatal(false);
        
        // Error handling
        factory.setCommonErrorHandler(new org.springframework.kafka.listener.DefaultErrorHandler(
                (consumerRecord, exception) -> {
                    logger.error("Error processing message: topic={}, partition={}, offset={}, key={}", 
                               consumerRecord.topic(), 
                               consumerRecord.partition(), 
                               consumerRecord.offset(), 
                               consumerRecord.key(), 
                               exception);
                }
        ));

        logger.info("Configured Kafka listener container factory with concurrency: {}, poll timeout: {}ms", 
                   concurrency, pollTimeout);

        return factory;
    }

    /**
     * Creates a separate consumer factory for dead letter topic handling.
     *
     * @return the DLT consumer factory
     */
    @Bean
    public ConsumerFactory<String, RatingSubmittedEvent> dltConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Basic Kafka consumer configuration
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId + "-dlt");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Deserialization configuration (same as main consumer)
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // JSON deserializer configuration
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "com.ratings.shared.events");
        configProps.put(JsonDeserializer.TYPE_MAPPINGS, "RatingSubmittedEvent:com.ratings.shared.events.RatingSubmittedEvent");
        configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, RatingSubmittedEvent.class.getName());
        configProps.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        
        // DLT specific configuration
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true); // Auto-commit for DLT
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10); // Process fewer records at once
        
        logger.info("Configured DLT Kafka consumer with group ID: {}", groupId + "-dlt");

        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Creates the Kafka listener container factory for dead letter topic.
     *
     * @return the DLT listener container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RatingSubmittedEvent> dltKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RatingSubmittedEvent> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(dltConsumerFactory());
        factory.setConcurrency(1); // Single thread for DLT processing
        
        // Configure container properties for DLT
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.BATCH);
        containerProperties.setPollTimeout(5000L); // Longer poll timeout for DLT
        containerProperties.setMissingTopicsFatal(false);

        logger.info("Configured DLT Kafka listener container factory");

        return factory;
    }
}