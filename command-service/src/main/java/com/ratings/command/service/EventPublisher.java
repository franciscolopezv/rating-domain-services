package com.ratings.command.service;

import com.ratings.shared.constants.RatingConstants;
import com.ratings.shared.events.RatingSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Service responsible for publishing events to Kafka.
 * Handles the asynchronous publishing of rating events with proper error handling.
 */
@Service
public class EventPublisher {
    
    private static final Logger logger = LoggerFactory.getLogger(EventPublisher.class);
    
    private final KafkaTemplate<String, RatingSubmittedEvent> kafkaTemplate;
    private final String ratingEventsTopic;
    
    @Autowired
    public EventPublisher(
            KafkaTemplate<String, RatingSubmittedEvent> kafkaTemplate,
            @Value("${ratings.kafka.topic.rating-events:" + RatingConstants.RATINGS_EVENTS_TOPIC + "}") String ratingEventsTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.ratingEventsTopic = ratingEventsTopic;
    }
    
    /**
     * Publishes a rating submitted event to Kafka.
     * 
     * @param event the rating submitted event to publish
     * @return CompletableFuture that completes when the event is published
     */
    public CompletableFuture<SendResult<String, RatingSubmittedEvent>> publishRatingSubmittedEvent(RatingSubmittedEvent event) {
        logger.info("Publishing rating submitted event: eventId={}, productId={}, rating={}", 
                   event.getEventId(), event.getProductId(), event.getRating());
        
        // Use product ID as the partition key to ensure events for the same product go to the same partition
        String partitionKey = event.getProductId();
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = 
            kafkaTemplate.send(ratingEventsTopic, partitionKey, event);
        
        // Add success and failure callbacks
        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.error("Failed to publish rating submitted event: eventId={}, productId={}, error={}", 
                           event.getEventId(), event.getProductId(), throwable.getMessage(), throwable);
            } else {
                logger.info("Successfully published rating submitted event: eventId={}, productId={}, partition={}, offset={}", 
                           event.getEventId(), event.getProductId(), 
                           result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
        
        return future;
    }
    
    /**
     * Publishes a rating submitted event synchronously.
     * This method blocks until the event is published or an error occurs.
     * Use this method when you need to ensure the event is published before proceeding.
     * 
     * @param event the rating submitted event to publish
     * @throws EventPublishingException if the event cannot be published
     */
    public void publishRatingSubmittedEventSync(RatingSubmittedEvent event) throws EventPublishingException {
        try {
            logger.info("Publishing rating submitted event synchronously: eventId={}, productId={}", 
                       event.getEventId(), event.getProductId());
            
            String partitionKey = event.getProductId();
            SendResult<String, RatingSubmittedEvent> result = kafkaTemplate.send(ratingEventsTopic, partitionKey, event).get();
            
            logger.info("Successfully published rating submitted event synchronously: eventId={}, productId={}, partition={}, offset={}", 
                       event.getEventId(), event.getProductId(), 
                       result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            
        } catch (Exception e) {
            logger.error("Failed to publish rating submitted event synchronously: eventId={}, productId={}", 
                        event.getEventId(), event.getProductId(), e);
            throw new EventPublishingException("Failed to publish rating submitted event", e);
        }
    }
    
    /**
     * Exception thrown when event publishing fails.
     */
    public static class EventPublishingException extends Exception {
        public EventPublishingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}