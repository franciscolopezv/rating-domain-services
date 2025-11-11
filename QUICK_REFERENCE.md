# Quick Reference - Ratings Microservice

## 🚀 Quick Start

```bash
# 1. Setup databases
./setup-database.sh

# 2. Build services
./mvnw clean package -DskipTests

# 3. Start services
docker compose up -d

# 4. Verify endpoints
./test-endpoints.sh
```

## 📡 Service Endpoints

### Command Service
- **gRPC**: `localhost:9090`
- **HTTP**: `localhost:8081`
- **Health**: `http://localhost:8081/health`

### Query Service
- **GraphQL**: `http://localhost:8082/graphql`
- **Health**: `http://localhost:8082/health`
- **Federation**: `http://localhost:8082/_service`

## 🔍 GraphQL Queries

### Submit Rating (gRPC)
```bash
grpcurl -plaintext -d '{
  "product_id": "P123",
  "rating": 5,
  "user_id": "test-user"
}' localhost:9090 com.ratings.RatingsCommandService/SubmitRating
```

### Query Rating Stats
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ productRatingStats(productId: \"P123\") { averageRating reviewCount } }"
  }'
```

### Federation Service Discovery
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ _service { sdl } }"}'
```

### Federation Entity Resolution
```bash
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{
    "query": "{ _entities(representations: [{__typename: \"Product\", id: \"P123\"}]) { ... on Product { id averageRating reviewCount } } }"
  }'
```

## 🗄️ Database

### Connection Details
- **Host**: `postgres` (in platform-infrastructure network)
- **Port**: `5432`
- **Write DB**: `ratings_write`
- **Read DB**: `ratings_read`
- **User**: `ratings_user`
- **Password**: `ratings_pass`

### Verify Database
```bash
./verify-database.sh
```

## 🔧 Useful Commands

### Build
```bash
./mvnw clean package -DskipTests
```

### Run Locally
```bash
# Command Service
cd command-service && ../mvnw spring-boot:run

# Query Service
cd query-service && ../mvnw spring-boot:run
```

### Docker
```bash
# Build images
docker compose build

# Start services
docker compose up -d

# View logs
docker compose logs -f

# Stop services
docker compose down
```

### Database Management
```bash
# Setup databases
./setup-database.sh

# Verify setup
./verify-database.sh

# Run SQL script
./run-sql.sh scripts/init-write-db.sql ratings_write ratings_user
```

## 📊 Health Checks

```bash
# Command Service
curl http://localhost:8081/health

# Query Service (simple)
curl http://localhost:8082/health

# Query Service (detailed)
curl http://localhost:8082/actuator/health
```

## 🔗 Apollo Federation

### Gateway Configuration
```javascript
const gateway = new ApolloGateway({
  serviceList: [
    { name: 'ratings', url: 'http://localhost:8082/graphql' }
  ]
});
```

### Federated Query Example
```graphql
query {
  product(id: "P123") {
    id
    name              # From products service
    averageRating     # From ratings service
    reviewCount       # From ratings service
  }
}
```

## 📁 Important Files

- `setup-database.sh` - Database setup script
- `verify-database.sh` - Database verification
- `test-endpoints.sh` - Endpoint testing
- `docker-compose.yml` - Service orchestration
- `FEDERATION_VALIDATION.md` - Federation compliance report
- `DATABASE_SETUP.md` - Database setup guide
- `NETWORK_SETUP.md` - Network configuration guide

## 🐛 Troubleshooting

### Service won't start
```bash
# Check logs
docker compose logs command-service
docker compose logs query-service

# Check database
./verify-database.sh

# Check network
docker network inspect platform-infrastructure
```

### Database connection issues
```bash
# Test connectivity
docker exec postgres psql -U ratings_user -d ratings_write -c "SELECT 1;"

# Reset databases
./setup-database.sh
```

### Federation not working
```bash
# Test _service query
curl -X POST http://localhost:8082/graphql \
  -H "Content-Type: application/json" \
  -d '{"query": "{ _service { sdl } }"}'

# Check schema
curl http://localhost:8082/_service
```

## 📚 Documentation

- `README.md` - Main documentation
- `DATABASE_SETUP.md` - Database setup guide
- `NETWORK_SETUP.md` - Network configuration
- `FEDERATION_VALIDATION.md` - Federation validation report
- `SECURITY.md` - Security configuration