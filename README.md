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

### Prerequisites

- Java 17 or higher
- Docker and Docker Compose
- No Maven installation required (uses Maven wrapper)

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

2. **Start the infrastructure:**
   ```bash
   docker-compose up -d postgres kafka zookeeper
   ```

3. **Run the services:**
   ```bash
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
  - Query: `localhost:8082/health`

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