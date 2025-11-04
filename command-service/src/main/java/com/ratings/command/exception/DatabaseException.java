package com.ratings.command.exception;

/**
 * Exception thrown when database operations fail.
 */
public class DatabaseException extends RuntimeException {
    
    private final String errorCode;
    
    public DatabaseException(String message) {
        super(message);
        this.errorCode = "DATABASE_ERROR";
    }
    
    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "DATABASE_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}