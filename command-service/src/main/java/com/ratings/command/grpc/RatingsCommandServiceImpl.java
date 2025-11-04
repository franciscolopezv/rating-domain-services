package com.ratings.command.grpc;

import com.ratings.command.entity.Review;
import com.ratings.command.exception.DatabaseException;
import com.ratings.command.exception.ValidationException;
import com.ratings.command.repository.ReviewRepository;
import com.ratings.command.security.SecurityContext;
import com.ratings.command.service.EventPublisher;
import com.ratings.shared.constants.RatingConstants;
import com.ratings.shared.events.RatingSubmittedEvent;
import com.ratings.shared.validation.RatingValidationUtils;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.annotation.Transactional;

/**
 * gRPC service implementation for ratings command operations.
 * Handles rating submissions with proper validation and error handling.
 */
@GrpcService
public class RatingsCommandServiceImpl extends RatingsCommandServiceGrpc.RatingsCommandServiceImplBase {
    
    private static final Logger logger = LoggerFactory.getLogger(RatingsCommandServiceImpl.class);
    
    private final ReviewRepository reviewRepository;
    private final EventPublisher eventPublisher;
    private final Counter submissionCounter;
    private final Timer submissionTimer;
    private final Counter eventPublishingCounter;
    private final Counter eventPublishingErrorCounter;
    private final MeterRegistry meterRegistry;
    
    @Autowired
    public RatingsCommandServiceImpl(
            ReviewRepository reviewRepository, 
            EventPublisher eventPublisher,
            @Qualifier("ratingSubmissionCounter") Counter ratingSubmissionCounter,
            Timer ratingSubmissionTimer,
            @Qualifier("eventPublishingCounter") Counter eventPublishingCounter,
            @Qualifier("eventPublishingErrorCounter") Counter eventPublishingErrorCounter,
            MeterRegistry meterRegistry) {
        this.reviewRepository = reviewRepository;
        this.eventPublisher = eventPublisher;
        this.submissionCounter = ratingSubmissionCounter;
        this.submissionTimer = ratingSubmissionTimer;
        this.eventPublishingCounter = eventPublishingCounter;
        this.eventPublishingErrorCounter = eventPublishingErrorCounter;
        this.meterRegistry = meterRegistry;
    }
    
    @Override
    @Transactional
    public void submitRating(SubmitRatingCommand request, StreamObserver<SubmitRatingResponse> responseObserver) {
        Timer.Sample sample = Timer.start();
        logger.info("Received rating submission request for product: {}", request.getProductId());
        
        try {
            // Validate the request
            validateRequest(request);
            
            // Create and save the review
            Review review = new Review(
                request.getProductId().trim(),
                request.getRating(),
                !request.getUserId().isEmpty() ? request.getUserId().trim() : null,
                !request.getReviewText().isEmpty() ? RatingValidationUtils.sanitizeReviewText(request.getReviewText()) : null
            );
            
            Review savedReview;
            try {
                savedReview = reviewRepository.save(review);
                logger.info("Successfully saved review with ID: {}", savedReview.getReviewId());
            } catch (DataAccessException e) {
                logger.error("Database error while saving review", e);
                Counter.builder("ratings.submissions.errors.total")
                    .tag("error_type", "database")
                    .register(meterRegistry)
                    .increment();
                throw new DatabaseException("Failed to save review to database", e);
            }
            
            // Publish event to Kafka
            try {
                RatingSubmittedEvent event = RatingSubmittedEvent.create(
                    savedReview.getReviewId(),
                    savedReview.getProductId(),
                    savedReview.getRating(),
                    savedReview.getUserId()
                );
                
                eventPublisher.publishRatingSubmittedEventSync(event);
                eventPublishingCounter.increment();
                logger.info("Successfully published rating submitted event for review: {}", savedReview.getReviewId());
                
            } catch (EventPublisher.EventPublishingException e) {
                logger.error("Failed to publish rating submitted event for review: {}", savedReview.getReviewId(), e);
                eventPublishingErrorCounter.increment();
                // Note: In a production system, you might want to implement compensation logic here
                // For now, we'll still return success since the review was saved to the database
                // The event can be republished later through a retry mechanism
            }
            
            // Return success response
            submissionCounter.increment();
            sample.stop(submissionTimer);
            
            SubmitRatingResponse response = SubmitRatingResponse.newBuilder()
                .setSubmissionId(savedReview.getReviewId())
                .setStatus(RatingConstants.SUCCESS_STATUS)
                .setMessage("Rating submitted successfully")
                .build();
            
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            
        } catch (DatabaseException e) {
            logger.error("Database error: {}", e.getMessage(), e);
            Counter.builder("ratings.submissions.errors.total")
                .tag("error_type", "database")
                .register(meterRegistry)
                .increment();
            sample.stop(submissionTimer);
            throw e; // Will be handled by GlobalExceptionHandler
            
        } catch (Exception e) {
            logger.error("Unexpected error while processing rating submission", e);
            Counter.builder("ratings.submissions.errors.total")
                .tag("error_type", "internal")
                .register(meterRegistry)
                .increment();
            sample.stop(submissionTimer);
            throw e; // Will be handled by GlobalExceptionHandler
        }
    }
    
    /**
     * Validates the rating submission request.
     */
    private ValidationResult validateRequest(SubmitRatingCommand request) {
        // Validate product ID
        if (!RatingValidationUtils.isValidProductId(request.getProductId())) {
            throw new ValidationException(RatingConstants.INVALID_PRODUCT_ID_MESSAGE);
        }
        
        // Validate rating value
        if (!RatingValidationUtils.isValidRating(request.getRating())) {
            throw new ValidationException(RatingConstants.INVALID_RATING_MESSAGE);
        }
        
        // Validate user ID if provided
        if (!request.getUserId().isEmpty() && !RatingValidationUtils.isValidUserId(request.getUserId())) {
            throw new ValidationException(RatingConstants.INVALID_USER_ID_MESSAGE);
        }
        
        // Validate review text if provided
        if (!request.getReviewText().isEmpty() && !RatingValidationUtils.isValidReviewText(request.getReviewText())) {
            throw new ValidationException(RatingConstants.INVALID_REVIEW_TEXT_MESSAGE);
        }
        
        return ValidationResult.valid();
    }
    
    /**
     * Helper class for validation results.
     */
    private static class ValidationResult {
        public static ValidationResult valid() {
            return new ValidationResult();
        }
    }
}