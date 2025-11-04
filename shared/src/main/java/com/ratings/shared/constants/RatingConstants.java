package com.ratings.shared.constants;

/**
 * Constants used across the ratings system.
 */
public final class RatingConstants {
    
    private RatingConstants() {
        // Constants class - prevent instantiation
    }
    
    // Rating value constraints
    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    
    // Field length constraints
    public static final int MAX_PRODUCT_ID_LENGTH = 255;
    public static final int MAX_USER_ID_LENGTH = 255;
    public static final int MAX_REVIEW_TEXT_LENGTH = 2000;
    
    // Event types
    public static final String RATING_SUBMITTED_EVENT_TYPE = "RatingSubmittedEvent";
    
    // Kafka topics
    public static final String RATINGS_EVENTS_TOPIC = "ratings_events";
    
    // Database table names
    public static final String REVIEWS_TABLE = "reviews";
    public static final String PRODUCT_STATS_TABLE = "product_stats";
    
    // gRPC status messages
    public static final String SUCCESS_STATUS = "SUCCESS";
    public static final String VALIDATION_ERROR_STATUS = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR_STATUS = "INTERNAL_ERROR";
    
    // Error messages
    public static final String INVALID_RATING_MESSAGE = "Rating must be between " + MIN_RATING + " and " + MAX_RATING;
    public static final String INVALID_PRODUCT_ID_MESSAGE = "Product ID is required and must be alphanumeric";
    public static final String INVALID_USER_ID_MESSAGE = "User ID must be alphanumeric if provided";
    public static final String INVALID_REVIEW_TEXT_MESSAGE = "Review text must not exceed " + MAX_REVIEW_TEXT_LENGTH + " characters";
    
    // Health check endpoints
    public static final String HEALTH_CHECK_PATH = "/health";
    public static final String READINESS_CHECK_PATH = "/ready";
    
    // Metrics
    public static final String RATING_SUBMISSION_COUNTER = "ratings.submissions.total";
    public static final String RATING_SUBMISSION_TIMER = "ratings.submissions.duration";
    public static final String EVENT_PROCESSING_COUNTER = "ratings.events.processed.total";
    public static final String EVENT_PROCESSING_TIMER = "ratings.events.processing.duration";
}