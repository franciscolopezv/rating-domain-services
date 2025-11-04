package com.ratings.shared.dto;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;

/**
 * Data Transfer Object for product statistics.
 * Used for transferring aggregated rating data between services and layers.
 */
public class ProductStatsDto {
    
    private String productId;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private Map<Integer, Integer> ratingDistribution;
    
    public ProductStatsDto() {
        // Default constructor for serialization
    }
    
    public ProductStatsDto(String productId, BigDecimal averageRating, Integer reviewCount, Map<Integer, Integer> ratingDistribution) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.ratingDistribution = ratingDistribution;
    }
    
    /**
     * Checks if this product has any ratings.
     * 
     * @return true if the product has ratings, false otherwise
     */
    public boolean hasRatings() {
        return reviewCount != null && reviewCount > 0;
    }
    
    /**
     * Gets the rating for a specific star level.
     * 
     * @param starLevel the star level (1-5)
     * @return the count of ratings for that star level, or 0 if none
     */
    public int getRatingCountForStars(int starLevel) {
        if (ratingDistribution == null) {
            return 0;
        }
        return ratingDistribution.getOrDefault(starLevel, 0);
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
    
    public Map<Integer, Integer> getRatingDistribution() {
        return ratingDistribution;
    }
    
    public void setRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStatsDto that = (ProductStatsDto) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(averageRating, that.averageRating) &&
               Objects.equals(reviewCount, that.reviewCount) &&
               Objects.equals(ratingDistribution, that.ratingDistribution);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productId, averageRating, reviewCount, ratingDistribution);
    }
    
    @Override
    public String toString() {
        return "ProductStatsDto{" +
               "productId='" + productId + '\'' +
               ", averageRating=" + averageRating +
               ", reviewCount=" + reviewCount +
               ", ratingDistribution=" + ratingDistribution +
               '}';
    }
}