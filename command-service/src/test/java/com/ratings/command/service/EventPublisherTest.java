package com.ratings.command.service;

import com.ratings.shared.events.RatingSubmittedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublisherTest {
    
    @Mock
    private KafkaTemplate<String, RatingSubmittedEvent> kafkaTemplate;
    
    @Mock
    private SendResult<String, RatingSubmittedEvent> sendResult;
    
    private EventPublisher eventPublisher;
    private final String topicName = "test-rating-events";
    
    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(kafkaTemplate, topicName);
    }
    
    @Test
    void publishRatingSubmittedEvent_ValidEvent_Success() {
        // Arrange
        RatingSubmittedEvent event = RatingSubmittedEvent.create(
            "review-123", "product-456", 5, "user-789");
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topicName), eq("product-456"), eq(event)))
            .thenReturn(future);
        
        // Act
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> result = 
            eventPublisher.publishRatingSubmittedEvent(event);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        
        verify(kafkaTemplate).send(topicName, "product-456", event);
    }
    
    @Test
    void publishRatingSubmittedEvent_UsesProductIdAsPartitionKey() {
        // Arrange
        RatingSubmittedEvent event = RatingSubmittedEvent.create(
            "review-123", "product-456", 5, "user-789");
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(any(String.class), any(String.class), any(RatingSubmittedEvent.class)))
            .thenReturn(future);
        
        // Act
        eventPublisher.publishRatingSubmittedEvent(event);
        
        // Assert
        ArgumentCaptor<String> partitionKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq(topicName), partitionKeyCaptor.capture(), eq(event));
        
        assertEquals("product-456", partitionKeyCaptor.getValue());
    }
    
    @Test
    void publishRatingSubmittedEventSync_ValidEvent_Success() throws Exception {
        // Arrange
        RatingSubmittedEvent event = RatingSubmittedEvent.create(
            "review-123", "product-456", 5, "user-789");
        
        // Mock RecordMetadata
        org.apache.kafka.clients.producer.RecordMetadata recordMetadata = 
            new org.apache.kafka.clients.producer.RecordMetadata(
                new org.apache.kafka.common.TopicPartition(topicName, 0), 
                0, 0, 0, 0, 0);
        
        when(sendResult.getRecordMetadata()).thenReturn(recordMetadata);
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topicName), eq("product-456"), eq(event)))
            .thenReturn(future);
        
        // Act & Assert
        assertDoesNotThrow(() -> {
            eventPublisher.publishRatingSubmittedEventSync(event);
        });
        
        verify(kafkaTemplate).send(topicName, "product-456", event);
    }
    
    @Test
    void publishRatingSubmittedEventSync_KafkaFailure_ThrowsException() {
        // Arrange
        RatingSubmittedEvent event = RatingSubmittedEvent.create(
            "review-123", "product-456", 5, "user-789");
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = new CompletableFuture<>();
        future.completeExceptionally(new RuntimeException("Kafka error"));
        
        when(kafkaTemplate.send(eq(topicName), eq("product-456"), eq(event)))
            .thenReturn(future);
        
        // Act & Assert
        EventPublisher.EventPublishingException exception = assertThrows(
            EventPublisher.EventPublishingException.class,
            () -> eventPublisher.publishRatingSubmittedEventSync(event)
        );
        
        assertEquals("Failed to publish rating submitted event", exception.getMessage());
        assertNotNull(exception.getCause());
    }
    
    @Test
    void publishRatingSubmittedEvent_EventWithNullUserId_Success() {
        // Arrange
        RatingSubmittedEvent event = RatingSubmittedEvent.create(
            "review-123", "product-456", 5, null); // null user ID
        
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> future = 
            CompletableFuture.completedFuture(sendResult);
        
        when(kafkaTemplate.send(eq(topicName), eq("product-456"), eq(event)))
            .thenReturn(future);
        
        // Act
        CompletableFuture<SendResult<String, RatingSubmittedEvent>> result = 
            eventPublisher.publishRatingSubmittedEvent(event);
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isDone());
        assertFalse(result.isCompletedExceptionally());
        
        verify(kafkaTemplate).send(topicName, "product-456", event);
    }
}