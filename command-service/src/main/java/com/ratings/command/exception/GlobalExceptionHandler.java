package com.ratings.command.exception;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;

/**
 * Global exception handler for gRPC services.
 * Converts various exceptions to appropriate gRPC status codes.
 */
@GrpcAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    @GrpcExceptionHandler(ValidationException.class)
    public StatusRuntimeException handleValidationException(ValidationException e) {
        logger.warn("Validation error: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .withCause(e)
            .asRuntimeException();
    }
    
    @GrpcExceptionHandler(DatabaseException.class)
    public StatusRuntimeException handleDatabaseException(DatabaseException e) {
        logger.error("Database error: {}", e.getMessage(), e);
        return Status.INTERNAL
            .withDescription("Database operation failed")
            .withCause(e)
            .asRuntimeException();
    }
    
    @GrpcExceptionHandler(DataAccessException.class)
    public StatusRuntimeException handleDataAccessException(DataAccessException e) {
        logger.error("Data access error: {}", e.getMessage(), e);
        return Status.INTERNAL
            .withDescription("Database operation failed")
            .withCause(e)
            .asRuntimeException();
    }
    
    @GrpcExceptionHandler(IllegalArgumentException.class)
    public StatusRuntimeException handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("Invalid argument: {}", e.getMessage());
        return Status.INVALID_ARGUMENT
            .withDescription(e.getMessage())
            .withCause(e)
            .asRuntimeException();
    }
    
    @GrpcExceptionHandler(Exception.class)
    public StatusRuntimeException handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        return Status.INTERNAL
            .withDescription("Internal server error")
            .withCause(e)
            .asRuntimeException();
    }
}