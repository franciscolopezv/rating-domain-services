-- Create reviews table for storing raw rating data
-- This table is optimized for write operations in the command service

CREATE TABLE reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    user_id VARCHAR(255),
    review_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create indexes for efficient querying
CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_created_at ON reviews(created_at);
CREATE INDEX idx_reviews_user_id ON reviews(user_id) WHERE user_id IS NOT NULL;

-- Create a composite index for product-user lookups (useful for duplicate prevention)
CREATE INDEX idx_reviews_product_user ON reviews(product_id, user_id) WHERE user_id IS NOT NULL;

-- Add comments for documentation
COMMENT ON TABLE reviews IS 'Stores individual product reviews and ratings';
COMMENT ON COLUMN reviews.review_id IS 'Unique identifier for each review';
COMMENT ON COLUMN reviews.product_id IS 'Identifier of the product being reviewed';
COMMENT ON COLUMN reviews.rating IS 'Rating value from 1 to 5 stars';
COMMENT ON COLUMN reviews.user_id IS 'Optional identifier of the user who submitted the review';
COMMENT ON COLUMN reviews.review_text IS 'Optional text content of the review';
COMMENT ON COLUMN reviews.created_at IS 'Timestamp when the review was created';
COMMENT ON COLUMN reviews.updated_at IS 'Timestamp when the review was last updated';