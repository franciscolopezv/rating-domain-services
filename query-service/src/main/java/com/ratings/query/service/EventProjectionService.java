package com.ratings.query.service;

import com.ratings.query.entity.ProductStats;
import com.ratings.query.exception.DatabaseException;
import com.ratings.query.repository.ProductStatsRepository;
import com.ratings.shared.events.RatingSubmittedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service responsible for projecting rating events into read-optimized data structures.
 * This service processes RatingSubmittedEvent messages and updates product statistics.
 */
@Service
public class EventProjectionService {

    private static final Logger logger = LoggerFactory.getLogger(EventProjectionService.class);

    private final ProductStatsRepository productStatsRepository;
    private final MonitoringService monitoringService;

    @Autowired
    public EventProjectionService(ProductStatsRepository productStatsRepository, MonitoringService monitoringService) {
        this.productStatsRepository = productStatsRepository;
        this.monitoringService = monitoringService;
    }

    /**
     * Projects a RatingSubmittedEvent into the product statistics.
     * This method recalculates and updates the aggregated statistics for the affected product.
     *
     * @param event the rating submitted event
     */
    @Transactional
    public void projectRatingSubmittedEvent(RatingSubmittedEvent event) {
        logger.info("Processing RatingSubmittedEvent for product: {} with rating: {}", 
                   event.getProductId(), event.getRating());

        long startTime = System.currentTimeMillis();

        try {
            // Get or create product statistics
            ProductStats productStats = productStatsRepository.findByProductId(event.getProductId())
                    .orElse(new ProductStats(event.getProductId()));

            // Add the new rating to the statistics
            addRatingToStats(productStats, event.getRating());

            // Save the updated statistics
            ProductStats savedStats = productStatsRepository.save(productStats);

            long processingTime = System.currentTimeMillis() - startTime;
            monitoringService.recordKafkaEvent("RatingSubmittedEvent", processingTime);

            logger.info("Successfully updated product statistics for product: {}. " +
                       "New average: {}, review count: {} (processed in {}ms)", 
                       savedStats.getProductId(), 
                       savedStats.getAverageRating(), 
                       savedStats.getReviewCount(),
                       processingTime);

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            monitoringService.recordKafkaEventError("RatingSubmittedEvent", e.getClass().getSimpleName());
            
            logger.error("Error projecting RatingSubmittedEvent for product: {} with rating: {} (failed after {}ms)", 
                        event.getProductId(), event.getRating(), processingTime, e);
            
            if (e instanceof org.springframework.dao.DataAccessException) {
                throw new DatabaseException("Database error during event projection", e);
            }
            
            throw new EventProjectionException("Failed to project rating event", e);
        }
    }

    /**
     * Adds a new rating to the product statistics and recalculates aggregates.
     *
     * @param productStats the product statistics to update
     * @param rating the new rating to add
     */
    private void addRatingToStats(ProductStats productStats, Integer rating) {
        logger.debug("Adding rating {} to product stats for product: {}", rating, productStats.getProductId());

        // Initialize rating distribution if null
        Map<Integer, Integer> distribution = productStats.getRatingDistribution();
        if (distribution == null) {
            distribution = initializeRatingDistribution();
            productStats.setRatingDistribution(distribution);
        }

        // Update rating distribution
        distribution.put(rating, distribution.getOrDefault(rating, 0) + 1);

        // Update review count
        Integer currentCount = productStats.getReviewCount();
        if (currentCount == null) {
            currentCount = 0;
        }
        productStats.setReviewCount(currentCount + 1);

        // Recalculate average rating
        BigDecimal newAverage = calculateAverageRating(distribution);
        productStats.setAverageRating(newAverage);

        logger.debug("Updated product stats for product: {}. Distribution: {}, Average: {}, Count: {}", 
                    productStats.getProductId(), distribution, newAverage, productStats.getReviewCount());
    }

    /**
     * Calculates the average rating based on the rating distribution.
     *
     * @param ratingDistribution the rating distribution map
     * @return the calculated average rating
     */
    private BigDecimal calculateAverageRating(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return null;
        }

        int totalRatingPoints = 0;
        int totalReviews = 0;

        for (Map.Entry<Integer, Integer> entry : ratingDistribution.entrySet()) {
            int starLevel = entry.getKey();
            int count = entry.getValue();
            totalRatingPoints += starLevel * count;
            totalReviews += count;
        }

        if (totalReviews == 0) {
            return null;
        }

        double average = (double) totalRatingPoints / totalReviews;
        return BigDecimal.valueOf(average).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Initializes a rating distribution map with zero counts for all star levels.
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
     * Recalculates statistics for a specific product by querying all its ratings.
     * This method can be used for data repair or initial population.
     *
     * @param productId the product ID to recalculate statistics for
     */
    @Transactional
    public void recalculateProductStats(String productId) {
        logger.info("Recalculating statistics for product: {}", productId);

        try {
            // This would typically query the write database to get all ratings for the product
            // For now, we'll just ensure the product exists in the stats table
            Optional<ProductStats> existingStats = productStatsRepository.findByProductId(productId);
            
            if (existingStats.isEmpty()) {
                ProductStats newStats = new ProductStats(productId);
                productStatsRepository.save(newStats);
                logger.info("Created empty statistics record for product: {}", productId);
            } else {
                logger.info("Statistics already exist for product: {}", productId);
            }

        } catch (Exception e) {
            logger.error("Error recalculating statistics for product: {}", productId, e);
            throw new EventProjectionException("Failed to recalculate product statistics", e);
        }
    }

    /**
     * Custom exception for event projection errors.
     */
    public static class EventProjectionException extends RuntimeException {
        public EventProjectionException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}