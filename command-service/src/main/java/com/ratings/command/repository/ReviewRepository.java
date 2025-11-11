package com.ratings.command.repository;

import com.ratings.command.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Review entity operations in the command service.
 * Provides basic CRUD operations and custom queries for review management.
 */
@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {
    
    /**
     * Find all reviews for a specific product.
     * 
     * @param productId the product ID to search for
     * @return list of reviews for the product
     */
    List<Review> findByProductId(UUID productId);
    
    /**
     * Find all reviews by a specific user.
     * 
     * @param userId the user ID to search for
     * @return list of reviews by the user
     */
    List<Review> findByUserId(String userId);
    
    /**
     * Find reviews for a product within a date range.
     * 
     * @param productId the product ID
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of reviews within the date range
     */
    @Query("SELECT r FROM Review r WHERE r.productId = :productId AND r.createdAt BETWEEN :startDate AND :endDate ORDER BY r.createdAt DESC")
    List<Review> findByProductIdAndCreatedAtBetween(
        @Param("productId") UUID productId,
        @Param("startDate") Instant startDate,
        @Param("endDate") Instant endDate
    );
    
    /**
     * Count total reviews for a specific product.
     * 
     * @param productId the product ID
     * @return count of reviews for the product
     */
    long countByProductId(UUID productId);
    
    /**
     * Find the most recent review for a product.
     * 
     * @param productId the product ID
     * @return the most recent review, if any
     */
    Optional<Review> findFirstByProductIdOrderByCreatedAtDesc(UUID productId);
    
    /**
     * Check if a user has already reviewed a specific product.
     * This can be used to prevent duplicate reviews if business rules require it.
     * 
     * @param productId the product ID
     * @param userId the user ID
     * @return true if the user has already reviewed the product
     */
    boolean existsByProductIdAndUserId(UUID productId, String userId);
}