package com.ratings.query.entity;

import com.ratings.shared.entities.ProductStatsEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * JPA entity representing aggregated product statistics in the read database.
 * This entity stores pre-calculated rating data for efficient GraphQL queries.
 */
@Entity
@Table(name = "product_stats")
public class ProductStats implements ProductStatsEntity {

    @Id
    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "average_rating", precision = 3, scale = 2)
    private BigDecimal averageRating;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rating_distribution")
    private Map<Integer, Integer> ratingDistribution = new HashMap<>();

    @Column(name = "last_updated")
    private Instant lastUpdated;

    /**
     * Default constructor for JPA.
     */
    public ProductStats() {
        this.lastUpdated = Instant.now();
    }

    /**
     * Constructor for creating new product statistics.
     *
     * @param productId the product ID
     */
    public ProductStats(String productId) {
        this.productId = productId;
        this.reviewCount = 0;
        this.ratingDistribution = initializeRatingDistribution();
        this.lastUpdated = Instant.now();
    }

    /**
     * Constructor with all fields.
     *
     * @param productId the product ID
     * @param averageRating the average rating
     * @param reviewCount the review count
     * @param ratingDistribution the rating distribution
     */
    public ProductStats(String productId, BigDecimal averageRating, Integer reviewCount, Map<Integer, Integer> ratingDistribution) {
        this.productId = productId;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.ratingDistribution = ratingDistribution != null ? ratingDistribution : initializeRatingDistribution();
        this.lastUpdated = Instant.now();
    }

    /**
     * Initializes the rating distribution with zero counts for all star levels.
     *
     * @return initialized rating distribution map
     */
    private Map<Integer, Integer> initializeRatingDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();
        for (int i = 1; i <= 5; i++) {
            distribution.put(i, 0);
        }
        return distribution;
    }

    /**
     * Updates the statistics with a new rating.
     *
     * @param rating the new rating (1-5)
     */
    public void addRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        // Initialize if needed
        if (ratingDistribution == null) {
            ratingDistribution = initializeRatingDistribution();
        }
        if (reviewCount == null) {
            reviewCount = 0;
        }

        // Update distribution
        ratingDistribution.put(rating, ratingDistribution.getOrDefault(rating, 0) + 1);
        
        // Update review count
        reviewCount++;
        
        // Recalculate average rating
        recalculateAverageRating();
        
        // Update timestamp
        this.lastUpdated = Instant.now();
    }

    /**
     * Recalculates the average rating based on the current distribution.
     */
    private void recalculateAverageRating() {
        if (ratingDistribution == null || reviewCount == null || reviewCount == 0) {
            this.averageRating = null;
            return;
        }

        int totalRatingPoints = 0;
        for (Map.Entry<Integer, Integer> entry : ratingDistribution.entrySet()) {
            totalRatingPoints += entry.getKey() * entry.getValue();
        }

        this.averageRating = BigDecimal.valueOf((double) totalRatingPoints / reviewCount)
                .setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Updates the last updated timestamp.
     * Called automatically by JPA before update operations.
     */
    @PreUpdate
    public void updateTimestamp() {
        this.lastUpdated = Instant.now();
    }

    // Getters and setters

    @Override
    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    @Override
    public BigDecimal getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(BigDecimal averageRating) {
        this.averageRating = averageRating;
    }

    @Override
    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    @Override
    public Map<Integer, Integer> getRatingDistribution() {
        return ratingDistribution;
    }

    public void setRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        this.ratingDistribution = ratingDistribution;
    }

    @Override
    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductStats that = (ProductStats) o;
        return Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId);
    }

    @Override
    public String toString() {
        return "ProductStats{" +
                "productId='" + productId + '\'' +
                ", averageRating=" + averageRating +
                ", reviewCount=" + reviewCount +
                ", ratingDistribution=" + ratingDistribution +
                ", lastUpdated=" + lastUpdated +
                '}';
    }
}