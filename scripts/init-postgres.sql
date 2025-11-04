-- Initialize PostgreSQL with both write and read databases for ratings system
-- This script creates both databases and their schemas

-- Create the write database
CREATE DATABASE ratings_write;
-- Create the read database  
CREATE DATABASE ratings_read;

-- Connect to write database and initialize schema
\c ratings_write;

-- Create reviews table for write operations
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

-- Grant permissions for write database
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ratings_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ratings_user;

-- Connect to read database and initialize schema
\c ratings_read;

-- Create product_stats table for read operations
CREATE TABLE IF NOT EXISTS product_stats (
    product_id VARCHAR(255) PRIMARY KEY,
    average_rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    rating_distribution JSONB,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for performance
CREATE INDEX IF NOT EXISTS idx_product_stats_average_rating ON product_stats(average_rating);
CREATE INDEX IF NOT EXISTS idx_product_stats_review_count ON product_stats(review_count);
CREATE INDEX IF NOT EXISTS idx_product_stats_last_updated ON product_stats(last_updated);

-- Create GIN index for JSONB rating_distribution queries
CREATE INDEX IF NOT EXISTS idx_product_stats_rating_distribution ON product_stats USING GIN (rating_distribution);

-- Create trigger to update last_updated timestamp
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_product_stats_last_updated 
    BEFORE UPDATE ON product_stats 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

-- Insert sample aggregated data for development
INSERT INTO product_stats (product_id, average_rating, review_count, rating_distribution) VALUES
    ('product-1', 4.67, 3, '{"1": 0, "2": 0, "3": 0, "4": 1, "5": 2}'),
    ('product-2', 3.50, 2, '{"1": 0, "2": 0, "3": 1, "4": 1, "5": 0}'),
    ('product-3', 2.00, 3, '{"1": 1, "2": 1, "3": 1, "4": 0, "5": 0}')
ON CONFLICT (product_id) DO UPDATE SET
    average_rating = EXCLUDED.average_rating,
    review_count = EXCLUDED.review_count,
    rating_distribution = EXCLUDED.rating_distribution,
    last_updated = CURRENT_TIMESTAMP;

-- Create function to recalculate product statistics
CREATE OR REPLACE FUNCTION recalculate_product_stats(p_product_id VARCHAR(255))
RETURNS VOID AS $$
DECLARE
    v_avg_rating DECIMAL(3,2);
    v_review_count INTEGER;
    v_distribution JSONB;
BEGIN
    -- This function would be called by the query service
    -- It's included here for reference but actual calculation
    -- will be done by the Java application
    
    -- Calculate average rating (placeholder - actual calculation in Java)
    SELECT 0.0, 0, '{}'::JSONB INTO v_avg_rating, v_review_count, v_distribution;
    
    -- Upsert product statistics
    INSERT INTO product_stats (product_id, average_rating, review_count, rating_distribution)
    VALUES (p_product_id, v_avg_rating, v_review_count, v_distribution)
    ON CONFLICT (product_id) DO UPDATE SET
        average_rating = EXCLUDED.average_rating,
        review_count = EXCLUDED.review_count,
        rating_distribution = EXCLUDED.rating_distribution,
        last_updated = CURRENT_TIMESTAMP;
END;
$$ language 'plpgsql';

-- Grant permissions for read database
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ratings_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ratings_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ratings_user;