# Simple runtime-only Dockerfile for Command Service
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for health checks and create non-root user
RUN apk add --no-cache curl && \
    addgroup -S ratings && \
    adduser -S ratings -G ratings

# Copy the pre-built jar (build locally with: mvn clean package -DskipTests)
COPY target/command-service-*.jar app.jar

# Set ownership and switch to non-root user
RUN chown -R ratings:ratings /app
USER ratings

# Expose ports (gRPC: 9090, HTTP: 8081)
EXPOSE 8081 9090

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8081/actuator/health || exit 1

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseStringDeduplication"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]