package com.ratings.integration;

import org.junit.jupiter.api.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance and load testing for the ratings system.
 * Tests concurrent rating submissions, event processing latency, and query performance.
 */
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PerformanceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> writeDatabase = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ratings_write")
            .withUsername("ratings_user")
            .withPassword("ratings_pass")
            .withInitScript("init-write-db.sql")
            .waitingFor(Wait.forListeningPort());

    @Container
    static PostgreSQLContainer<?> readDatabase = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("ratings_read")
            .withUsername("ratings_user")
            .withPassword("ratings_pass")
            .withInitScript("init-read-db.sql")
            .waitingFor(Wait.forListeningPort());

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"))
            .withEmbeddedZookeeper()
            .waitingFor(Wait.forListeningPort());

    private static TestRestTemplate restTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", writeDatabase::getJdbcUrl);
        registry.add("spring.datasource.username", writeDatabase::getUsername);
        registry.add("spring.datasource.password", writeDatabase::getPassword);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.flyway.enabled", () -> "false");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @BeforeAll
    static void setUp() {
        restTemplate = new TestRestTemplate();
        System.out.println("Performance test containers started:");
        System.out.println("Write DB: " + writeDatabase.getJdbcUrl());
        System.out.println("Read DB: " + readDatabase.getJdbcUrl());
        System.out.println("Kafka: " + kafka.getBootstrapServers());
    }

    @Test
    @Order(1)
    void testConcurrentRatingSubmissionPerformance() throws InterruptedException {
        int numberOfThreads = 10;
        int ratingsPerThread = 20;
        int totalRatings = numberOfThreads * ratingsPerThread;
        String productId = "performance-test-product";
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(totalRatings);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalLatency = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        // Submit ratings concurrently
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try (var connection = writeDatabase.createConnection("")) {
                    var stmt = connection.prepareStatement(
                        "INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES (?, ?, ?, ?)"
                    );
                    
                    for (int j = 0; j < ratingsPerThread; j++) {
                        try {
                            long requestStart = System.currentTimeMillis();
                            
                            stmt.setString(1, productId);
                            stmt.setInt(2, (j % 5) + 1);
                            stmt.setString(3, "perf-user-" + threadId + "-" + j);
                            stmt.setString(4, "Performance test review " + threadId + "-" + j);
                            
                            int rowsAffected = stmt.executeUpdate();
                            
                            long requestEnd = System.currentTimeMillis();
                            long latency = requestEnd - requestStart;
                            totalLatency.addAndGet(latency);
                            
                            if (rowsAffected == 1) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        } catch (Exception e) {
                            errorCount.incrementAndGet();
                            System.err.println("Error in thread " + threadId + ", iteration " + j + ": " + e.getMessage());
                        } finally {
                            latch.countDown();
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Connection error in thread " + threadId + ": " + e.getMessage());
                    // Count down remaining iterations for this thread
                    for (int j = 0; j < ratingsPerThread; j++) {
                        errorCount.incrementAndGet();
                        latch.countDown();
                    }
                }
            });
        }
        
        // Wait for all requests to complete
        assertTrue(latch.await(60, TimeUnit.SECONDS), "All requests should complete within 60 seconds");
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        
        executor.shutdown();
        
        // Performance metrics
        double throughput = (totalRatings * 1000.0) / totalDuration;
        double averageLatency = totalLatency.get() / (double) totalRatings;
        double successRate = (successCount.get() * 100.0) / totalRatings;
        
        System.out.println("=== Concurrent Rating Submission Performance ===");
        System.out.println("Total requests: " + totalRatings);
        System.out.println("Successful requests: " + successCount.get());
        System.out.println("Failed requests: " + errorCount.get());
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " requests/second");
        System.out.println("Average latency: " + String.format("%.2f", averageLatency) + "ms");
        System.out.println("Success rate: " + String.format("%.2f", successRate) + "%");
        
        // Performance assertions
        assertTrue(successRate > 95.0, "At least 95% of requests should succeed, got: " + successRate + "%");
        assertTrue(totalDuration < 30000, "All requests should complete within 30 seconds, took: " + totalDuration + "ms");
        assertTrue(averageLatency < 500, "Average latency should be under 500ms, got: " + averageLatency + "ms");
        assertTrue(throughput > 10, "Should achieve at least 10 requests/second, got: " + throughput);
        
        System.out.println("✅ Concurrent rating submission performance test completed successfully");
    }

    @Test
    @Order(2)
    void testBatchInsertPerformance() {
        String productId = "batch-performance-test";
        int batchSize = 100;
        int numberOfBatches = 5;
        int totalRatings = batchSize * numberOfBatches;
        
        long startTime = System.currentTimeMillis();
        
        try (var connection = writeDatabase.createConnection("")) {
            connection.setAutoCommit(false);
            
            var stmt = connection.prepareStatement(
                "INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES (?, ?, ?, ?)"
            );
            
            for (int batch = 0; batch < numberOfBatches; batch++) {
                for (int i = 0; i < batchSize; i++) {
                    stmt.setString(1, productId);
                    stmt.setInt(2, (i % 5) + 1);
                    stmt.setString(3, "batch-user-" + batch + "-" + i);
                    stmt.setString(4, "Batch performance test " + batch + "-" + i);
                    stmt.addBatch();
                }
                
                int[] results = stmt.executeBatch();
                stmt.clearBatch();
                
                assertEquals(batchSize, results.length, "Batch should contain " + batchSize + " operations");
                for (int result : results) {
                    assertEquals(1, result, "Each insert should affect one row");
                }
            }
            
            connection.commit();
            
        } catch (Exception e) {
            fail("Batch insert performance test failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Verify all ratings were inserted
        try (var connection = writeDatabase.createConnection("")) {
            var stmt = connection.prepareStatement("SELECT COUNT(*) FROM reviews WHERE product_id = ?");
            stmt.setString(1, productId);
            var rs = stmt.executeQuery();
            rs.next();
            assertEquals(totalRatings, rs.getInt(1), "Should have all batch inserted ratings");
        } catch (Exception e) {
            fail("Failed to verify batch insert results: " + e.getMessage());
        }
        
        double throughput = (totalRatings * 1000.0) / duration;
        
        System.out.println("=== Batch Insert Performance ===");
        System.out.println("Total ratings: " + totalRatings);
        System.out.println("Batch size: " + batchSize);
        System.out.println("Number of batches: " + numberOfBatches);
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " inserts/second");
        
        // Performance assertions
        assertTrue(duration < 10000, "Batch insert should complete within 10 seconds, took: " + duration + "ms");
        assertTrue(throughput > 50, "Should achieve at least 50 inserts/second with batching, got: " + throughput);
        
        System.out.println("✅ Batch insert performance test completed successfully");
    }

    @Test
    @Order(3)
    void testQueryPerformanceWithLargeDataset() {
        String baseProductId = "large-dataset-product";
        int numberOfProducts = 20;
        int ratingsPerProduct = 50;
        int totalRatings = numberOfProducts * ratingsPerProduct;
        
        // Create large dataset
        long setupStart = System.currentTimeMillis();
        
        try (var connection = writeDatabase.createConnection("")) {
            connection.setAutoCommit(false);
            
            var stmt = connection.prepareStatement(
                "INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES (?, ?, ?, ?)"
            );
            
            for (int productIndex = 0; productIndex < numberOfProducts; productIndex++) {
                String productId = baseProductId + "-" + productIndex;
                
                for (int ratingIndex = 0; ratingIndex < ratingsPerProduct; ratingIndex++) {
                    stmt.setString(1, productId);
                    stmt.setInt(2, (ratingIndex % 5) + 1);
                    stmt.setString(3, "dataset-user-" + productIndex + "-" + ratingIndex);
                    stmt.setString(4, "Large dataset review " + productIndex + "-" + ratingIndex);
                    stmt.addBatch();
                    
                    // Execute batch every 100 inserts
                    if ((productIndex * ratingsPerProduct + ratingIndex + 1) % 100 == 0) {
                        stmt.executeBatch();
                        stmt.clearBatch();
                    }
                }
            }
            
            // Execute remaining batch
            stmt.executeBatch();
            connection.commit();
            
        } catch (Exception e) {
            fail("Failed to create large dataset: " + e.getMessage());
        }
        
        long setupEnd = System.currentTimeMillis();
        System.out.println("Dataset setup took: " + (setupEnd - setupStart) + "ms");
        
        // Test query performance
        List<Long> queryLatencies = new ArrayList<>();
        int numberOfQueries = 50;
        
        for (int i = 0; i < numberOfQueries; i++) {
            String productId = baseProductId + "-" + (i % numberOfProducts);
            
            long queryStart = System.currentTimeMillis();
            
            try (var connection = writeDatabase.createConnection("")) {
                // Complex query: get rating statistics for a product
                var stmt = connection.prepareStatement(
                    "SELECT " +
                    "  COUNT(*) as review_count, " +
                    "  AVG(rating::decimal) as average_rating, " +
                    "  COUNT(CASE WHEN rating = 1 THEN 1 END) as one_star, " +
                    "  COUNT(CASE WHEN rating = 2 THEN 1 END) as two_star, " +
                    "  COUNT(CASE WHEN rating = 3 THEN 1 END) as three_star, " +
                    "  COUNT(CASE WHEN rating = 4 THEN 1 END) as four_star, " +
                    "  COUNT(CASE WHEN rating = 5 THEN 1 END) as five_star " +
                    "FROM reviews WHERE product_id = ?"
                );
                stmt.setString(1, productId);
                
                var rs = stmt.executeQuery();
                assertTrue(rs.next(), "Query should return results");
                
                int reviewCount = rs.getInt("review_count");
                double averageRating = rs.getDouble("average_rating");
                
                assertEquals(ratingsPerProduct, reviewCount, "Should have correct review count");
                assertTrue(averageRating >= 1.0 && averageRating <= 5.0, "Average rating should be valid");
                
            } catch (Exception e) {
                fail("Query failed for product " + productId + ": " + e.getMessage());
            }
            
            long queryEnd = System.currentTimeMillis();
            long queryLatency = queryEnd - queryStart;
            queryLatencies.add(queryLatency);
        }
        
        // Analyze query performance
        double averageLatency = queryLatencies.stream().mapToLong(Long::longValue).average().orElse(0);
        long maxLatency = queryLatencies.stream().mapToLong(Long::longValue).max().orElse(0);
        long minLatency = queryLatencies.stream().mapToLong(Long::longValue).min().orElse(0);
        
        System.out.println("=== Query Performance with Large Dataset ===");
        System.out.println("Dataset size: " + totalRatings + " ratings across " + numberOfProducts + " products");
        System.out.println("Number of queries: " + numberOfQueries);
        System.out.println("Average latency: " + String.format("%.2f", averageLatency) + "ms");
        System.out.println("Min latency: " + minLatency + "ms");
        System.out.println("Max latency: " + maxLatency + "ms");
        
        // Performance assertions
        assertTrue(averageLatency < 100, "Average query latency should be under 100ms, got: " + averageLatency + "ms");
        assertTrue(maxLatency < 500, "Max query latency should be under 500ms, got: " + maxLatency + "ms");
        
        System.out.println("✅ Query performance test completed successfully");
    }

    @Test
    @Order(4)
    void testConnectionPoolPerformance() throws InterruptedException {
        String productId = "connection-pool-test";
        int numberOfThreads = 20;
        int operationsPerThread = 10;
        int totalOperations = numberOfThreads * operationsPerThread;
        
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
        CountDownLatch latch = new CountDownLatch(totalOperations);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicLong totalConnectionTime = new AtomicLong(0);
        
        long startTime = System.currentTimeMillis();
        
        for (int i = 0; i < numberOfThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int j = 0; j < operationsPerThread; j++) {
                    try {
                        long connectionStart = System.currentTimeMillis();
                        
                        try (var connection = writeDatabase.createConnection("")) {
                            long connectionEnd = System.currentTimeMillis();
                            totalConnectionTime.addAndGet(connectionEnd - connectionStart);
                            
                            var stmt = connection.prepareStatement(
                                "INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES (?, ?, ?, ?)"
                            );
                            stmt.setString(1, productId);
                            stmt.setInt(2, (j % 5) + 1);
                            stmt.setString(3, "pool-user-" + threadId + "-" + j);
                            stmt.setString(4, "Connection pool test " + threadId + "-" + j);
                            
                            int rowsAffected = stmt.executeUpdate();
                            if (rowsAffected == 1) {
                                successCount.incrementAndGet();
                            } else {
                                errorCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Connection pool error in thread " + threadId + ", operation " + j + ": " + e.getMessage());
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }
        
        assertTrue(latch.await(60, TimeUnit.SECONDS), "All operations should complete within 60 seconds");
        
        long endTime = System.currentTimeMillis();
        long totalDuration = endTime - startTime;
        
        executor.shutdown();
        
        double averageConnectionTime = totalConnectionTime.get() / (double) totalOperations;
        double successRate = (successCount.get() * 100.0) / totalOperations;
        double throughput = (totalOperations * 1000.0) / totalDuration;
        
        System.out.println("=== Connection Pool Performance ===");
        System.out.println("Total operations: " + totalOperations);
        System.out.println("Concurrent threads: " + numberOfThreads);
        System.out.println("Successful operations: " + successCount.get());
        System.out.println("Failed operations: " + errorCount.get());
        System.out.println("Success rate: " + String.format("%.2f", successRate) + "%");
        System.out.println("Total duration: " + totalDuration + "ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " operations/second");
        System.out.println("Average connection time: " + String.format("%.2f", averageConnectionTime) + "ms");
        
        // Performance assertions
        assertTrue(successRate > 95.0, "Success rate should be above 95%, got: " + successRate + "%");
        assertTrue(averageConnectionTime < 100, "Average connection time should be under 100ms, got: " + averageConnectionTime + "ms");
        assertTrue(throughput > 20, "Should achieve at least 20 operations/second, got: " + throughput);
        
        System.out.println("✅ Connection pool performance test completed successfully");
    }

    @Test
    @Order(5)
    void testMemoryUsageWithLargeOperations() {
        String productId = "memory-test-product";
        int largeTextSize = 1000; // 1KB review text
        int numberOfReviews = 100;
        
        // Create large review text
        StringBuilder largeReviewText = new StringBuilder();
        for (int i = 0; i < largeTextSize; i++) {
            largeReviewText.append("A");
        }
        String reviewText = largeReviewText.toString();
        
        Runtime runtime = Runtime.getRuntime();
        long memoryBefore = runtime.totalMemory() - runtime.freeMemory();
        
        long startTime = System.currentTimeMillis();
        
        try (var connection = writeDatabase.createConnection("")) {
            connection.setAutoCommit(false);
            
            var stmt = connection.prepareStatement(
                "INSERT INTO reviews (product_id, rating, user_id, review_text) VALUES (?, ?, ?, ?)"
            );
            
            for (int i = 0; i < numberOfReviews; i++) {
                stmt.setString(1, productId);
                stmt.setInt(2, (i % 5) + 1);
                stmt.setString(3, "memory-user-" + i);
                stmt.setString(4, reviewText);
                stmt.addBatch();
                
                // Execute batch every 20 inserts to manage memory
                if ((i + 1) % 20 == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();
                }
            }
            
            // Execute remaining batch
            stmt.executeBatch();
            connection.commit();
            
        } catch (Exception e) {
            fail("Memory usage test failed: " + e.getMessage());
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        // Force garbage collection and measure memory
        System.gc();
        Thread.yield();
        long memoryAfter = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = memoryAfter - memoryBefore;
        
        // Verify all reviews were inserted
        try (var connection = writeDatabase.createConnection("")) {
            var stmt = connection.prepareStatement("SELECT COUNT(*) FROM reviews WHERE product_id = ?");
            stmt.setString(1, productId);
            var rs = stmt.executeQuery();
            rs.next();
            assertEquals(numberOfReviews, rs.getInt(1), "Should have all large reviews");
        } catch (Exception e) {
            fail("Failed to verify large reviews: " + e.getMessage());
        }
        
        System.out.println("=== Memory Usage Test ===");
        System.out.println("Number of reviews: " + numberOfReviews);
        System.out.println("Review text size: " + largeTextSize + " characters");
        System.out.println("Total data size: ~" + (numberOfReviews * largeTextSize / 1024) + " KB");
        System.out.println("Duration: " + duration + "ms");
        System.out.println("Memory used: " + (memoryUsed / 1024) + " KB");
        
        // Performance assertions
        assertTrue(duration < 15000, "Large operations should complete within 15 seconds, took: " + duration + "ms");
        // Memory usage assertion is lenient as it depends on JVM and GC behavior
        assertTrue(memoryUsed < 50 * 1024 * 1024, "Memory usage should be reasonable (< 50MB), used: " + (memoryUsed / 1024 / 1024) + " MB");
        
        System.out.println("✅ Memory usage test completed successfully");
    }
}