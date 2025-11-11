# Network Setup

This project uses an external Docker network called `platform-infrastructure` that provides shared infrastructure services.

## Prerequisites

Before running the ratings services, ensure the `platform-infrastructure` network exists with the required services:

### 1. Create the Network (if not exists)
```bash
docker network create platform-infrastructure
```

### 2. Required Infrastructure Services

The following services must be running in the `platform-infrastructure` network:

#### PostgreSQL
- **Container name**: `postgres`
- **Port**: 5432
- **Databases**: 
  - `ratings_write` (for command service)
  - `ratings_read` (for query service)
- **Credentials**: 
  - Username: `ratings_user`
  - Password: `ratings_pass`

**Database Setup Required**: Run `./setup-database.sh` to create databases and schemas (see [DATABASE_SETUP.md](DATABASE_SETUP.md))

#### Apache Kafka
- **Container name**: `kafka`
- **Internal port**: 29092
- **External port**: 9092
- **Required topics**: `rating-events`

#### Zookeeper
- **Container name**: `zookeeper`
- **Port**: 2181

## Database Setup

Before running the services, set up the required databases:

```bash
# Automated setup (recommended)
./setup-database.sh

# Or follow manual setup guide
# See DATABASE_SETUP.md for detailed instructions
```

## Running the Services

Once the infrastructure is available and databases are set up:

```bash
# Build the services
./mvnw clean package -DskipTests

# Build Docker images
docker compose build

# Start the ratings services
docker compose up -d

# Verify setup
./verify-database.sh
```

## Service Endpoints

- **Command Service (gRPC)**: `localhost:9090`
- **Command Service (HTTP)**: `localhost:8081`
- **Query Service (GraphQL)**: `localhost:8082/graphql`
- **Query Service (Federation)**: `localhost:8082/_service`

## Health Checks

- Command Service: `http://localhost:8081/health`
- Query Service: `http://localhost:8082/health` (simple)
- Query Service: `http://localhost:8082/actuator/health` (detailed)

## Apollo Federation Support

The query service is a fully compliant Apollo Federation subgraph:

### GraphQL Federation Queries
- **`_service` query**: Returns the GraphQL SDL (Schema Definition Language)
  ```graphql
  query {
    _service {
      sdl
    }
  }
  ```

- **`_entities` query**: Resolves entity references from the gateway
  ```graphql
  query {
    _entities(representations: [{__typename: "Product", id: "P123"}]) {
      ... on Product {
        id
        averageRating
        reviewCount
      }
    }
  }
  ```

### REST Endpoints (Alternative)
- **Service Discovery**: `http://localhost:8082/_service` - Returns GraphQL SDL
- **Service Info**: `http://localhost:8082/_service/info` - Returns service metadata

## Environment Variables

The services can be configured using environment variables:

### Database Configuration
- `DB_HOST`: PostgreSQL host (default: `postgres`)
- `DB_PORT`: PostgreSQL port (default: `5432`)
- `DB_USERNAME`: Database username (default: `ratings_user`)
- `DB_PASSWORD`: Database password (default: `ratings_pass`)

### Kafka Configuration
- `KAFKA_BOOTSTRAP_SERVERS`: Kafka bootstrap servers (default: `kafka:29092`)

## Troubleshooting

### Network Issues
```bash
# Check if network exists
docker network ls | grep platform-infrastructure

# Inspect network
docker network inspect platform-infrastructure

# Check which containers are connected
docker network inspect platform-infrastructure --format='{{range .Containers}}{{.Name}} {{end}}'
```

### Service Dependencies
```bash
# Check if required services are running
docker ps --filter network=platform-infrastructure

# Test connectivity from ratings services
docker exec ratings-command-service ping postgres
docker exec ratings-command-service ping kafka
```