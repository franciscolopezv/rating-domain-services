package com.ratings.shared.entities;

import java.time.Instant;

/**
 * Interface defining the contract for Review entities across the system.
 * This allows different implementations in command and query services while maintaining consistency.
 */
public interface ReviewEntity {
    
    /**
     * Gets the unique identifier for this review.
     * 
     * @return the review ID
     */
    String getReviewId();
    
    /**
     * Gets the product ID this review is for.
     * 
     * @return the product ID
     */
    String getProductId();
    
    /**
     * Gets the rating value (1-5).
     * 
     * @return the rating value
     */
    Integer getRating();
    
    /**
     * Gets the user ID who submitted this review.
     * 
     * @return the user ID, may be null
     */
    String getUserId();
    
    /**
     * Gets the review text content.
     * 
     * @return the review text, may be null
     */
    String getReviewText();
    
    /**
     * Gets the timestamp when this review was created.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Gets the timestamp when this review was last updated.
     * 
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
}