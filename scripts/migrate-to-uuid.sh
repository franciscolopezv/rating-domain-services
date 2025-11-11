#!/bin/bash

# Migration script to convert product_id from VARCHAR to UUID
# This script drops and recreates tables - ALL DATA WILL BE LOST

set -e

# Configuration
POSTGRES_CONTAINER="postgres"
RATINGS_USER="ratings_user"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔄 Product ID UUID Migration${NC}"
echo "=============================="
echo ""
echo -e "${RED}⚠️  WARNING: This will drop all tables and data!${NC}"
echo -e "${YELLOW}   Make sure you have a backup if needed.${NC}"
echo ""
read -p "Are you sure you want to continue? (yes/NO) " -r
echo
if [[ ! $REPLY =~ ^[Yy][Ee][Ss]$ ]]; then
    echo -e "${RED}Migration cancelled${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}🗑️  Dropping existing tables...${NC}"

# Drop write database tables
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -c "
DROP TABLE IF EXISTS reviews CASCADE;
DROP FUNCTION IF EXISTS update_updated_at_column() CASCADE;
" 2>/dev/null || true

echo -e "${GREEN}✅ Write database tables dropped${NC}"

# Drop read database tables
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -c "
DROP TABLE IF EXISTS product_stats CASCADE;
DROP FUNCTION IF EXISTS update_last_updated_column() CASCADE;
DROP FUNCTION IF EXISTS recalculate_product_stats(VARCHAR) CASCADE;
DROP FUNCTION IF EXISTS recalculate_product_stats(UUID) CASCADE;
" 2>/dev/null || true

echo -e "${GREEN}✅ Read database tables dropped${NC}"

# Recreate write database schema
echo -e "${YELLOW}📝 Creating write database schema with UUID...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write < scripts/init-write-db.sql

echo -e "${GREEN}✅ Write database schema created${NC}"

# Recreate read database schema
echo -e "${YELLOW}📊 Creating read database schema with UUID...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read < scripts/init-read-db.sql

echo -e "${GREEN}✅ Read database schema created${NC}"

# Verify migration
echo -e "${YELLOW}🔍 Verifying migration...${NC}"

# Check write database
WRITE_TYPE=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -t -c "
SELECT data_type 
FROM information_schema.columns 
WHERE table_name = 'reviews' 
AND column_name = 'product_id';
" | xargs)

if [ "$WRITE_TYPE" = "uuid" ]; then
    echo -e "${GREEN}✅ Write database: product_id is UUID${NC}"
else
    echo -e "${RED}❌ Write database: product_id type is ${WRITE_TYPE} (expected uuid)${NC}"
    exit 1
fi

# Check read database
READ_TYPE=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -t -c "
SELECT data_type 
FROM information_schema.columns 
WHERE table_name = 'product_stats' 
AND column_name = 'product_id';
" | xargs)

if [ "$READ_TYPE" = "uuid" ]; then
    echo -e "${GREEN}✅ Read database: product_id is UUID${NC}"
else
    echo -e "${RED}❌ Read database: product_id type is ${READ_TYPE} (expected uuid)${NC}"
    exit 1
fi

# Check sample data
SAMPLE_COUNT=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -t -c "
SELECT COUNT(*) FROM product_stats;
" | xargs)

echo -e "${GREEN}✅ Sample data loaded: ${SAMPLE_COUNT} products${NC}"

echo ""
echo -e "${GREEN}🎉 Migration completed successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Migration Summary:${NC}"
echo "  • Write database: reviews.product_id → UUID"
echo "  • Read database: product_stats.product_id → UUID"
echo "  • Sample products: ${SAMPLE_COUNT}"
echo ""
echo -e "${YELLOW}ℹ️  Next steps:${NC}"
echo "  1. Rebuild your services:"
echo "     ./mvnw clean package -DskipTests"
echo "     docker-compose up --build -d"
echo ""
echo "  2. Test with a sample UUID:"
echo "     grpcurl -plaintext -d '{\"product_id\": \"550e8400-e29b-41d4-a716-446655440001\", \"rating\": 5}' \\"
echo "       localhost:9090 com.ratings.RatingsCommandService/SubmitRating"
echo ""
