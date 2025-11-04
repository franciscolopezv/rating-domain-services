@echo off
REM Build script for Ratings System using Maven wrapper (Windows)
REM This script demonstrates how to build the project without requiring a local Maven installation

echo 🚀 Building Ratings System using Maven wrapper...
echo ==================================================

REM Check Java version
echo 📋 Checking Java version...
java -version

REM Clean and compile
echo 🧹 Cleaning and compiling...
mvnw.cmd clean compile

REM Run tests (optional - can be skipped with --skip-tests)
if "%1"=="--skip-tests" (
    echo ⏭️  Skipping tests...
) else (
    echo 🧪 Running tests...
    mvnw.cmd test
)

REM Package applications
echo 📦 Packaging applications...
mvnw.cmd package -DskipTests

echo ✅ Build completed successfully!
echo.
echo 📁 Generated artifacts:
echo   - command-service/target/command-service-*.jar
echo   - query-service/target/query-service-*.jar
echo   - shared/target/shared-*.jar
echo.
echo 🐳 To build Docker images:
echo   docker compose build
echo.
echo 🚀 To start the system:
echo   docker compose up -d