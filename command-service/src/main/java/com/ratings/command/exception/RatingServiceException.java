package com.ratings.command.exception;

/**
 * Base exception class for rating service errors.
 */
public class RatingServiceException extends Exception {
    
    private final String errorCode;
    
    public RatingServiceException(String message) {
        super(message);
        this.errorCode = "RATING_SERVICE_ERROR";
    }
    
    public RatingServiceException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RATING_SERVICE_ERROR";
    }
    
    public RatingServiceException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
    
    public RatingServiceException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}