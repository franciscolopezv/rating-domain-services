package com.ratings.command.repository;

import com.ratings.command.entity.Review;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class ReviewRepositoryTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("test_ratings")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }
    
    @Autowired
    private TestEntityManager entityManager;
    
    @Autowired
    private ReviewRepository reviewRepository;
    
    @Test
    void save_ValidReview_Success() {
        // Arrange
        Review review = new Review("product-123", 5, "user-456", "Great product!");
        
        // Act
        Review savedReview = reviewRepository.save(review);
        
        // Assert
        assertNotNull(savedReview.getReviewId());
        assertEquals("product-123", savedReview.getProductId());
        assertEquals(5, savedReview.getRating());
        assertEquals("user-456", savedReview.getUserId());
        assertEquals("Great product!", savedReview.getReviewText());
        assertNotNull(savedReview.getCreatedAt());
        assertNotNull(savedReview.getUpdatedAt());
    }
    
    @Test
    void findByProductId_ExistingProduct_ReturnsReviews() {
        // Arrange
        Review review1 = new Review("product-123", 5, "user-1", "Great!");
        Review review2 = new Review("product-123", 4, "user-2", "Good!");
        Review review3 = new Review("product-456", 3, "user-3", "OK!");
        
        entityManager.persistAndFlush(review1);
        entityManager.persistAndFlush(review2);
        entityManager.persistAndFlush(review3);
        
        // Act
        List<Review> reviews = reviewRepository.findByProductId("product-123");
        
        // Assert
        assertEquals(2, reviews.size());
        assertTrue(reviews.stream().allMatch(r -> "product-123".equals(r.getProductId())));
    }
    
    @Test
    void findByUserId_ExistingUser_ReturnsReviews() {
        // Arrange
        Review review1 = new Review("product-123", 5, "user-1", "Great!");
        Review review2 = new Review("product-456", 4, "user-1", "Good!");
        Review review3 = new Review("product-789", 3, "user-2", "OK!");
        
        entityManager.persistAndFlush(review1);
        entityManager.persistAndFlush(review2);
        entityManager.persistAndFlush(review3);
        
        // Act
        List<Review> reviews = reviewRepository.findByUserId("user-1");
        
        // Assert
        assertEquals(2, reviews.size());
        assertTrue(reviews.stream().allMatch(r -> "user-1".equals(r.getUserId())));
    }
    
    @Test
    void countByProductId_ExistingProduct_ReturnsCorrectCount() {
        // Arrange
        Review review1 = new Review("product-123", 5, "user-1", "Great!");
        Review review2 = new Review("product-123", 4, "user-2", "Good!");
        Review review3 = new Review("product-456", 3, "user-3", "OK!");
        
        entityManager.persistAndFlush(review1);
        entityManager.persistAndFlush(review2);
        entityManager.persistAndFlush(review3);
        
        // Act
        long count = reviewRepository.countByProductId("product-123");
        
        // Assert
        assertEquals(2, count);
    }
    
    @Test
    void findFirstByProductIdOrderByCreatedAtDesc_ExistingProduct_ReturnsMostRecent() {
        // Arrange
        Review oldReview = new Review("product-123", 3, "user-1", "Old review");
        entityManager.persistAndFlush(oldReview);
        
        // Wait a bit to ensure different timestamps
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        Review newReview = new Review("product-123", 5, "user-2", "New review");
        entityManager.persistAndFlush(newReview);
        
        // Act
        Optional<Review> mostRecent = reviewRepository.findFirstByProductIdOrderByCreatedAtDesc("product-123");
        
        // Assert
        assertTrue(mostRecent.isPresent());
        assertEquals("New review", mostRecent.get().getReviewText());
        assertEquals("user-2", mostRecent.get().getUserId());
    }
    
    @Test
    void existsByProductIdAndUserId_ExistingCombination_ReturnsTrue() {
        // Arrange
        Review review = new Review("product-123", 5, "user-456", "Great!");
        entityManager.persistAndFlush(review);
        
        // Act
        boolean exists = reviewRepository.existsByProductIdAndUserId("product-123", "user-456");
        
        // Assert
        assertTrue(exists);
    }
    
    @Test
    void existsByProductIdAndUserId_NonExistingCombination_ReturnsFalse() {
        // Arrange
        Review review = new Review("product-123", 5, "user-456", "Great!");
        entityManager.persistAndFlush(review);
        
        // Act
        boolean exists = reviewRepository.existsByProductIdAndUserId("product-123", "user-999");
        
        // Assert
        assertFalse(exists);
    }
    
    @Test
    void findByProductIdAndCreatedAtBetween_WithinDateRange_ReturnsReviews() {
        // Arrange
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant tomorrow = now.plus(1, ChronoUnit.DAYS);
        
        Review review1 = new Review("product-123", 5, "user-1", "Recent review");
        Review review2 = new Review("product-123", 4, "user-2", "Another recent review");
        
        entityManager.persistAndFlush(review1);
        entityManager.persistAndFlush(review2);
        
        // Act
        List<Review> reviews = reviewRepository.findByProductIdAndCreatedAtBetween(
            "product-123", yesterday, tomorrow);
        
        // Assert
        assertEquals(2, reviews.size());
        assertTrue(reviews.stream().allMatch(r -> "product-123".equals(r.getProductId())));
    }
}