#!/bin/bash

# Utility script to run SQL files against PostgreSQL container
# Usage: ./run-sql.sh <sql-file> [database] [user]

set -e

# Configuration
POSTGRES_CONTAINER="postgres"
DEFAULT_USER="postgres"
DEFAULT_DATABASE="postgres"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Check arguments
if [ $# -lt 1 ]; then
    echo -e "${RED}Usage: $0 <sql-file> [database] [user]${NC}"
    echo ""
    echo "Examples:"
    echo "  $0 scripts/create-user.sql"
    echo "  $0 scripts/init-write-db.sql ratings_write ratings_user"
    echo "  $0 scripts/init-read-db.sql ratings_read ratings_user"
    exit 1
fi

SQL_FILE="$1"
DATABASE="${2:-$DEFAULT_DATABASE}"
USER="${3:-$DEFAULT_USER}"

# Check if SQL file exists
if [ ! -f "$SQL_FILE" ]; then
    echo -e "${RED}❌ SQL file '$SQL_FILE' not found${NC}"
    exit 1
fi

# Check if PostgreSQL container is running
if ! docker ps --format "table {{.Names}}" | grep -q "^${POSTGRES_CONTAINER}$"; then
    echo -e "${RED}❌ PostgreSQL container '${POSTGRES_CONTAINER}' is not running${NC}"
    exit 1
fi

echo -e "${BLUE}🗄️  Running SQL Script${NC}"
echo "======================"
echo "File: $SQL_FILE"
echo "Database: $DATABASE"
echo "User: $USER"
echo "Container: $POSTGRES_CONTAINER"
echo ""

# Run the SQL file
echo -e "${YELLOW}📝 Executing SQL script...${NC}"
if docker exec -i ${POSTGRES_CONTAINER} psql -U ${USER} -d ${DATABASE} < "$SQL_FILE"; then
    echo -e "${GREEN}✅ SQL script executed successfully${NC}"
else
    echo -e "${RED}❌ SQL script execution failed${NC}"
    exit 1
fi