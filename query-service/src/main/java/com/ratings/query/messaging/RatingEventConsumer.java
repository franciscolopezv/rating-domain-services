package com.ratings.query.messaging;

import com.ratings.query.service.EventProjectionService;
import com.ratings.query.service.MonitoringService;
import com.ratings.shared.events.RatingSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.TopicSuffixingStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for rating events.
 * Listens to rating events and triggers event projection to update read models.
 */
@Component
public class RatingEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RatingEventConsumer.class);

    private final EventProjectionService eventProjectionService;
    private final MonitoringService monitoringService;

    @Autowired
    public RatingEventConsumer(EventProjectionService eventProjectionService, MonitoringService monitoringService) {
        this.eventProjectionService = eventProjectionService;
        this.monitoringService = monitoringService;
    }

    /**
     * Consumes RatingSubmittedEvent messages from the rating_events topic.
     * Uses retryable topic pattern for error handling with exponential backoff.
     *
     * @param event the rating submitted event
     * @param acknowledgment the manual acknowledgment
     * @param partition the partition number
     * @param offset the message offset
     */
    @RetryableTopic(
            attempts = "3",
            backoff = @Backoff(delay = 1000, multiplier = 2.0),
            dltStrategy = org.springframework.kafka.retrytopic.DltStrategy.FAIL_ON_ERROR,
            topicSuffixingStrategy = TopicSuffixingStrategy.SUFFIX_WITH_INDEX_VALUE,
            include = {Exception.class}
    )
    @KafkaListener(
            topics = "${app.kafka.topics.rating-events:rating_events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleRatingSubmittedEvent(
            @Payload RatingSubmittedEvent event,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.info("Received RatingSubmittedEvent: eventId={}, productId={}, rating={}, partition={}, offset={}", 
                   event.getEventId(), event.getProductId(), event.getRating(), partition, offset);

        try {
            // Validate event
            validateEvent(event);

            // Project the event to update read models
            eventProjectionService.projectRatingSubmittedEvent(event);

            // Manually acknowledge the message
            acknowledgment.acknowledge();

            logger.info("Successfully processed RatingSubmittedEvent: eventId={}, productId={}", 
                       event.getEventId(), event.getProductId());

        } catch (Exception e) {
            logger.error("Error processing RatingSubmittedEvent: eventId={}, productId={}, rating={}", 
                        event.getEventId(), event.getProductId(), event.getRating(), e);
            
            // Don't acknowledge - let retry mechanism handle it
            throw new EventProcessingException("Failed to process rating event", e);
        }
    }

    /**
     * Handles messages that have exhausted all retry attempts.
     * This is the dead letter topic handler.
     *
     * @param event the failed event
     * @param partition the partition number
     * @param offset the message offset
     */
    @KafkaListener(
            topics = "${app.kafka.dead-letter-topic:rating_events_dlt}",
            groupId = "${spring.kafka.consumer.group-id}-dlt"
    )
    public void handleDeadLetterEvent(
            @Payload RatingSubmittedEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        logger.error("Received event in dead letter topic: eventId={}, productId={}, rating={}, partition={}, offset={}", 
                    event.getEventId(), event.getProductId(), event.getRating(), partition, offset);

        // Here you could:
        // 1. Store the failed event in a database for manual processing
        // 2. Send an alert to monitoring systems
        // 3. Attempt alternative processing logic
        // 4. Log detailed information for debugging

        try {
            // For now, just log the failure details
            logFailedEventDetails(event);
            
            // In a production system, you might want to store this in a failure table
            // or send to a monitoring system
            
        } catch (Exception e) {
            logger.error("Error handling dead letter event: eventId={}", event.getEventId(), e);
        }
    }

    /**
     * Validates the incoming rating event.
     *
     * @param event the event to validate
     * @throws IllegalArgumentException if the event is invalid
     */
    private void validateEvent(RatingSubmittedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }

        if (event.getEventId() == null || event.getEventId().trim().isEmpty()) {
            throw new IllegalArgumentException("Event ID cannot be null or empty");
        }

        if (event.getProductId() == null || event.getProductId().trim().isEmpty()) {
            throw new IllegalArgumentException("Product ID cannot be null or empty");
        }

        if (event.getRating() == null || event.getRating() < 1 || event.getRating() > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        if (event.getTimestamp() == null) {
            throw new IllegalArgumentException("Event timestamp cannot be null");
        }

        logger.debug("Event validation passed for eventId: {}", event.getEventId());
    }

    /**
     * Logs detailed information about a failed event for debugging purposes.
     *
     * @param event the failed event
     */
    private void logFailedEventDetails(RatingSubmittedEvent event) {
        logger.error("Failed event details: " +
                    "eventId={}, " +
                    "eventType={}, " +
                    "timestamp={}, " +
                    "submissionId={}, " +
                    "productId={}, " +
                    "rating={}, " +
                    "userId={}", 
                    event.getEventId(),
                    event.getEventType(),
                    event.getTimestamp(),
                    event.getSubmissionId(),
                    event.getProductId(),
                    event.getRating(),
                    event.getUserId());
    }

    /**
     * Custom exception for event processing errors.
     */
    public static class EventProcessingException extends RuntimeException {
        public EventProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}