package com.ratings.command.entity;

import com.ratings.shared.entities.ReviewEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity representing a product review in the command service write database.
 * Implements the shared ReviewEntity interface for consistency across services.
 */
@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_reviews_product_id", columnList = "product_id"),
    @Index(name = "idx_reviews_created_at", columnList = "created_at")
})
public class Review implements ReviewEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "review_id")
    private UUID reviewId;
    
    @NotNull
    @Column(name = "product_id", nullable = false)
    private String productId;
    
    @NotNull
    @Min(1)
    @Max(5)
    @Column(name = "rating", nullable = false)
    private Integer rating;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "review_text", columnDefinition = "TEXT")
    private String reviewText;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // Default constructor for JPA
    public Review() {
        // UUID will be generated automatically by JPA
    }
    
    // Constructor for creating new reviews
    public Review(String productId, Integer rating, String userId, String reviewText) {
        this();
        this.productId = productId;
        this.rating = rating;
        this.userId = userId;
        this.reviewText = reviewText;
    }
    
    @Override
    public String getReviewId() {
        return reviewId != null ? reviewId.toString() : null;
    }
    
    public void setReviewId(String reviewId) {
        this.reviewId = reviewId != null ? UUID.fromString(reviewId) : null;
    }
    
    public UUID getReviewIdAsUUID() {
        return reviewId;
    }
    
    public void setReviewIdAsUUID(UUID reviewId) {
        this.reviewId = reviewId;
    }
    
    @Override
    public String getProductId() {
        return productId;
    }
    
    public void setProductId(String productId) {
        this.productId = productId;
    }
    
    @Override
    public Integer getRating() {
        return rating;
    }
    
    public void setRating(Integer rating) {
        this.rating = rating;
    }
    
    @Override
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    @Override
    public String getReviewText() {
        return reviewText;
    }
    
    public void setReviewText(String reviewText) {
        this.reviewText = reviewText;
    }
    
    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}