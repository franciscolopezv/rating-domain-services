# Security Configuration Guide

This document outlines the security configurations and best practices for the Ratings microservice system.

## Overview

The Ratings system implements multiple layers of security:

1. **JWT Authentication** for gRPC and GraphQL endpoints
2. **Database Connection Security** with SSL/TLS encryption
3. **Service-to-Service Authentication** for internal communication
4. **CORS Configuration** for web security
5. **Actuator Security** for monitoring endpoints

## JWT Authentication

### Configuration

JWT authentication is configured through environment variables:

```bash
# JWT Secret (minimum 256 bits)
JWT_SECRET=your_secure_jwt_secret_key_here_minimum_256_bits

# JWT Token Expiration (in seconds)
JWT_EXPIRATION=3600
```

### gRPC Authentication

The command service uses JWT tokens in gRPC metadata:

```
authorization: Bearer <jwt_token>
```

The `GrpcAuthenticationInterceptor` validates tokens for all gRPC calls except:
- Health checks
- gRPC reflection services

### GraphQL Authentication

The query service supports optional JWT authentication for GraphQL queries:

```http
POST /graphql
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "query": "{ product(id: \"123\") { averageRating } }"
}
```

## Database Security

### SSL/TLS Configuration

For production environments, enable SSL connections to PostgreSQL:

```yaml
# Environment variables
DB_SSL_ENABLED=true
DB_SSL_CERT_PATH=/path/to/client.crt
DB_SSL_KEY_PATH=/path/to/client.key
DB_SSL_ROOT_CERT_PATH=/path/to/ca.crt
```

### Connection Pool Security

- Connection validation queries prevent SQL injection
- Read-only connections for query service
- Connection timeouts prevent resource exhaustion
- Pool monitoring enabled in production

## Service-to-Service Authentication

For internal service communication, use service tokens:

```bash
# Service authentication secret
SERVICE_SECRET=your_service_authentication_secret

# Service token expiration (in seconds)
SERVICE_TOKEN_EXPIRATION=7200
```

## CORS Configuration

### Development
```yaml
spring:
  graphql:
    cors:
      allowed-origins: "*"
      allowed-methods: GET,POST,OPTIONS
      allowed-headers: "*"
```

### Production
```yaml
spring:
  graphql:
    cors:
      allowed-origins: "https://api.example.com"
      allowed-methods: GET,POST
      allowed-headers: "Content-Type,Authorization"
      allow-credentials: true
```

## Actuator Security

Actuator endpoints are secured based on environment:

### Development
- All endpoints exposed: `management.endpoints.web.exposure.include=*`
- Health details always shown

### Production
- Limited endpoints: `health,info,metrics,prometheus`
- Health details never shown to unauthorized users
- Metrics secured for monitoring systems

## Security Profiles

### Development (`dev` profile)
- JWT validation enabled but lenient
- All actuator endpoints exposed
- CORS allows all origins
- Database SSL optional

### Staging (`staging` profile)
- JWT validation enforced
- Limited actuator endpoints
- Restricted CORS origins
- Database SSL recommended

### Production (`prod` profile)
- Full JWT validation enforced
- Minimal actuator endpoints
- Strict CORS configuration
- Database SSL required
- Service-to-service authentication required

## Environment Variables

### Required for Production

```bash
# JWT Configuration
JWT_SECRET=<256-bit-secret>
JWT_EXPIRATION=3600

# Database Security
DB_SSL_ENABLED=true
DB_SSL_CERT_PATH=/certs/client.crt
DB_SSL_KEY_PATH=/certs/client.key
DB_SSL_ROOT_CERT_PATH=/certs/ca.crt

# Service Authentication
SERVICE_SECRET=<service-secret>

# CORS Configuration
ALLOWED_ORIGINS=https://api.example.com
```

### Optional

```bash
# Security Features
GRPC_SECURITY_ENABLED=true
GRAPHQL_SECURITY_ENABLED=false

# Service Token Expiration
SERVICE_TOKEN_EXPIRATION=7200
```

## Security Best Practices

### JWT Tokens
1. Use strong secrets (minimum 256 bits)
2. Set appropriate expiration times
3. Rotate secrets regularly
4. Never log JWT tokens

### Database Connections
1. Use SSL/TLS in production
2. Use connection pooling
3. Set connection timeouts
4. Use read-only connections where appropriate

### Service Communication
1. Use service-specific tokens for internal calls
2. Validate all incoming requests
3. Log security events
4. Monitor for suspicious activity

### Deployment
1. Use secrets management systems
2. Never commit secrets to version control
3. Use environment-specific configurations
4. Regular security audits

## Monitoring and Logging

Security events are logged at INFO level:
- Authentication attempts
- Token validation failures
- Database connection issues
- CORS violations

Monitor these logs for security incidents and adjust configurations as needed.

## Troubleshooting

### Common Issues

1. **JWT Token Invalid**
   - Check token expiration
   - Verify JWT secret configuration
   - Ensure proper token format

2. **Database Connection Failed**
   - Verify SSL certificate paths
   - Check database credentials
   - Confirm network connectivity

3. **CORS Errors**
   - Check allowed origins configuration
   - Verify request headers
   - Confirm HTTP methods allowed

4. **gRPC Authentication Failed**
   - Ensure authorization header is set
   - Check token format (Bearer prefix)
   - Verify gRPC metadata configuration