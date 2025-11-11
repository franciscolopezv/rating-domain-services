# Ratings System

A CQRS-based microservice system for managing product ratings and reviews, built with Spring Boot, gRPC, GraphQL, and Kafka.

## Architecture Overview

The system implements Command Query Responsibility Segregation (CQRS) with event sourcing to handle the complete ratings domain:

- **Command Service**: Handles write operations via gRPC
- **Query Service**: Handles read operations via GraphQL federation and processes events for data projection
- **Shared Module**: Common models, events, and utilities

## Technology Stack

- **Java 17** - Programming language
- **Spring Boot 3.2** - Application framework
- **gRPC** - Command service API
- **GraphQL** - Query service API with Apollo Federation
- **Apache Kafka** - Event streaming
- **PostgreSQL** - Database (separate read/write databases)
- **Maven** - Build tool (includes Maven wrapper)
- **Docker** - Containerization

## Project Structure

```
ratings-system/
├── shared/                 # Common models and utilities
├── command-service/        # gRPC-based write service
├── query-service/         # GraphQL-based read service
├── docker-compose.yml     # Local development environment
└── pom.xml               # Parent Maven configuration
```

## Quick Start

> **📘 New to this project?** See [SETUP_QUICK_START.md](SETUP_QUICK_START.md) for a step-by-step guide including the UUID migration.

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- No Maven installation required (uses Maven wrapper)
- External `platform-infrastructure` network with PostgreSQL, Kafka, and Zookeeper (see [NETWORK_SETUP.md](NETWORK_SETUP.md))

### Local Development

1. **Clone and build the project:**
   ```bash
   git clone <repository-url>
   cd ratings-system
   
   # Option 1: Use Maven wrapper directly
   ./mvnw clean install
   
   # Option 2: Use provided build script
   ./build.sh                    # Linux/macOS
   build.cmd                     # Windows
   ```

2. **Ensure infrastructure is running:**
   ```bash
   # The platform-infrastructure network should already have:
   # - PostgreSQL (postgres:5432)
   # - Kafka (kafka:29092) 
   # - Zookeeper (zookeeper:2181)
   # See NETWORK_SETUP.md for details
   ```

3. **Run the services:**
   ```bash
   # Option 1: Using Docker Compose (recommended)
   docker compose up -d
   
   # Option 2: Run locally (for development)
   # Terminal 1 - Command Service
   cd command-service
   ../mvnw spring-boot:run

   # Terminal 2 - Query Service
   cd query-service
   ../mvnw spring-boot:run
   ```

### API Endpoints

- **Command Service (gRPC)**: `localhost:9090`
- **Query Service (GraphQL)**: `localhost:8082/graphql`
- **Health Checks**: 
  - Command: `localhost:8081/health`
  - Query: `localhost:8082/health` (simple) or `localhost:8082/actuator/health` (detailed)
- **Federation Service**: `localhost:8082/_service` (GraphQL Federation SDL)

## Development

### Build Profiles

The project supports multiple build profiles:

- `dev` (default) - Development environment
- `staging` - Staging environment  
- `prod` - Production environment

```bash
# Build for specific environment
./mvnw clean install -P staging
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run tests for specific module
cd shared && ../mvnw test
```

### Maven Wrapper

This project includes a Maven wrapper (`mvnw`/`mvnw.cmd`) that automatically downloads and uses the correct Maven version. This ensures consistent builds across different environments without requiring a local Maven installation.

**Benefits:**
- No need to install Maven locally
- Consistent Maven version across all environments
- Simplified CI/CD setup
- Better reproducibility

**Usage:**
- Linux/macOS: `./mvnw [goals]`
- Windows: `mvnw.cmd [goals]`

### Code Quality

The project uses standard Java conventions and includes:

- Input validation using Bean Validation
- Comprehensive error handling
- Structured logging
- Health checks and metrics

## API Documentation

### gRPC Service

The command service exposes a gRPC API for rating submissions:

```protobuf
service RatingsCommandService {
  rpc SubmitRating (SubmitRatingCommand) returns (SubmitRatingResponse);
}
```

### GraphQL Schema

The query service extends the Product type via Apollo Federation:

```graphql
extend type Product @key(fields: "id") {
  id: ID! @external
  averageRating: Float
  reviewCount: Int
  ratingDistribution: RatingDistribution
}
```

## Testing the APIs

### Testing gRPC Endpoints

#### Using grpcurl

Install grpcurl if you haven't already:
```bash
# macOS
brew install grpcurl

# Linux
go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest

# Or download from: https://github.com/fullstorydev/grpcurl/releases
```

**List available services:**
```bash
grpcurl -plaintext localhost:9090 list
```

**Describe the service:**
```bash
grpcurl -plaintext localhost:9090 describe com.ratings.RatingsCommandService
```

**Submit a rating:**
```bash
grpcurl -plaintext -d '{
  "product_id": "550e8400-e29b-41d4-a716-446655440001",
  "rating": 5,
  "user_id": "user-456",
  "review_text": "Excellent product! Highly recommended."
}' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
```

**Submit a rating without review text:**
```bash
grpcurl -plaintext -d '{
  "product_id": "550e8400-e29b-41d4-a716-446655440002",
  "rating": 4,
  "user_id": "user-101"
}' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
```

#### Using Python (grpcio)

```python
import grpc
from ratings_pb2 import SubmitRatingCommand
from ratings_pb2_grpc import RatingsCommandServiceStub

# Create a channel
channel = grpc.insecure_channel('localhost:9090')
stub = RatingsCommandServiceStub(channel)

# Submit a rating
request = SubmitRatingCommand(
    product_id="550e8400-e29b-41d4-a716-446655440001",
    rating=5,
    user_id="user-456",
    review_text="Great product!"
)

response = stub.SubmitRating(request)
print(f"Submission ID: {response.submission_id}")
print(f"Status: {response.status}")
print(f"Message: {response.message}")
```

### Testing GraphQL Endpoints

#### Using curl

**Check service health:**
```bash
curl http://localhost:8082/health
```

**Introspection query:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ __schema { queryType { name } } }"}'
```

**Get Federation SDL:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ _service { sdl } }"}' | jq -r '.data._service.sdl'
```

**Query product rating statistics:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query GetProductStats($productId: ID!) { productRatingStats(productId: $productId) { productId averageRating reviewCount ratingDistribution { oneStar twoStar threeStar fourStar fiveStar total mostCommonRating hasPositiveRatings hasNegativeRatings diversityScore } lastUpdated } }",
    "variables": {"productId": "550e8400-e29b-41d4-a716-446655440001"}
  }'
```

**Query top-rated products:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ topRatedProducts(limit: 5) { productId averageRating reviewCount } }"
  }'
```

**Query most reviewed products:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ mostReviewedProducts(limit: 5) { productId reviewCount averageRating } }"
  }'
```

**Query overall statistics:**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ overallRatingStats { totalProducts totalReviews overallAverageRating productsWithRatings } }"
  }'
```

**Federation entity resolution (for Apollo Gateway):**
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "query($_representations: [_Any!]!) { _entities(representations: $_representations) { ... on Product { id averageRating reviewCount } } }",
    "variables": {
      "_representations": [
        {"__typename": "Product", "id": "550e8400-e29b-41d4-a716-446655440001"}
      ]
    }
  }'
```

#### Using GraphQL Playground or Postman

1. Open your GraphQL client and connect to: `http://localhost:8082/graphql`

2. Example queries:

```graphql
# Get product rating statistics
query GetProductStats {
  productRatingStats(productId: "550e8400-e29b-41d4-a716-446655440001") {
    productId
    averageRating
    reviewCount
    ratingDistribution {
      oneStar
      twoStar
      threeStar
      fourStar
      fiveStar
      total
      mostCommonRating
      hasPositiveRatings
      hasNegativeRatings
      diversityScore
    }
    lastUpdated
  }
}

# Get top-rated products
query TopRated {
  topRatedProducts(limit: 10) {
    productId
    averageRating
    reviewCount
  }
}

# Get overall statistics
query OverallStats {
  overallRatingStats {
    totalProducts
    totalReviews
    overallAverageRating
    productsWithRatings
  }
}
```

### Automated Testing Script

Run the provided test script to verify all endpoints:

```bash
chmod +x test-endpoints.sh
./test-endpoints.sh
```

This script tests:
- Health endpoints (`/health`, `/actuator/health`)
- Federation REST endpoints (`/_service`, `/_service/info`)
- GraphQL endpoint (`/graphql`)
- Federation SDL query

## Monitoring and Operations

### Health Checks

Both services provide health check endpoints:

- `/health` - Overall service health
- `/ready` - Readiness probe for Kubernetes

### Metrics

The services expose metrics for:

- Rating submission rates and latency
- Event processing rates and latency
- Database connection health
- Kafka connectivity

### Logging

Structured logging is configured with:

- Request/response logging
- Error tracking
- Performance metrics
- Event processing logs

## Contributing

1. Follow the existing code style and conventions
2. Write tests for new functionality
3. Update documentation as needed
4. Ensure all tests pass before submitting

## License

[Add your license information here]