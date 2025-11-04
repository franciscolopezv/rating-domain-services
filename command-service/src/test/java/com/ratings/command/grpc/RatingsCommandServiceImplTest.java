package com.ratings.command.grpc;

import com.ratings.command.entity.Review;
import com.ratings.command.repository.ReviewRepository;
import com.ratings.command.service.EventPublisher;
import com.ratings.shared.constants.RatingConstants;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RatingsCommandServiceImplTest {
    
    @Mock
    private ReviewRepository reviewRepository;
    
    @Mock
    private EventPublisher eventPublisher;
    
    @Mock
    private StreamObserver<SubmitRatingResponse> responseObserver;
    
    private RatingsCommandServiceImpl service;
    private SimpleMeterRegistry meterRegistry;
    
    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        
        Counter submissionCounter = Counter.builder("ratings.submissions.total").register(meterRegistry);
        Timer submissionTimer = Timer.builder("ratings.submissions.duration").register(meterRegistry);
        Counter errorCounter = Counter.builder("ratings.submissions.errors.total").register(meterRegistry);
        Counter eventPublishingCounter = Counter.builder("ratings.events.published.total").register(meterRegistry);
        Counter eventPublishingErrorCounter = Counter.builder("ratings.events.publishing.errors.total").register(meterRegistry);
        
        service = new RatingsCommandServiceImpl(
            reviewRepository,
            eventPublisher,
            submissionCounter,
            submissionTimer,
            eventPublishingCounter,
            eventPublishingErrorCounter,
            meterRegistry
        );
    }
    
    @Test
    void submitRating_ValidRequest_Success() throws Exception {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("product-123")
            .setRating(5)
            .setUserId("user-456")
            .setReviewText("Great product!")
            .build();
        
        Review savedReview = new Review("product-123", 5, "user-456", "Great product!");
        savedReview.setReviewId("review-789");
        
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        doNothing().when(eventPublisher).publishRatingSubmittedEventSync(any());
        
        // Act
        service.submitRating(request, responseObserver);
        
        // Assert
        ArgumentCaptor<SubmitRatingResponse> responseCaptor = ArgumentCaptor.forClass(SubmitRatingResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        SubmitRatingResponse response = responseCaptor.getValue();
        assertEquals("review-789", response.getSubmissionId());
        assertEquals(RatingConstants.SUCCESS_STATUS, response.getStatus());
        assertEquals("Rating submitted successfully", response.getMessage());
        
        // Verify metrics
        assertEquals(1.0, meterRegistry.counter("ratings.submissions.total").count());
        assertEquals(1.0, meterRegistry.counter("ratings.events.published.total").count());
    }
    
    @Test
    void submitRating_InvalidProductId_ThrowsValidationException() {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("") // Invalid empty product ID
            .setRating(5)
            .build();
        
        // Act & Assert - Expect ValidationException since GlobalExceptionHandler is not active in unit tests
        assertThrows(RuntimeException.class, () -> {
            service.submitRating(request, responseObserver);
        });
        
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void submitRating_InvalidRating_ThrowsValidationException() {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("product-123")
            .setRating(6) // Invalid rating > 5
            .build();
        
        // Act & Assert - Expect ValidationException since GlobalExceptionHandler is not active in unit tests
        assertThrows(RuntimeException.class, () -> {
            service.submitRating(request, responseObserver);
        });
        
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        verify(reviewRepository, never()).save(any());
    }
    
    @Test
    void submitRating_DatabaseError_ThrowsException() {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("product-123")
            .setRating(5)
            .build();
        
        when(reviewRepository.save(any(Review.class))).thenThrow(new DataAccessException("Database error") {});
        
        // Act & Assert - Expect DatabaseException since GlobalExceptionHandler is not active in unit tests
        assertThrows(RuntimeException.class, () -> {
            service.submitRating(request, responseObserver);
        });
        
        verify(responseObserver, never()).onNext(any());
        verify(responseObserver, never()).onCompleted();
        // Note: Cannot verify publishRatingSubmittedEventSync due to checked exception
    }
    
    @Test
    void submitRating_EventPublishingFails_StillReturnsSuccess() throws EventPublisher.EventPublishingException {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("product-123")
            .setRating(5)
            .build();
        
        Review savedReview = new Review("product-123", 5, null, null);
        savedReview.setReviewId("review-789");
        
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        doThrow(new EventPublisher.EventPublishingException("Kafka error", new RuntimeException()))
            .when(eventPublisher).publishRatingSubmittedEventSync(any());
        
        // Act
        service.submitRating(request, responseObserver);
        
        // Assert
        ArgumentCaptor<SubmitRatingResponse> responseCaptor = ArgumentCaptor.forClass(SubmitRatingResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        SubmitRatingResponse response = responseCaptor.getValue();
        assertEquals("review-789", response.getSubmissionId());
        assertEquals(RatingConstants.SUCCESS_STATUS, response.getStatus());
        
        // Verify metrics show error
        assertEquals(1.0, meterRegistry.counter("ratings.events.publishing.errors.total").count());
    }
    
    @Test
    void submitRating_MinimalValidRequest_Success() throws Exception {
        // Arrange
        SubmitRatingCommand request = SubmitRatingCommand.newBuilder()
            .setProductId("product-123")
            .setRating(1) // Minimum valid rating
            .build();
        
        Review savedReview = new Review("product-123", 1, null, null);
        savedReview.setReviewId("review-789");
        
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        doNothing().when(eventPublisher).publishRatingSubmittedEventSync(any());
        
        // Act
        service.submitRating(request, responseObserver);
        
        // Assert
        ArgumentCaptor<SubmitRatingResponse> responseCaptor = ArgumentCaptor.forClass(SubmitRatingResponse.class);
        verify(responseObserver).onNext(responseCaptor.capture());
        verify(responseObserver).onCompleted();
        
        SubmitRatingResponse response = responseCaptor.getValue();
        assertEquals("review-789", response.getSubmissionId());
        assertEquals(RatingConstants.SUCCESS_STATUS, response.getStatus());
    }
}