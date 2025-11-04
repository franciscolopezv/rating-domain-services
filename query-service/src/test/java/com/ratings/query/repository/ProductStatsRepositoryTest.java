package com.ratings.query.repository;

import com.ratings.query.entity.ProductStats;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProductStatsRepositoryTest {

    @Autowired
    private ProductStatsRepository productStatsRepository;

    @Test
    void findByProductId_WhenProductExists_ReturnsProductStats() {
        // Given
        ProductStats stats = new ProductStats("product-1");
        stats.setAverageRating(BigDecimal.valueOf(4.5));
        stats.setReviewCount(10);
        productStatsRepository.save(stats);

        // When
        Optional<ProductStats> result = productStatsRepository.findByProductId("product-1");

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo("product-1");
        assertThat(result.get().getAverageRating()).isEqualTo(BigDecimal.valueOf(4.5));
        assertThat(result.get().getReviewCount()).isEqualTo(10);
    }

    @Test
    void findByProductId_WhenProductNotExists_ReturnsEmpty() {
        // When
        Optional<ProductStats> result = productStatsRepository.findByProductId("non-existent");

        // Then
        assertThat(result).isEmpty();
    }

    @Test
    void existsByProductId_WhenProductExists_ReturnsTrue() {
        // Given
        ProductStats stats = new ProductStats("product-1");
        productStatsRepository.save(stats);

        // When
        boolean result = productStatsRepository.existsByProductId("product-1");

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void existsByProductId_WhenProductNotExists_ReturnsFalse() {
        // When
        boolean result = productStatsRepository.existsByProductId("non-existent");

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void save_CreatesNewProductStats() {
        // Given
        ProductStats stats = new ProductStats("new-product");
        stats.setAverageRating(BigDecimal.valueOf(3.5));
        stats.setReviewCount(5);

        // When
        ProductStats saved = productStatsRepository.save(stats);

        // Then
        assertThat(saved.getProductId()).isEqualTo("new-product");
        assertThat(saved.getAverageRating()).isEqualTo(BigDecimal.valueOf(3.5));
        assertThat(saved.getReviewCount()).isEqualTo(5);
        assertThat(saved.getLastUpdated()).isNotNull();
    }
}