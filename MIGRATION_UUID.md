# Product ID Migration to UUID

## Overview
This document describes the migration of `product_id` from VARCHAR to UUID type across the ratings system.

## Changes Made

### Database Schema Changes

#### Write Database (scripts/init-write-db.sql)
- Changed `product_id` column type from `VARCHAR(255)` to `UUID`
- Table: `reviews`

#### Read Database (scripts/init-read-db.sql)
- Changed `product_id` column type from `VARCHAR(255)` to `UUID`
- Updated sample data to use UUID format
- Updated `recalculate_product_stats` function parameter from `VARCHAR(255)` to `UUID`
- Table: `product_stats`

### Java Entity Changes

#### Command Service
**Review.java** (`command-service/src/main/java/com/ratings/command/entity/Review.java`)
- Changed `productId` field type from `String` to `UUID`
- Updated constructor to accept `UUID` parameter
- Added `getProductIdAsUUID()` and `setProductIdAsUUID()` methods
- Modified `getProductId()` to return `UUID.toString()`
- Modified `setProductId(String)` to parse string to UUID

**RatingsCommandServiceImpl.java**
- Added UUID parsing with validation before creating Review entity
- Added proper error handling for invalid UUID format

#### Query Service
**ProductStats.java** (`query-service/src/main/java/com/ratings/query/entity/ProductStats.java`)
- Changed `productId` field type from `String` to `UUID`
- Updated constructors to accept `UUID` parameter
- Added `getProductIdAsUUID()` and `setProductIdAsUUID()` methods
- Modified `getProductId()` to return `UUID.toString()`
- Modified `setProductId(String)` to parse string to UUID

**ProductStatsRepository.java**
- Changed JpaRepository generic type from `<ProductStats, String>` to `<ProductStats, UUID>`
- Updated all method signatures to use `UUID` instead of `String`
- Updated native query to cast productId parameter to UUID

**ProductStatsService.java**
- Added UUID parsing in all methods that accept `String productId`
- Added error handling for invalid UUID format
- Methods updated:
  - `getProductStats(String)`
  - `getProductRatingStats(String)`
  - `hasProductStats(String)`
  - `createEmptyProductStats(String)`
  - `saveProductStats(ProductStatsDto)`

**EventProjectionService.java**
- Added UUID parsing when processing `RatingSubmittedEvent`
- Updated `recalculateProductStats(String)` to parse UUID

### Validation Changes

**RatingValidationUtils.java** (`shared/src/main/java/com/ratings/shared/validation/RatingValidationUtils.java`)
- Changed `PRODUCT_ID_PATTERN` to `UUID_PATTERN`
- Updated `isValidProductId()` to validate UUID format using regex pattern
- Pattern: `^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$`

### Documentation Changes

**README.md**
- Updated all example product IDs to use UUID format
- Changed from `product-123` to `550e8400-e29b-41d4-a716-446655440001`
- Updated examples in:
  - gRPC curl commands
  - Python gRPC example
  - GraphQL queries
  - Federation examples

## Sample UUIDs for Testing

The following UUIDs are used in sample data and examples:
- `550e8400-e29b-41d4-a716-446655440001` - Product 1 (avg rating: 4.67)
- `550e8400-e29b-41d4-a716-446655440002` - Product 2 (avg rating: 3.50)
- `550e8400-e29b-41d4-a716-446655440003` - Product 3 (avg rating: 2.00)

## Quick Migration (Recommended)

For a fresh setup or if you're okay with losing existing data:

```bash
# Run the automated migration script
./scripts/migrate-to-uuid.sh
```

This script will:
1. Drop existing tables
2. Recreate them with UUID type
3. Load sample data
4. Verify the migration

## Manual Migration Steps

If you have existing data and need to preserve it, follow these steps:

1. **Backup your databases** before making any changes

2. **Update the write database:**
   ```sql
   -- Add new UUID column
   ALTER TABLE reviews ADD COLUMN product_id_uuid UUID;
   
   -- Migrate data (if product_id was already UUID-formatted strings)
   UPDATE reviews SET product_id_uuid = product_id::UUID;
   
   -- Or generate new UUIDs if needed
   -- UPDATE reviews SET product_id_uuid = gen_random_uuid();
   
   -- Drop old column and rename new one
   ALTER TABLE reviews DROP COLUMN product_id;
   ALTER TABLE reviews RENAME COLUMN product_id_uuid TO product_id;
   ALTER TABLE reviews ALTER COLUMN product_id SET NOT NULL;
   ```

3. **Update the read database:**
   ```sql
   -- Similar process for product_stats table
   ALTER TABLE product_stats ADD COLUMN product_id_uuid UUID;
   UPDATE product_stats SET product_id_uuid = product_id::UUID;
   ALTER TABLE product_stats DROP CONSTRAINT product_stats_pkey;
   ALTER TABLE product_stats DROP COLUMN product_id;
   ALTER TABLE product_stats RENAME COLUMN product_id_uuid TO product_id;
   ALTER TABLE product_stats ADD PRIMARY KEY (product_id);
   ```

4. **Rebuild and redeploy services:**
   ```bash
   ./mvnw clean install
   docker-compose up --build
   ```

## Validation

After migration, verify:

1. **Database schema:**
   ```sql
   \d reviews
   \d product_stats
   ```
   Confirm `product_id` is type `UUID`

2. **Submit a test rating:**
   ```bash
   grpcurl -plaintext -d '{
     "product_id": "550e8400-e29b-41d4-a716-446655440001",
     "rating": 5,
     "user_id": "test-user",
     "review_text": "Test review"
   }' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
   ```

3. **Query the statistics:**
   ```bash
   curl -X POST http://localhost:8082/graphql \
     -H "Content-Type: application/json" \
     -d '{"query": "{ productRatingStats(productId: \"550e8400-e29b-41d4-a716-446655440001\") { productId averageRating reviewCount } }"}'
   ```

## Breaking Changes

⚠️ **This is a breaking change** for any clients using the API:

1. **gRPC clients** must now send product_id as a valid UUID string
2. **GraphQL clients** must use UUID format for productId arguments
3. Invalid UUID formats will be rejected with validation errors

## Rollback Plan

If you need to rollback:

1. Restore database backups
2. Revert code changes using git:
   ```bash
   git revert <commit-hash>
   ```
3. Rebuild and redeploy services
