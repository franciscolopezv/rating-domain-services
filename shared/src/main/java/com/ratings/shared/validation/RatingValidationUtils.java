package com.ratings.shared.validation;

import java.util.regex.Pattern;

import com.ratings.shared.constants.RatingConstants;

/**
 * Utility class for common validation logic across the ratings system.
 */
public final class RatingValidationUtils {
    
    private static final Pattern PRODUCT_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    private static final Pattern USER_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$");
    
    private RatingValidationUtils() {
        // Utility class - prevent instantiation
    }
    
    /**
     * Validates that a rating value is within the acceptable range.
     * 
     * @param rating the rating value to validate
     * @return true if the rating is valid (1-5), false otherwise
     */
    public static boolean isValidRating(Integer rating) {
        return rating != null && rating >= RatingConstants.MIN_RATING && rating <= RatingConstants.MAX_RATING;
    }
    
    /**
     * Validates that a product ID follows the expected format.
     * 
     * @param productId the product ID to validate
     * @return true if the product ID is valid, false otherwise
     */
    public static boolean isValidProductId(String productId) {
        return productId != null && 
               !productId.trim().isEmpty() && 
               productId.length() <= RatingConstants.MAX_PRODUCT_ID_LENGTH &&
               PRODUCT_ID_PATTERN.matcher(productId).matches();
    }
    
    /**
     * Validates that a user ID follows the expected format.
     * 
     * @param userId the user ID to validate
     * @return true if the user ID is valid, false otherwise
     */
    public static boolean isValidUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return true; // User ID is optional
        }
        return userId.length() <= RatingConstants.MAX_USER_ID_LENGTH &&
               USER_ID_PATTERN.matcher(userId).matches();
    }
    
    /**
     * Validates that review text is within acceptable length limits.
     * 
     * @param reviewText the review text to validate
     * @return true if the review text is valid, false otherwise
     */
    public static boolean isValidReviewText(String reviewText) {
        if (reviewText == null || reviewText.trim().isEmpty()) {
            return true; // Review text is optional
        }
        return reviewText.length() <= RatingConstants.MAX_REVIEW_TEXT_LENGTH;
    }
    
    /**
     * Sanitizes review text by trimming whitespace and removing potentially harmful content.
     * 
     * @param reviewText the review text to sanitize
     * @return sanitized review text, or null if input was null
     */
    public static String sanitizeReviewText(String reviewText) {
        if (reviewText == null) {
            return null;
        }
        
        String sanitized = reviewText.trim();
        if (sanitized.isEmpty()) {
            return null;
        }
        
        // Basic sanitization - remove control characters
        sanitized = sanitized.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
        
        return sanitized.length() > RatingConstants.MAX_REVIEW_TEXT_LENGTH 
            ? sanitized.substring(0, RatingConstants.MAX_REVIEW_TEXT_LENGTH)
            : sanitized;
    }
}