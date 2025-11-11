# Database Setup Guide

This guide helps you set up the required databases for the Ratings System on an existing PostgreSQL container in the `platform-infrastructure` network.

## Quick Setup (Automated)

Run the automated setup script:

```bash
./setup-database.sh
```

This script will:
- Create the `ratings_user` with appropriate permissions
- Create `ratings_write` and `ratings_read` databases
- Run all necessary migrations using SQL scripts from `scripts/` directory
- Verify the setup

## SQL Scripts

The setup uses the following SQL scripts (located in `scripts/` directory):

- **`create-user.sql`** - Creates the ratings_user with proper permissions
- **`create-databases.sql`** - Shows database creation status
- **`grant-permissions.sql`** - Grants database permissions to ratings_user
- **`init-write-db.sql`** - Creates write database schema (reviews table)
- **`init-read-db.sql`** - Creates read database schema (product_stats table)
- **`init-postgres.sql`** - Complete initialization script (legacy)

## Manual Script Execution

You can run individual SQL scripts using the utility script:

```bash
# Run a SQL script
./run-sql.sh scripts/create-user.sql

# Run script on specific database with specific user
./run-sql.sh scripts/init-write-db.sql ratings_write ratings_user
./run-sql.sh scripts/init-read-db.sql ratings_read ratings_user
```

## Manual Setup

If you prefer to set up manually or need to troubleshoot:

### 1. Connect to PostgreSQL Container

```bash
# Connect as admin user
docker exec -it postgres psql -U postgres

# Or if your container has a different name
docker exec -it <your-postgres-container-name> psql -U postgres
```

### 2. Create User and Databases

```sql
-- Create the ratings user
CREATE USER ratings_user WITH PASSWORD 'ratings_pass';
ALTER USER ratings_user CREATEDB;

-- Create databases
CREATE DATABASE ratings_write;
CREATE DATABASE ratings_read;

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE ratings_write TO ratings_user;
GRANT ALL PRIVILEGES ON DATABASE ratings_read TO ratings_user;

-- Exit admin session
\q
```

### 3. Set Up Write Database Schema

```bash
# Connect to write database
docker exec -it postgres psql -U ratings_user -d ratings_write
```

```sql
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

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_reviews_product_id ON reviews(product_id);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews(created_at);
CREATE INDEX IF NOT EXISTS idx_reviews_user_id ON reviews(user_id) WHERE user_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reviews_rating ON reviews(rating);

-- Create update trigger
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$ language 'plpgsql';

CREATE TRIGGER update_reviews_updated_at 
    BEFORE UPDATE ON reviews 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

\q
```

### 4. Set Up Read Database Schema

```bash
# Connect to read database
docker exec -it postgres psql -U ratings_user -d ratings_read
```

```sql
-- Create product_stats table
CREATE TABLE IF NOT EXISTS product_stats (
    product_id VARCHAR(255) PRIMARY KEY,
    average_rating DECIMAL(3,2),
    review_count INTEGER DEFAULT 0,
    rating_distribution JSONB,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_product_stats_average_rating ON product_stats(average_rating);
CREATE INDEX IF NOT EXISTS idx_product_stats_review_count ON product_stats(review_count);
CREATE INDEX IF NOT EXISTS idx_product_stats_last_updated ON product_stats(last_updated);
CREATE INDEX IF NOT EXISTS idx_product_stats_rating_distribution ON product_stats USING GIN (rating_distribution);

-- Create update trigger
CREATE OR REPLACE FUNCTION update_last_updated_column()
RETURNS TRIGGER AS $
BEGIN
    NEW.last_updated = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$ language 'plpgsql';

CREATE TRIGGER update_product_stats_last_updated 
    BEFORE UPDATE ON product_stats 
    FOR EACH ROW 
    EXECUTE FUNCTION update_last_updated_column();

\q
```

## Verification

### Check Database Setup

```bash
# List databases
docker exec postgres psql -U postgres -c "\l"

# Check write database tables
docker exec postgres psql -U ratings_user -d ratings_write -c "\dt"

# Check read database tables
docker exec postgres psql -U ratings_user -d ratings_read -c "\dt"
```

### Test Connectivity

```bash
# Test write database connection
docker exec postgres psql -U ratings_user -d ratings_write -c "SELECT 'Write DB Connected' as status;"

# Test read database connection
docker exec postgres psql -U ratings_user -d ratings_read -c "SELECT 'Read DB Connected' as status;"
```

## Environment Variables

Make sure your services use these database configurations:

```bash
# Write Database (Command Service)
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ratings_write
DB_USERNAME=ratings_user
DB_PASSWORD=ratings_pass

# Read Database (Query Service)
DB_HOST=postgres
DB_PORT=5432
DB_NAME=ratings_read
DB_USERNAME=ratings_user
DB_PASSWORD=ratings_pass
```

## Troubleshooting

### Connection Issues

```bash
# Check if PostgreSQL container is running
docker ps | grep postgres

# Check if container is in the correct network
docker network inspect platform-infrastructure

# Test network connectivity
docker run --rm --network platform-infrastructure postgres:15-alpine pg_isready -h postgres -p 5432
```

### Permission Issues

```sql
-- Grant additional permissions if needed
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ratings_user;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ratings_user;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ratings_user;
```

### Reset Database (if needed)

```sql
-- Connect as admin user
DROP DATABASE IF EXISTS ratings_write;
DROP DATABASE IF EXISTS ratings_read;
DROP USER IF EXISTS ratings_user;

-- Then re-run the setup
```

## Sample Data (Optional)

To add some test data for development:

```sql
-- Connect to write database
\c ratings_write

-- Insert sample reviews
INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES
    ('P123', 5, 'user-1', 'Excellent product!'),
    ('P123', 4, 'user-2', 'Very good quality'),
    ('P456', 3, 'user-3', 'Average product'),
    ('P456', 2, 'user-4', 'Could be better')
ON CONFLICT DO NOTHING;
```

## Next Steps

After database setup:

1. **Build and start services:**
   ```bash
   ./mvnw clean package -DskipTests
   docker compose build
   docker compose up -d
   ```

2. **Verify services:**
   ```bash
   curl http://localhost:8081/health  # Command Service
   curl http://localhost:8082/health  # Query Service
   ```

3. **Test the system:**
   ```bash
   # Submit a rating via gRPC
   grpcurl -plaintext -d '{"product_id": "P123", "rating": 5, "user_id": "test-user"}' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
   
   # Query via GraphQL
   curl -X POST http://localhost:8082/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ productRatingStats(productId: \"P123\") { averageRating reviewCount } }"}'
   ```