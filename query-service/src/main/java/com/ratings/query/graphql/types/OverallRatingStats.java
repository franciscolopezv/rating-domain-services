package com.ratings.query.graphql.types;

import java.math.BigDecimal;

/**
 * GraphQL type for overall system rating statistics.
 */
public class OverallRatingStats {
    
    private Integer totalProducts;
    private Long totalReviews;
    private BigDecimal overallAverageRating;
    private Long productsWithRatings;
    
    public OverallRatingStats() {
    }
    
    public OverallRatingStats(Integer totalProducts, Long totalReviews, 
                             BigDecimal overallAverageRating, Long productsWithRatings) {
        this.totalProducts = totalProducts;
        this.totalReviews = totalReviews;
        this.overallAverageRating = overallAverageRating;
        this.productsWithRatings = productsWithRatings;
    }
    
    public Integer getTotalProducts() {
        return totalProducts;
    }
    
    public void setTotalProducts(Integer totalProducts) {
        this.totalProducts = totalProducts;
    }
    
    public Long getTotalReviews() {
        return totalReviews;
    }
    
    public void setTotalReviews(Long totalReviews) {
        this.totalReviews = totalReviews;
    }
    
    public BigDecimal getOverallAverageRating() {
        return overallAverageRating;
    }
    
    public void setOverallAverageRating(BigDecimal overallAverageRating) {
        this.overallAverageRating = overallAverageRating;
    }
    
    public Long getProductsWithRatings() {
        return productsWithRatings;
    }
    
    public void setProductsWithRatings(Long productsWithRatings) {
        this.productsWithRatings = productsWithRatings;
    }
}