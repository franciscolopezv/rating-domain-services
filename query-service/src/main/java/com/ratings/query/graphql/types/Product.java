package com.ratings.query.graphql.types;

import java.math.BigDecimal;

/**
 * GraphQL Product type for federation.
 * This represents the Product entity extended with rating information.
 */
public class Product {
    
    private String id;
    private BigDecimal averageRating;
    private Integer reviewCount;
    private RatingDistribution ratingDistribution;
    
    public Product() {
    }
    
    public Product(String id) {
        this.id = id;
    }
    
    public Product(String id, BigDecimal averageRating, Integer reviewCount, RatingDistribution ratingDistribution) {
        this.id = id;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.ratingDistribution = ratingDistribution;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
}