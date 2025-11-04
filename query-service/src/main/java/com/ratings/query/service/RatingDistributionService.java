package com.ratings.query.service;

import com.ratings.query.graphql.types.RatingDistribution;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Service for rating distribution calculations and analysis.
 * Provides utility methods for working with rating distributions.
 */
@Service
public class RatingDistributionService {

    /**
     * Calculates the percentage distribution of ratings.
     *
     * @param ratingDistribution the raw rating counts
     * @return a map of star levels to percentages
     */
    public Map<Integer, BigDecimal> calculatePercentageDistribution(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return Map.of(1, BigDecimal.ZERO, 2, BigDecimal.ZERO, 3, BigDecimal.ZERO, 4, BigDecimal.ZERO, 5, BigDecimal.ZERO);
        }

        int totalReviews = ratingDistribution.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalReviews == 0) {
            return Map.of(1, BigDecimal.ZERO, 2, BigDecimal.ZERO, 3, BigDecimal.ZERO, 4, BigDecimal.ZERO, 5, BigDecimal.ZERO);
        }

        return Map.of(
            1, calculatePercentage(ratingDistribution.getOrDefault(1, 0), totalReviews),
            2, calculatePercentage(ratingDistribution.getOrDefault(2, 0), totalReviews),
            3, calculatePercentage(ratingDistribution.getOrDefault(3, 0), totalReviews),
            4, calculatePercentage(ratingDistribution.getOrDefault(4, 0), totalReviews),
            5, calculatePercentage(ratingDistribution.getOrDefault(5, 0), totalReviews)
        );
    }

    /**
     * Calculates the most common rating (mode) from the distribution.
     *
     * @param ratingDistribution the rating distribution
     * @return the most common rating, or null if no ratings exist
     */
    public Integer getMostCommonRating(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return null;
        }

        return ratingDistribution.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .max(Map.Entry.<Integer, Integer>comparingByValue()
                        .thenComparing(Map.Entry::getKey)) // Prefer higher rating in case of tie
                .map(Map.Entry::getKey)
                .orElse(null);
    }

    /**
     * Calculates the rating diversity score (how spread out the ratings are).
     * Returns a value between 0 (all ratings are the same) and 1 (perfectly distributed).
     *
     * @param ratingDistribution the rating distribution
     * @return the diversity score
     */
    public BigDecimal calculateDiversityScore(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return BigDecimal.ZERO;
        }

        int totalReviews = ratingDistribution.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalReviews == 0) {
            return BigDecimal.ZERO;
        }

        // Calculate entropy-based diversity score
        double entropy = 0.0;
        for (int count : ratingDistribution.values()) {
            if (count > 0) {
                double probability = (double) count / totalReviews;
                entropy -= probability * Math.log(probability) / Math.log(5); // Normalize by log(5)
            }
        }

        return BigDecimal.valueOf(entropy).setScale(3, RoundingMode.HALF_UP);
    }

    /**
     * Determines if the product has predominantly positive ratings (4-5 stars).
     *
     * @param ratingDistribution the rating distribution
     * @return true if more than 60% of ratings are 4-5 stars
     */
    public boolean hasPositiveRatings(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return false;
        }

        int totalReviews = ratingDistribution.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalReviews == 0) {
            return false;
        }

        int positiveRatings = ratingDistribution.getOrDefault(4, 0) + ratingDistribution.getOrDefault(5, 0);
        double positivePercentage = (double) positiveRatings / totalReviews;
        
        return positivePercentage > 0.6;
    }

    /**
     * Determines if the product has predominantly negative ratings (1-2 stars).
     *
     * @param ratingDistribution the rating distribution
     * @return true if more than 40% of ratings are 1-2 stars
     */
    public boolean hasNegativeRatings(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null || ratingDistribution.isEmpty()) {
            return false;
        }

        int totalReviews = ratingDistribution.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalReviews == 0) {
            return false;
        }

        int negativeRatings = ratingDistribution.getOrDefault(1, 0) + ratingDistribution.getOrDefault(2, 0);
        double negativePercentage = (double) negativeRatings / totalReviews;
        
        return negativePercentage > 0.4;
    }

    /**
     * Creates a RatingDistribution GraphQL type with additional calculated fields.
     *
     * @param ratingDistribution the raw rating distribution
     * @return the enhanced RatingDistribution object
     */
    public RatingDistribution createEnhancedRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        RatingDistribution distribution = new RatingDistribution(ratingDistribution);
        
        // Add calculated fields
        distribution.setMostCommonRating(getMostCommonRating(ratingDistribution));
        distribution.setHasPositiveRatings(hasPositiveRatings(ratingDistribution));
        distribution.setHasNegativeRatings(hasNegativeRatings(ratingDistribution));
        distribution.setDiversityScore(calculateDiversityScore(ratingDistribution).doubleValue());
        
        return distribution;
    }

    /**
     * Validates that a rating distribution is properly formatted.
     *
     * @param ratingDistribution the distribution to validate
     * @return true if valid
     */
    public boolean isValidRatingDistribution(Map<Integer, Integer> ratingDistribution) {
        if (ratingDistribution == null) {
            return false;
        }

        // Check that all keys are valid star levels (1-5)
        for (Integer starLevel : ratingDistribution.keySet()) {
            if (starLevel == null || starLevel < 1 || starLevel > 5) {
                return false;
            }
        }

        // Check that all values are non-negative
        for (Integer count : ratingDistribution.values()) {
            if (count == null || count < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * Calculates percentage with proper rounding.
     *
     * @param count the count for this rating
     * @param total the total number of ratings
     * @return the percentage as a BigDecimal
     */
    private BigDecimal calculatePercentage(int count, int total) {
        if (total == 0) {
            return BigDecimal.ZERO;
        }
        
        return BigDecimal.valueOf(count)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}