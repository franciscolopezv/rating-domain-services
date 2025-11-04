#!/bin/bash

# Build script for Ratings System using Maven wrapper
# This script demonstrates how to build the project without requiring a local Maven installation

set -e

echo "🚀 Building Ratings System using Maven wrapper..."
echo "=================================================="

# Check Java version
echo "📋 Checking Java version..."
java -version

# Clean and compile
echo "🧹 Cleaning and compiling..."
./mvnw clean compile

# Run tests (optional - can be skipped with -DskipTests)
if [[ "$1" != "--skip-tests" ]]; then
    echo "🧪 Running tests..."
    ./mvnw test
else
    echo "⏭️  Skipping tests..."
fi

# Package applications
echo "📦 Packaging applications..."
./mvnw package -DskipTests

echo "✅ Build completed successfully!"
echo ""
echo "📁 Generated artifacts:"
echo "  - command-service/target/command-service-*.jar"
echo "  - query-service/target/query-service-*.jar"
echo "  - shared/target/shared-*.jar"
echo ""
echo "🐳 To build Docker images:"
echo "  docker compose build"
echo ""
echo "🚀 To start the system:"
echo "  docker compose up -d"