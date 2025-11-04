-- Initialize read database schema for integration tests
CREATE TABLE IF NOT EXISTS product_stats (
    product_id VARCHAR(255) PRIMARY KEY,
    average_rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    one_star_count INTEGER DEFAULT 0,
    two_star_count INTEGER DEFAULT 0,
    three_star_count INTEGER DEFAULT 0,
    four_star_count INTEGER DEFAULT 0,
    five_star_count INTEGER DEFAULT 0,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_product_stats_average_rating ON product_stats(average_rating);
CREATE INDEX IF NOT EXISTS idx_product_stats_review_count ON product_stats(review_count);
CREATE INDEX IF NOT EXISTS idx_product_stats_last_updated ON product_stats(last_updated);