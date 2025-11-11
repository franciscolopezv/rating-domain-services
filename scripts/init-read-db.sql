-- Initialize read database schema for ratings system
-- This script creates the product_stats table for storing aggregated rating data

-- Create product_stats table
CREATE TABLE IF NOT EXISTS product_stats (
    product_id UUID PRIMARY KEY,
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
    ('550e8400-e29b-41d4-a716-446655440001'::UUID, 4.67, 3, '{"1": 0, "2": 0, "3": 0, "4": 1, "5": 2}'),
    ('550e8400-e29b-41d4-a716-446655440002'::UUID, 3.50, 2, '{"1": 0, "2": 0, "3": 1, "4": 1, "5": 0}'),
    ('550e8400-e29b-41d4-a716-446655440003'::UUID, 2.00, 3, '{"1": 1, "2": 1, "3": 1, "4": 0, "5": 0}')
ON CONFLICT (product_id) DO UPDATE SET
    average_rating = EXCLUDED.average_rating,
    review_count = EXCLUDED.review_count,
    rating_distribution = EXCLUDED.rating_distribution,
    last_updated = CURRENT_TIMESTAMP;

-- Create function to recalculate product statistics
CREATE OR REPLACE FUNCTION recalculate_product_stats(p_product_id UUID)
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

-- Grant permissions
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ratings_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ratings_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ratings_user;