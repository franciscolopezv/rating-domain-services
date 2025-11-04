package com.ratings.query.exception;

/**
 * Exception thrown when event projection operations fail.
 */
public class EventProjectionException extends RuntimeException {

    public EventProjectionException(String message) {
        super(message);
    }

    public EventProjectionException(String message, Throwable cause) {
        super(message, cause);
    }
}