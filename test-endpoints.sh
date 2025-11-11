#!/bin/bash

# Test script for query service endpoints
# This script tests the health and federation endpoints

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🧪 Testing Query Service Endpoints${NC}"
echo "=================================="

# Test simple health endpoint
echo -e "${YELLOW}📋 Testing /health endpoint...${NC}"
if curl -s -f http://localhost:8082/health > /dev/null; then
    echo -e "${GREEN}✅ /health endpoint is accessible${NC}"
    curl -s http://localhost:8082/health | jq '.' || echo "Response received (jq not available for formatting)"
else
    echo -e "${RED}❌ /health endpoint is not accessible${NC}"
fi

echo ""

# Test actuator health endpoint
echo -e "${YELLOW}📋 Testing /actuator/health endpoint...${NC}"
if curl -s -f http://localhost:8082/actuator/health > /dev/null; then
    echo -e "${GREEN}✅ /actuator/health endpoint is accessible${NC}"
    curl -s http://localhost:8082/actuator/health | jq '.' || echo "Response received (jq not available for formatting)"
else
    echo -e "${RED}❌ /actuator/health endpoint is not accessible${NC}"
fi

echo ""

# Test federation service endpoint
echo -e "${YELLOW}📋 Testing /_service endpoint...${NC}"
if curl -s -f http://localhost:8082/_service > /dev/null; then
    echo -e "${GREEN}✅ /_service endpoint is accessible${NC}"
    echo "SDL Schema (first 200 characters):"
    curl -s http://localhost:8082/_service | jq -r '.sdl' | head -c 200 || echo "Response received (jq not available for formatting)"
    echo "..."
else
    echo -e "${RED}❌ /_service endpoint is not accessible${NC}"
fi

echo ""

# Test federation service info endpoint
echo -e "${YELLOW}📋 Testing /_service/info endpoint...${NC}"
if curl -s -f http://localhost:8082/_service/info > /dev/null; then
    echo -e "${GREEN}✅ /_service/info endpoint is accessible${NC}"
    curl -s http://localhost:8082/_service/info | jq '.' || echo "Response received (jq not available for formatting)"
else
    echo -e "${RED}❌ /_service/info endpoint is not accessible${NC}"
fi

echo ""

# Test GraphQL endpoint
echo -e "${YELLOW}📋 Testing /graphql endpoint...${NC}"
GRAPHQL_QUERY='{"query": "{ __schema { queryType { name } } }"}'
if curl -s -f -X POST -H "Content-Type: application/json" -d "$GRAPHQL_QUERY" http://localhost:8082/graphql > /dev/null; then
    echo -e "${GREEN}✅ /graphql endpoint is accessible${NC}"
    curl -s -X POST -H "Content-Type: application/json" -d "$GRAPHQL_QUERY" http://localhost:8082/graphql | jq '.' || echo "Response received (jq not available for formatting)"
else
    echo -e "${RED}❌ /graphql endpoint is not accessible${NC}"
fi

echo ""

# Test Federation _service query
echo -e "${YELLOW}📋 Testing GraphQL _service query (Apollo Federation)...${NC}"
FEDERATION_QUERY='{"query": "{ _service { sdl } }"}'
if curl -s -f -X POST -H "Content-Type: application/json" -d "$FEDERATION_QUERY" http://localhost:8082/graphql > /dev/null; then
    echo -e "${GREEN}✅ _service query is accessible${NC}"
    echo "SDL Schema (first 200 characters):"
    curl -s -X POST -H "Content-Type: application/json" -d "$FEDERATION_QUERY" http://localhost:8082/graphql | jq -r '.data._service.sdl' | head -c 200 || echo "Response received (jq not available for formatting)"
    echo "..."
else
    echo -e "${RED}❌ _service query is not accessible${NC}"
fi

echo ""
echo -e "${GREEN}🎉 Endpoint testing completed!${NC}"
echo ""
echo -e "${BLUE}📋 Summary:${NC}"
echo "  • Health endpoints: /health and /actuator/health"
echo "  • Federation REST: /_service and /_service/info"
echo "  • Federation GraphQL: _service query"
echo "  • GraphQL endpoint: /graphql"