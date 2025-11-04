package com.ratings.query.graphql.types;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * GraphQL type for standalone product rating statistics queries.
 */
public class ProductRatingStats {
    
    private String productId;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private RatingDistribution ratingDistribution;
    private Instant lastUpdated;
    
    public ProductRatingStats() {
    }
    
    public ProductRatingStats(String productId, BigDecimal averageRating, Integer reviewCount, 
                             RatingDistribution ratingDistribution, Instant lastUpdated) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.ratingDistribution = ratingDistribution;
        this.lastUpdated = lastUpdated;
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public BigDecimal getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }
    
    public Integer getReviewCount() {
        return reviewCount;
    }
    
    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }
    
    public RatingDistribution getRatingDistribution() {
        return ratingDistribution;
    }
    
    public void setRatingDistribution(RatingDistribution ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
    
    public Instant getLastUpdated() {
        return lastUpdated;
    }
    
    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}