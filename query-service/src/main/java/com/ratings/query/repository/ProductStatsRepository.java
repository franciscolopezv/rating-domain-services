package com.ratings.query.repository;

import com.ratings.query.entity.ProductStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for ProductStats entity operations.
 * Provides methods for querying and updating aggregated product rating statistics.
 */
@Repository
public interface ProductStatsRepository extends JpaRepository<ProductStats, UUID> {

    /**
     * Finds product statistics by product ID.
     *
     * @param productId the product ID
     * @return optional containing the product statistics if found
     */
    Optional<ProductStats> findByProductId(UUID productId);

    /**
     * Finds all products with ratings above a specified threshold.
     *
     * @param minRating the minimum average rating
     * @return list of product statistics with ratings above the threshold
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.averageRating >= :minRating ORDER BY ps.averageRating DESC")
    List<ProductStats> findByAverageRatingGreaterThanEqual(@Param("minRating") BigDecimal minRating);

    /**
     * Finds all products with review count above a specified threshold.
     *
     * @param minReviewCount the minimum review count
     * @return list of product statistics with review count above the threshold
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.reviewCount >= :minReviewCount ORDER BY ps.reviewCount DESC")
    List<ProductStats> findByReviewCountGreaterThanEqual(@Param("minReviewCount") Integer minReviewCount);

    /**
     * Finds products updated after a specific timestamp.
     *
     * @param timestamp the timestamp to compare against
     * @return list of product statistics updated after the timestamp
     */
    @Query("SELECT ps FROM ProductStats ps WHERE ps.lastUpdated > :timestamp ORDER BY ps.lastUpdated DESC")
    List<ProductStats> findByLastUpdatedAfter(@Param("timestamp") Instant timestamp);

    /**
     * Gets the count of products with ratings.
     *
     * @return the count of products that have at least one rating
     */
    @Query("SELECT COUNT(ps) FROM ProductStats ps WHERE ps.reviewCount > 0")
    Long countProductsWithRatings();

    /**
     * Gets the overall average rating across all products.
     *
     * @return the overall average rating, or null if no ratings exist
     */
    @Query("SELECT AVG(ps.averageRating) FROM ProductStats ps WHERE ps.averageRating IS NOT NULL")
    BigDecimal getOverallAverageRating();

    /**
     * Gets the total number of reviews across all products.
     *
     * @return the total review count
     */
    @Query("SELECT COALESCE(SUM(ps.reviewCount), 0) FROM ProductStats ps")
    Long getTotalReviewCount();

    /**
     * Finds the top-rated products limited by count.
     *
     * @param limit the maximum number of products to return
     * @return list of top-rated products
     */
    @Query(value = "SELECT * FROM product_stats WHERE average_rating IS NOT NULL ORDER BY average_rating DESC, review_count DESC LIMIT :limit", 
           nativeQuery = true)
    List<ProductStats> findTopRatedProducts(@Param("limit") int limit);

    /**
     * Finds the most reviewed products limited by count.
     *
     * @param limit the maximum number of products to return
     * @return list of most reviewed products
     */
    @Query(value = "SELECT * FROM product_stats ORDER BY review_count DESC, average_rating DESC LIMIT :limit", 
           nativeQuery = true)
    List<ProductStats> findMostReviewedProducts(@Param("limit") int limit);

    /**
     * Updates the statistics for a specific product using native SQL for better performance.
     * This method is used by the event projection logic.
     *
     * @param productId the product ID
     * @param averageRating the new average rating
     * @param reviewCount the new review count
     * @param ratingDistribution the rating distribution as JSON
     * @return the number of rows affected
     */
    @Modifying
    @Query(value = """
        INSERT INTO product_stats (product_id, average_rating, review_count, rating_distribution, last_updated)
        VALUES (CAST(:productId AS UUID), :averageRating, :reviewCount, CAST(:ratingDistribution AS jsonb), CURRENT_TIMESTAMP)
        ON CONFLICT (product_id) DO UPDATE SET
            average_rating = EXCLUDED.average_rating,
            review_count = EXCLUDED.review_count,
            rating_distribution = EXCLUDED.rating_distribution,
            last_updated = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    int upsertProductStats(@Param("productId") UUID productId,
                          @Param("averageRating") BigDecimal averageRating,
                          @Param("reviewCount") Integer reviewCount,
                          @Param("ratingDistribution") String ratingDistribution);

    /**
     * Deletes statistics for products that haven't been updated recently.
     * This can be used for cleanup of stale data.
     *
     * @param cutoffTime the cutoff time before which data should be deleted
     * @return the number of rows deleted
     */
    @Modifying
    @Query("DELETE FROM ProductStats ps WHERE ps.lastUpdated < :cutoffTime AND ps.reviewCount = 0")
    int deleteStaleEmptyStats(@Param("cutoffTime") Instant cutoffTime);

    /**
     * Checks if a product has any statistics recorded.
     *
     * @param productId the product ID
     * @return true if statistics exist for the product
     */
    boolean existsByProductId(UUID productId);
}