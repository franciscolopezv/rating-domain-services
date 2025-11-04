package com.ratings.command.integration;

import com.ratings.command.entity.Review;
import com.ratings.command.repository.ReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
@ActiveProfiles("test")
class CommandServiceIntegrationTest {
    
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
    private ReviewRepository reviewRepository;
    
    @Test
    void testDatabaseIntegration_SaveAndRetrieveReview() {
        // Arrange
        Review review = new Review("integration-test-product", 4, "integration-test-user", "Integration test review");
        
        // Act
        Review savedReview = reviewRepository.save(review);
        
        // Assert
        assertNotNull(savedReview.getReviewId());
        assertEquals("integration-test-product", savedReview.getProductId());
        assertEquals(4, savedReview.getRating());
        assertEquals("integration-test-user", savedReview.getUserId());
        assertEquals("Integration test review", savedReview.getReviewText());
        assertNotNull(savedReview.getCreatedAt());
        assertNotNull(savedReview.getUpdatedAt());
        
        // Verify data can be retrieved
        Optional<Review> retrievedReview = reviewRepository.findById(savedReview.getReviewId());
        assertTrue(retrievedReview.isPresent());
        assertEquals(savedReview.getReviewId(), retrievedReview.get().getReviewId());
    }
    
    @Test
    void testDatabaseIntegration_MultipleReviewsForSameProduct() {
        // Arrange
        String productId = "multi-rating-product";
        Review review1 = new Review(productId, 5, "user-1", "Great product!");
        Review review2 = new Review(productId, 3, "user-2", "Average product");
        
        // Act
        reviewRepository.save(review1);
        reviewRepository.save(review2);
        
        // Assert
        List<Review> reviews = reviewRepository.findByProductId(productId);
        assertEquals(2, reviews.size());
        
        assertTrue(reviews.stream().anyMatch(r -> r.getRating().equals(5) && "user-1".equals(r.getUserId())));
        assertTrue(reviews.stream().anyMatch(r -> r.getRating().equals(3) && "user-2".equals(r.getUserId())));
    }
    
    @Test
    void testDatabaseIntegration_MinimalReview() {
        // Arrange
        Review review = new Review("minimal-test-product", 1, null, null);
        
        // Act
        Review savedReview = reviewRepository.save(review);
        
        // Assert
        assertNotNull(savedReview.getReviewId());
        assertEquals("minimal-test-product", savedReview.getProductId());
        assertEquals(1, savedReview.getRating());
        assertNull(savedReview.getUserId());
        assertNull(savedReview.getReviewText());
    }
}