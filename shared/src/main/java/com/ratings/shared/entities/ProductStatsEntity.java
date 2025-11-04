package com.ratings.shared.entities;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Interface defining the contract for ProductStats entities across the system.
 * This allows different implementations in query services while maintaining consistency.
 */
public interface ProductStatsEntity {
    
    /**
     * Gets the product ID these statistics are for.
     * 
     * @return the product ID
     */
    String getProductId();
    
    /**
     * Gets the average rating for this product.
     * 
     * @return the average rating, may be null if no ratings exist
     */
    BigDecimal getAverageRating();
    
    /**
     * Gets the total number of reviews for this product.
     * 
     * @return the review count
     */
    Integer getReviewCount();
    
    /**
     * Gets the distribution of ratings by star level.
     * Map keys are rating values (1-5), values are counts.
     * 
     * @return the rating distribution map
     */
    Map<Integer, Integer> getRatingDistribution();
    
    /**
     * Gets the timestamp when these statistics were last updated.
     * 
     * @return the last update timestamp
     */
    Instant getLastUpdated();
}