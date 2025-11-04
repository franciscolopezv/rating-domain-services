package com.ratings.query.messaging;

import com.ratings.query.service.EventProjectionService;
import com.ratings.query.service.MonitoringService;
import com.ratings.shared.events.RatingSubmittedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingEventConsumerTest {

    @Mock
    private EventProjectionService eventProjectionService;

    @Mock
    private MonitoringService monitoringService;

    @Mock
    private Acknowledgment acknowledgment;

    private RatingEventConsumer ratingEventConsumer;

    @BeforeEach
    void setUp() {
        ratingEventConsumer = new RatingEventConsumer(eventProjectionService, monitoringService);
    }

    @Test
    void handleRatingSubmittedEvent_WhenValidEvent_ProcessesSuccessfully() {
        // Given
        RatingSubmittedEvent event = createValidRatingEvent();

        // When
        ratingEventConsumer.handleRatingSubmittedEvent(event, acknowledgment, 0, 100L);

        // Then
        verify(eventProjectionService).projectRatingSubmittedEvent(event);
        verify(acknowledgment).acknowledge();
    }

    @Test
    void handleRatingSubmittedEvent_WhenInvalidEvent_ThrowsException() {
        // Given
        RatingSubmittedEvent invalidEvent = createInvalidRatingEvent();

        // When & Then
        assertThatThrownBy(() -> 
            ratingEventConsumer.handleRatingSubmittedEvent(invalidEvent, acknowledgment, 0, 100L))
            .isInstanceOf(RatingEventConsumer.EventProcessingException.class);
        
        verify(eventProjectionService, never()).projectRatingSubmittedEvent(any());
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleRatingSubmittedEvent_WhenProjectionFails_ThrowsException() {
        // Given
        RatingSubmittedEvent event = createValidRatingEvent();
        doThrow(new RuntimeException("Projection failed")).when(eventProjectionService).projectRatingSubmittedEvent(event);

        // When & Then
        assertThatThrownBy(() -> 
            ratingEventConsumer.handleRatingSubmittedEvent(event, acknowledgment, 0, 100L))
            .isInstanceOf(RatingEventConsumer.EventProcessingException.class);
        
        verify(acknowledgment, never()).acknowledge();
    }

    @Test
    void handleDeadLetterEvent_LogsFailedEvent() {
        // Given
        RatingSubmittedEvent event = createValidRatingEvent();

        // When
        ratingEventConsumer.handleDeadLetterEvent(event, 0, 100L);

        // Then
        // Should not throw exception and should log the event
        // Verification is mainly through logging, which we can't easily test
        // In a real scenario, you might want to verify interactions with a failure storage system
    }

    private RatingSubmittedEvent createValidRatingEvent() {
        return RatingSubmittedEvent.create(
                "submission-123",
                "product-456",
                4,
                "user-789"
        );
    }

    private RatingSubmittedEvent createInvalidRatingEvent() {
        return new RatingSubmittedEvent(
                null, // Invalid: null eventId
                "RatingSubmittedEvent",
                Instant.now(),
                "submission-123",
                "product-456",
                4,
                "user-789"
        );
    }
}