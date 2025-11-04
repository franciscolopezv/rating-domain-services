package com.ratings.command.exception;

/**
 * Exception thrown when validation fails.
 */
public class ValidationException extends RuntimeException {
    
    private final String errorCode;
    
    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public ValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "VALIDATION_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}