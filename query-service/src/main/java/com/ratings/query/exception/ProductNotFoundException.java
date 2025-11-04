package com.ratings.query.exception;

/**
 * Exception thrown when a requested product is not found in the system.
 */
public class ProductNotFoundException extends RuntimeException {

    private final String productId;

    public ProductNotFoundException(String productId) {
        super("Product not found with ID: " + productId);
        this.productId = productId;
    }

    public ProductNotFoundException(String productId, String message) {
        super(message);
        this.productId = productId;
    }

    public ProductNotFoundException(String productId, String message, Throwable cause) {
        super(message, cause);
        this.productId = productId;
    }

    public String getProductId() {
        return productId;
    }
}