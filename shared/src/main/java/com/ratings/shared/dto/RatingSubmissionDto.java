package com.ratings.shared.dto;

import com.ratings.shared.validation.RatingValidationUtils;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

/**
 * Data Transfer Object for rating submissions.
 * Used for transferring rating data between services and layers.
 */
public class RatingSubmissionDto {
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;
    
    private String userId;
    
    private String reviewText;
    
    public RatingSubmissionDto() {
        // Default constructor for serialization
    }
    
    public RatingSubmissionDto(String productId, Integer rating, String userId, String reviewText) {
        this.productId = productId;
        this.rating = rating;
        this.userId = userId;
        this.reviewText = reviewText;
    }
    
    /**
     * Validates this DTO using the shared validation utilities.
     * 
     * @return true if all fields are valid, false otherwise
     */
    public boolean isValid() {
        return RatingValidationUtils.isValidProductId(productId) &&
               RatingValidationUtils.isValidRating(rating) &&
               RatingValidationUtils.isValidUserId(userId) &&
               RatingValidationUtils.isValidReviewText(reviewText);
    }
    
    /**
     * Sanitizes the review text field.
     */
    public void sanitize() {
        this.reviewText = RatingValidationUtils.sanitizeReviewText(this.reviewText);
    }
    
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getReviewText() {
        return reviewText;
    }
    
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RatingSubmissionDto that = (RatingSubmissionDto) o;
        return Objects.equals(productId, that.productId) &&
               Objects.equals(rating, that.rating) &&
               Objects.equals(userId, that.userId) &&
               Objects.equals(reviewText, that.reviewText);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(productId, rating, userId, reviewText);
    }
    
    @Override
    public String toString() {
        return "RatingSubmissionDto{" +
               "productId='" + productId + '\'' +
               ", rating=" + rating +
               ", userId='" + userId + '\'' +
               ", reviewText='" + (reviewText != null ? reviewText.substring(0, Math.min(50, reviewText.length())) + "..." : null) + '\'' +
               '}';
    }
}