# Final Acceptance Test for Ratings Microservice System

This document describes the final end-to-end acceptance test that validates the complete ratings microservice system according to the specified acceptance criteria.

## Test Overview

The final acceptance test implements the following 9-step validation process:

1. **Create Product**: Send a gRPC CreateProductCommand to the products-service with name: "My PoC Product"
2. **Wait for Projection**: Pause for 3 seconds
3. **Query Empty State**: Send GetProductPage GraphQL query, expect null ratings
4. **Submit Rating**: Send gRPC SubmitRatingCommand (rating: 5)
5. **Wait for Projection**: Pause for 3 seconds
6. **Query Populated State**: Send GraphQL query, expect averageRating: 5.0, reviewCount: 1
7. **Test Aggregation**: Send second gRPC SubmitRatingCommand (rating: 1)
8. **Wait for Projection**: Pause for 3 seconds
9. **Query Aggregated State**: Send GraphQL query, expect averageRating: 3.0, reviewCount: 2

## Implementation Notes

Since this ratings microservice system doesn't include a separate products service (it extends Product entities via GraphQL federation), the test has been adapted to work with the current architecture:

- **Product Creation**: Simulated since no products service exists
- **gRPC Calls**: Made to the actual command service
- **GraphQL Queries**: Made to the actual query service
- **Event Projection**: Validated through database state changes

## Running the Test

### Prerequisites

1. **Services Running**: Both command and query services must be running
2. **Dependencies**: Docker, Docker Compose, curl, python3
3. **Optional**: grpcurl for actual gRPC calls

### Option 1: Shell Script (Recommended)

```bash
# Start the system
docker-compose up -d

# Wait for services to be ready
sleep 30

# Run the acceptance test
./integration-tests/final-acceptance-test.sh
```

### Option 2: Java Integration Test

```bash
# Run the Java-based acceptance test
cd integration-tests
mvn test -Dtest=FinalAcceptanceTest
```

### Option 3: Manual Step-by-Step

#### Step 1: Start Services

```bash
# Terminal 1: Start Command Service
cd command-service
mvn spring-boot:run

# Terminal 2: Start Query Service  
cd query-service
mvn spring-boot:run

# Terminal 3: Start Infrastructure
docker-compose up -d postgres-write postgres-read kafka
```

#### Step 2: Execute Test Steps

```bash
# Test 1: Product Creation (Simulated)
echo "Product P123 'My PoC Product' created (simulated)"

# Test 2: Wait
sleep 3

# Test 3: Query Empty State
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetProductPage($id: ID!) { product(id: $id) { id name averageRating reviewCount } }",
    "variables": { "id": "P123" }
  }'

# Test 4: Submit Rating (requires grpcurl)
grpcurl -plaintext \
  -d '{"product_id":"P123","rating":5,"user_id":"test-user"}' \
  localhost:9090 \
  com.ratings.RatingsCommandService/SubmitRating

# Test 5: Wait
sleep 3

# Test 6: Query Populated State
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetProductPage($id: ID!) { product(id: $id) { id name averageRating reviewCount } }",
    "variables": { "id": "P123" }
  }'

# Test 7: Submit Second Rating
grpcurl -plaintext \
  -d '{"product_id":"P123","rating":1,"user_id":"test-user-2"}' \
  localhost:9090 \
  com.ratings.RatingsCommandService/SubmitRating

# Test 8: Wait
sleep 3

# Test 9: Query Aggregated State
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetProductPage($id: ID!) { product(id: $id) { id name averageRating reviewCount } }",
    "variables": { "id": "P123" }
  }'
```

## Expected Results

### Test 3: Empty State Response
```json
{
  "data": {
    "product": {
      "id": "P123",
      "name": "My PoC Product",
      "averageRating": null,
      "reviewCount": null
    }
  }
}
```

### Test 6: Populated State Response
```json
{
  "data": {
    "product": {
      "id": "P123", 
      "name": "My PoC Product",
      "averageRating": 5.0,
      "reviewCount": 1
    }
  }
}
```

### Test 9: Aggregated State Response
```json
{
  "data": {
    "product": {
      "id": "P123",
      "name": "My PoC Product", 
      "averageRating": 3.0,
      "reviewCount": 2
    }
  }
}
```

## Troubleshooting

### Services Not Starting

```bash
# Check service logs
docker-compose logs command-service
docker-compose logs query-service

# Check database connectivity
docker-compose logs postgres-write
docker-compose logs postgres-read

# Check Kafka
docker-compose logs kafka
```

### gRPC Connection Issues

```bash
# Test gRPC service health
grpcurl -plaintext localhost:9090 list

# Check if port is open
nc -z localhost 9090
```

### GraphQL Query Issues

```bash
# Test GraphQL endpoint
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query":"{ __schema { types { name } } }"}'

# Check if port is open
nc -z localhost 8082
```

### Event Projection Issues

```bash
# Check Kafka topics
docker exec -it $(docker-compose ps -q kafka) kafka-topics --list --bootstrap-server localhost:9092

# Check database state
docker exec -it $(docker-compose ps -q postgres-read) psql -U ratings_user -d ratings_read -c "SELECT * FROM product_stats;"
```

## Architecture Validation

This test validates the following architectural components:

- ✅ **CQRS Pattern**: Separate command and query services
- ✅ **Event Sourcing**: Events published to Kafka after commands
- ✅ **Event Projection**: Read database updated from events
- ✅ **gRPC Interface**: Command service accepts gRPC calls
- ✅ **GraphQL Federation**: Query service extends Product type
- ✅ **Eventual Consistency**: Data consistency across services
- ✅ **Aggregation Logic**: Proper calculation of averages and counts

## Success Criteria

The test passes when:

1. All 9 test steps complete without errors
2. gRPC rating submissions return "OK" status
3. GraphQL queries return expected data structure
4. Event projection updates read database correctly
5. Aggregation calculations are accurate (3.0 average from ratings 5 and 1)
6. Review counts are properly maintained

## Performance Expectations

- gRPC calls should complete within 1 second
- GraphQL queries should complete within 500ms
- Event projection should complete within 3 seconds
- System should handle the test load without errors

This acceptance test validates that the complete ratings microservice system works end-to-end according to the specified requirements and design.