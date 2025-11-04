-- Initialize write database schema for ratings system
-- This script creates the reviews table for storing raw rating data

-- Create reviews table
CREATE TABLE IF NOT EXISTS reviews (
    review_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id VARCHAR(255) NOT NULL,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    user_id VARCHAR(255),
    review_text TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_reviews_updated_at 
    BEFORE UPDATE ON reviews 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Insert sample data for development
INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES
    ('product-1', 5, 'user-1', 'Excellent product! Highly recommended.'),
    ('product-1', 4, 'user-2', 'Very good quality, fast delivery.'),
    ('product-1', 5, 'user-3', 'Perfect! Exactly what I needed.'),
    ('product-2', 3, 'user-4', 'Average product, could be better.'),
    ('product-2', 4, 'user-5', 'Good value for money.'),
    ('product-3', 2, 'user-6', 'Not satisfied with the quality.'),
    ('product-3', 1, 'user-7', 'Poor quality, would not recommend.'),
    ('product-3', 3, 'user-8', 'Okay product, nothing special.')
ON CONFLICT DO NOTHING;

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ratings_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ratings_user;