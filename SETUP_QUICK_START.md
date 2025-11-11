# Quick Start Guide - UUID Migration

## Prerequisites
- Docker and Docker Compose running
- PostgreSQL container running in `platform-infrastructure` network
- Java 17+

## Step 1: Migrate Database to UUID

Run the automated migration script:

```bash
./scripts/migrate-to-uuid.sh
```

This will:
- Drop existing tables (⚠️ data will be lost)
- Create new tables with UUID product_id
- Load sample data with 3 test products

## Step 2: Rebuild Services

```bash
# Clean and build all modules
./mvnw clean package -DskipTests

# Rebuild Docker images
docker-compose build

# Start services
docker-compose up -d
```

## Step 3: Verify Services

Check that services are running:

```bash
# Check command service
curl http://localhost:8081/health

# Check query service
curl http://localhost:8082/health
```

## Step 4: Test with UUID

### Test gRPC Command Service

```bash
grpcurl -plaintext -d '{
  "product_id": "550e8400-e29b-41d4-a716-446655440001",
  "rating": 5,
  "user_id": "test-user",
  "review_text": "Great product!"
}' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
```

Expected response:
```json
{
  "submissionId": "<uuid>",
  "status": "SUCCESS",
  "message": "Rating submitted successfully"
}
```

### Test GraphQL Query Service

```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ productRatingStats(productId: \"550e8400-e29b-41d4-a716-446655440001\") { productId averageRating reviewCount } }"
  }'
```

Expected response:
```json
{
  "data": {
    "productRatingStats": {
      "productId": "550e8400-e29b-41d4-a716-446655440001",
      "averageRating": 4.67,
      "reviewCount": 4
    }
  }
}
```

## Sample UUIDs for Testing

Use these UUIDs from the sample data:

- `550e8400-e29b-41d4-a716-446655440001` - Product 1 (avg: 4.67, reviews: 3)
- `550e8400-e29b-41d4-a716-446655440002` - Product 2 (avg: 3.50, reviews: 2)
- `550e8400-e29b-41d4-a716-446655440003` - Product 3 (avg: 2.00, reviews: 3)

Or generate new UUIDs:
```bash
# On macOS/Linux
uuidgen | tr '[:upper:]' '[:lower:]'

# Or use online generator
# https://www.uuidgenerator.net/
```

## Troubleshooting

### Services won't start
```bash
# Check logs
docker-compose logs -f ratings-command-service
docker-compose logs -f ratings-query-service

# Rebuild from scratch
docker-compose down
./mvnw clean install -DskipTests
docker-compose up --build
```

### Database connection issues
```bash
# Verify database exists
docker exec postgres psql -U ratings_user -l

# Check table schemas
docker exec postgres psql -U ratings_user -d ratings_write -c "\d reviews"
docker exec postgres psql -U ratings_user -d ratings_read -c "\d product_stats"
```

### Invalid UUID errors
Make sure you're using valid UUID format:
- Format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Example: `550e8400-e29b-41d4-a716-446655440001`
- All lowercase hexadecimal characters (0-9, a-f)

## Full Setup Script

If you need to set up everything from scratch:

```bash
# 1. Ensure PostgreSQL is running
docker ps | grep postgres

# 2. Run database setup (will prompt for confirmation)
./setup-database.sh

# 3. Build and start services
./mvnw clean package -DskipTests
docker-compose up --build -d

# 4. Test endpoints
./test-endpoints.sh
```

## Next Steps

- See [README.md](README.md) for detailed API documentation
- See [MIGRATION_UUID.md](MIGRATION_UUID.md) for migration details
- See [QUICK_REFERENCE.md](QUICK_REFERENCE.md) for common commands
