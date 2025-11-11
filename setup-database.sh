#!/bin/bash

# Database setup script for Ratings System
# This script creates the required databases and runs migrations on an existing PostgreSQL container

set -e

# Configuration
POSTGRES_CONTAINER="postgres"  # Name of your PostgreSQL container
POSTGRES_USER="postgres"       # PostgreSQL admin user
POSTGRES_PASSWORD=""           # Will be prompted if not set
RATINGS_USER="ratings_user"
RATINGS_PASSWORD="ratings_pass"
NETWORK="platform-infrastructure"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🗄️  Ratings System Database Setup${NC}"
echo "=================================="
echo ""
echo -e "${YELLOW}⚠️  Note: This script will drop and recreate all tables${NC}"
echo -e "${YELLOW}   All existing data will be lost!${NC}"
echo ""
read -p "Continue? (y/N) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo -e "${RED}Setup cancelled${NC}"
    exit 1
fi
echo ""

# Check if PostgreSQL container exists and is running
echo -e "${YELLOW}📋 Checking PostgreSQL container...${NC}"
if ! docker ps --format "table {{.Names}}" | grep -q "^${POSTGRES_CONTAINER}$"; then
    echo -e "${RED}❌ PostgreSQL container '${POSTGRES_CONTAINER}' is not running in the platform-infrastructure network${NC}"
    echo "Please ensure your PostgreSQL container is running and accessible."
    exit 1
fi

echo -e "${GREEN}✅ PostgreSQL container found and running${NC}"

# Check network connectivity
echo -e "${YELLOW}🔗 Checking network connectivity...${NC}"
if ! docker network inspect ${NETWORK} >/dev/null 2>&1; then
    echo -e "${RED}❌ Network '${NETWORK}' does not exist${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Network '${NETWORK}' exists${NC}"

# Prompt for PostgreSQL password if not set
if [ -z "$POSTGRES_PASSWORD" ]; then
    echo -e "${YELLOW}🔐 Please enter the PostgreSQL admin password:${NC}"
    read -s POSTGRES_PASSWORD
fi

# Test connection
echo -e "${YELLOW}🔌 Testing PostgreSQL connection...${NC}"
if ! docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -c "SELECT 1;" >/dev/null 2>&1; then
    echo -e "${RED}❌ Cannot connect to PostgreSQL. Please check credentials and container status.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ PostgreSQL connection successful${NC}"

# Create ratings user if it doesn't exist
echo -e "${YELLOW}👤 Creating ratings user...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} < scripts/create-user.sql

echo -e "${GREEN}✅ Ratings user configured${NC}"

# Create databases
echo -e "${YELLOW}🗄️  Creating databases...${NC}"

# Check database status
docker exec -i ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} < scripts/create-databases.sql

# Create databases if they don't exist
WRITE_DB_EXISTS=$(docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -t -c "SELECT 1 FROM pg_database WHERE datname = 'ratings_write';" | xargs)
if [ "$WRITE_DB_EXISTS" != "1" ]; then
    docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -c "CREATE DATABASE ratings_write;"
    echo -e "${GREEN}✅ Database 'ratings_write' created${NC}"
else
    echo -e "${YELLOW}ℹ️  Database 'ratings_write' already exists${NC}"
fi

READ_DB_EXISTS=$(docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -t -c "SELECT 1 FROM pg_database WHERE datname = 'ratings_read';" | xargs)
if [ "$READ_DB_EXISTS" != "1" ]; then
    docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -c "CREATE DATABASE ratings_read;"
    echo -e "${GREEN}✅ Database 'ratings_read' created${NC}"
else
    echo -e "${YELLOW}ℹ️  Database 'ratings_read' already exists${NC}"
fi

echo -e "${GREEN}✅ Databases created${NC}"

# Grant permissions on databases
echo -e "${YELLOW}🔐 Granting database permissions...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} < scripts/grant-permissions.sql

# Grant schema permissions for both databases
echo -e "${YELLOW}🔐 Granting schema permissions...${NC}"
docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ratings_write -c "GRANT ALL ON SCHEMA public TO ${RATINGS_USER};"
docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ratings_read -c "GRANT ALL ON SCHEMA public TO ${RATINGS_USER};"

echo -e "${GREEN}✅ Database and schema permissions granted${NC}"

# Drop existing tables to apply UUID migration
echo -e "${YELLOW}🗑️  Dropping existing tables (if any)...${NC}"
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -c "DROP TABLE IF EXISTS reviews CASCADE;" 2>/dev/null || true
docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -c "DROP TABLE IF EXISTS product_stats CASCADE;" 2>/dev/null || true
echo -e "${GREEN}✅ Existing tables dropped${NC}"

# Run write database migrations
echo -e "${YELLOW}📝 Setting up write database schema with UUID...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write < scripts/init-write-db.sql

echo -e "${GREEN}✅ Write database schema created with UUID product_id${NC}"

# Run read database migrations
echo -e "${YELLOW}📊 Setting up read database schema with UUID...${NC}"
docker exec -i ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read < scripts/init-read-db.sql

echo -e "${GREEN}✅ Read database schema created with UUID product_id${NC}"

# Grant additional permissions on database objects
echo -e "${YELLOW}🔐 Granting object permissions...${NC}"
docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ratings_write -c "
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${RATINGS_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${RATINGS_USER};
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ${RATINGS_USER};
"

docker exec ${POSTGRES_CONTAINER} psql -U ${POSTGRES_USER} -d ratings_read -c "
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO ${RATINGS_USER};
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO ${RATINGS_USER};
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA public TO ${RATINGS_USER};
"

echo -e "${GREEN}✅ Object permissions granted${NC}"

# Verify setup
echo -e "${YELLOW}🔍 Verifying database setup...${NC}"

# Check write database
WRITE_TABLES=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_write -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'reviews';" | xargs)
if [ "$WRITE_TABLES" -eq 1 ]; then
    echo -e "${GREEN}✅ Write database (ratings_write) - reviews table exists${NC}"
else
    echo -e "${RED}❌ Write database setup failed${NC}"
    exit 1
fi

# Check read database
READ_TABLES=$(docker exec ${POSTGRES_CONTAINER} psql -U ${RATINGS_USER} -d ratings_read -t -c "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'product_stats';" | xargs)
if [ "$READ_TABLES" -eq 1 ]; then
    echo -e "${GREEN}✅ Read database (ratings_read) - product_stats table exists${NC}"
else
    echo -e "${RED}❌ Read database setup failed${NC}"
    exit 1
fi

echo ""
echo -e "${GREEN}🎉 Database setup completed successfully!${NC}"
echo ""
echo -e "${BLUE}📋 Setup Summary:${NC}"
echo "  • User: ${RATINGS_USER}"
echo "  • Write Database: ratings_write (reviews table)"
echo "  • Read Database: ratings_read (product_stats table)"
echo "  • Network: ${NETWORK}"
echo "  • Product ID Type: UUID (migrated from VARCHAR)"
echo ""
echo -e "${YELLOW}ℹ️  Important: product_id is now UUID type${NC}"
echo "  Sample UUIDs for testing:"
echo "    • 550e8400-e29b-41d4-a716-446655440001"
echo "    • 550e8400-e29b-41d4-a716-446655440002"
echo "    • 550e8400-e29b-41d4-a716-446655440003"
echo ""
echo -e "${BLUE}🚀 Next Steps:${NC}"
echo "  1. Build and start your services:"
echo "     ./mvnw clean package -DskipTests"
echo "     docker compose build"
echo "     docker compose up -d"
echo ""
echo "  2. Test the setup:"
echo "     curl http://localhost:8081/health"
echo "     curl http://localhost:8082/health"
echo ""
echo -e "${GREEN}✅ Ready to go!${NC}"