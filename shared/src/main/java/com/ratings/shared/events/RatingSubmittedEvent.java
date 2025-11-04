package com.ratings.shared.events;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Event published when a rating is successfully submitted.
 * This event is consumed by the query service to update product statistics.
 */
public class RatingSubmittedEvent {
    
    @NotBlank
    private final String eventId;
    
    @NotBlank
    private final String eventType;
    
    @NotNull
    private final Instant timestamp;
    
    @NotBlank
    private final String submissionId;
    
    @NotBlank
    private final String productId;
    
    @NotNull
    @Min(1)
    @Max(5)
    private final Integer rating;
    
    private final String userId; // Optional
    
    @JsonCreator
    public RatingSubmittedEvent(
            @JsonProperty("eventId") String eventId,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("timestamp") Instant timestamp,
            @JsonProperty("submissionId") String submissionId,
            @JsonProperty("productId") String productId,
            @JsonProperty("rating") Integer rating,
            @JsonProperty("userId") String userId) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.submissionId = submissionId;
        this.productId = productId;
        this.rating = rating;
        this.userId = userId;
    }
    
    /**
     * Factory method to create a new RatingSubmittedEvent
     */
    public static RatingSubmittedEvent create(String submissionId, String productId, Integer rating, String userId) {
        return new RatingSubmittedEvent(
                UUID.randomUUID().toString(),
                "RatingSubmittedEvent",
                Instant.now(),
                submissionId,
                productId,
                rating,
                userId
        );
    }
    
    @JsonProperty("eventId")
    public String getEventId() {
        return eventId;
    }
    
    @JsonProperty("eventType")
    public String getEventType() {
        return eventType;
    }
    
    @JsonProperty("timestamp")
    public Instant getTimestamp() {
        return timestamp;
    }
    
    @JsonProperty("submissionId")
    public String getSubmissionId() {
        return submissionId;
    }
    
    @JsonProperty("productId")
    public String getProductId() {
        return productId;
    }
    
    @JsonProperty("rating")
    public Integer getRating() {
        return rating;
    }
    
    @JsonProperty("userId")
    public String getUserId() {
        return userId;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingSubmittedEvent that = (RatingSubmittedEvent) o;
        return Objects.equals(eventId, that.eventId) &&
               Objects.equals(eventType, that.eventType) &&
               Objects.equals(timestamp, that.timestamp) &&
               Objects.equals(submissionId, that.submissionId) &&
               Objects.equals(productId, that.productId) &&
               Objects.equals(rating, that.rating) &&
               Objects.equals(userId, that.userId);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(eventId, eventType, timestamp, submissionId, productId, rating, userId);
    }
    
    @Override
    public String toString() {
        return "RatingSubmittedEvent{" +
               "eventId='" + eventId + '\'' +
               ", eventType='" + eventType + '\'' +
               ", timestamp=" + timestamp +
               ", submissionId='" + submissionId + '\'' +
               ", productId='" + productId + '\'' +
               ", rating=" + rating +
               ", userId='" + userId + '\'' +
               '}';
    }
}