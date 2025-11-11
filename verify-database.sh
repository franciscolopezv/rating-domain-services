#!/bin/bash

# Database verification script for Ratings System
# This script verifies that the databases are properly set up

set -e

# Configuration
POSTGRES_CONTAINER="postgres"
RATINGS_USER="ratings_user"
NETWORK="platform-infrastructure"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Ratings System Database Verification${NC}"
echo "======================================="

# Check if PostgreSQL container exists and is running
echo -e "${YELLOW}📋 Checking PostgreSQL container...${NC}"
if ! docker ps --format "table {{.Names}}" | grep -q "^${POSTGRES_CONTAINER}$"; then
    echo -e "${RED}❌ PostgreSQL container '${POSTGRES_CONTAINER}' is not running${NC}"
    exit 1
fi
echo -e "${GREEN}✅ PostgreSQL container is running${NC}"

# Check network
echo -e "${YELLOW}🔗 Checking network...${NC}"
if ! docker network inspect ${NETWORK} >/dev/null 2>&1; then
    echo -e "${RED}❌ Network '${NETWORK}' does not exist${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Network '${NETWORK}' exists${NC}"

# Check if ratings user exists
echo -e "${YELLOW}👤 Checking ratings user...${NC}"
USER_EXISTS=$(docker exec ${POSTGRES_CONTAINER} psql -U postgres -t -c "SELECT 1 FROM pg_roles WHERE rolname='${RATINGS_USER}';" 2>/dev/null | xargs)
if [ "$USER_EXISTS" != "1" ]; then
    echo -e "${RED}❌ User '${RATINGS_USER}' does not exist${NC}"
    exit 1
fi
echo -e "${GREEN}✅ User '${RATINGS_USER}' exists${NC}"

# Check if databases exist
echo -e "${YELLOW}🗄️  Checking databases...${NC}"
WRITE_DB_EXISTS=$(docker exec ${POSTGRES_CONTAINER} psql -U postgres -t -c "SELECT 1 FROM pg_database WHERE datname='ratings_write';" 2>/dev/null | xargs)
READ_DB_EXISTS=$(docker exec ${POSTGRES_CONTAINER} psql -U postgres -t -c "SELECT 1 FROM pg_database WHERE datname='ratings_read';" 2>/dev/null | xargs)

if [ "$WRITE_DB_EXISTS" != "1" ]; then
    echo -e "${RED}❌ Database 'ratings_write' does not exist${NC}"
    exit 1
fi

if [ "$READ_DB_EXISTS" != "1" ]; then
    echo -e "${RED}❌ Database 'ratings_read' does not exist${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Both databases exist${NC}"

# Check write database schema
echo -e "${YELLOW}📝 Checking write database schema...${NC}"
REVIEWS_TABLE=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -t -c "SELECT 1 FROM information_schema.tables WHERE table_name='reviews';" 2>/dev/null | xargs)
if [ "$REVIEWS_TABLE" != "1" ]; then
    echo -e "${RED}❌ Table 'reviews' does not exist in ratings_write database${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Write database schema is correct${NC}"

# Check read database schema
echo -e "${YELLOW}📊 Checking read database schema...${NC}"
STATS_TABLE=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -t -c "SELECT 1 FROM information_schema.tables WHERE table_name='product_stats';" 2>/dev/null | xargs)
if [ "$STATS_TABLE" != "1" ]; then
    echo -e "${RED}❌ Table 'product_stats' does not exist in ratings_read database${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Read database schema is correct${NC}"

# Test connectivity
echo -e "${YELLOW}🔌 Testing database connectivity...${NC}"
if ! docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -c "SELECT 'Connected to write DB' as status;" >/dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to write database${NC}"
    exit 1
fi

if ! docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -c "SELECT 'Connected to read DB' as status;" >/dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to read database${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Database connectivity test passed${NC}"

# Show database info
echo ""
echo -e "${BLUE}📊 Database Information:${NC}"
echo "========================"

echo -e "${YELLOW}Write Database (ratings_write):${NC}"
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -c "
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY tablename;
"

echo -e "${YELLOW}Read Database (ratings_read):${NC}"
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -c "
SELECT 
    schemaname,
    tablename,
    tableowner
FROM pg_tables 
WHERE schemaname = 'public'
ORDER BY tablename;
"

echo ""
echo -e "${GREEN}🎉 Database verification completed successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "  • PostgreSQL container: ${POSTGRES_CONTAINER}"
echo "  • Network: ${NETWORK}"
echo "  • User: ${RATINGS_USER}"
echo "  • Write database: ratings_write (reviews table)"
echo "  • Read database: ratings_read (product_stats table)"
echo ""
echo -e "${GREEN}✅ Your databases are ready for the Ratings System!${NC}"