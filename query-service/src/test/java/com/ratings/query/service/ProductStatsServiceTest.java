package com.ratings.query.service;

import com.ratings.query.entity.ProductStats;
import com.ratings.query.graphql.types.ProductRatingStats;
import com.ratings.query.repository.ProductStatsRepository;
import com.ratings.shared.dto.ProductStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductStatsServiceTest {

    @Mock
    private ProductStatsRepository productStatsRepository;

    @Mock
    private RatingDistributionService ratingDistributionService;

    @Mock
    private MonitoringService monitoringService;

    private ProductStatsService productStatsService;

    @BeforeEach
    void setUp() {
        productStatsService = new ProductStatsService(productStatsRepository, ratingDistributionService, monitoringService);
    }

    @Test
    void getProductStats_WhenProductExists_ReturnsProductStats() {
        // Given
        String productId = "product-1";
        Map<Integer, Integer> distribution = createRatingDistribution();
        ProductStats entity = new ProductStats(productId, BigDecimal.valueOf(4.5), 10, distribution);
        
        when(productStatsRepository.findByProductId(productId)).thenReturn(Optional.of(entity));

        // When
        Optional<ProductStatsDto> result = productStatsService.getProductStats(productId);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getProductId()).isEqualTo(productId);
        assertThat(result.get().getAverageRating()).isEqualTo(BigDecimal.valueOf(4.5));
        assertThat(result.get().getReviewCount()).isEqualTo(10);
        
        verify(monitoringService).recordDatabaseOperation(eq("getProductStats"), anyLong());
    }

    @Test
    void getProductStats_WhenProductNotExists_ReturnsEmpty() {
        // Given
        String productId = "non-existent";
        when(productStatsRepository.findByProductId(productId)).thenReturn(Optional.empty());

        // When
        Optional<ProductStatsDto> result = productStatsService.getProductStats(productId);

        // Then
        assertThat(result).isEmpty();
        verify(monitoringService).recordDatabaseOperation(eq("getProductStats"), anyLong());
    }

    @Test
    void saveProductStats_WhenNewProduct_CreatesNewStats() {
        // Given
        String productId = "new-product";
        Map<Integer, Integer> distribution = createRatingDistribution();
        ProductStatsDto dto = new ProductStatsDto(productId, BigDecimal.valueOf(4.0), 5, distribution);
        
        ProductStats newEntity = new ProductStats(productId);
        ProductStats savedEntity = new ProductStats(productId, BigDecimal.valueOf(4.0), 5, distribution);
        
        when(productStatsRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(productStatsRepository.save(any(ProductStats.class))).thenReturn(savedEntity);

        // When
        ProductStatsDto result = productStatsService.saveProductStats(dto);

        // Then
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getAverageRating()).isEqualTo(BigDecimal.valueOf(4.0));
        assertThat(result.getReviewCount()).isEqualTo(5);
        
        verify(productStatsRepository).save(any(ProductStats.class));
    }

    @Test
    void hasProductStats_WhenProductExists_ReturnsTrue() {
        // Given
        String productId = "existing-product";
        when(productStatsRepository.existsByProductId(productId)).thenReturn(true);

        // When
        boolean result = productStatsService.hasProductStats(productId);

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void hasProductStats_WhenProductNotExists_ReturnsFalse() {
        // Given
        String productId = "non-existent";
        when(productStatsRepository.existsByProductId(productId)).thenReturn(false);

        // When
        boolean result = productStatsService.hasProductStats(productId);

        // Then
        assertThat(result).isFalse();
    }

    @Test
    void createEmptyProductStats_WhenProductNotExists_CreatesEmptyStats() {
        // Given
        String productId = "new-product";
        ProductStats savedEntity = new ProductStats(productId);
        
        when(productStatsRepository.existsByProductId(productId)).thenReturn(false);
        when(productStatsRepository.save(any(ProductStats.class))).thenReturn(savedEntity);

        // When
        ProductStatsDto result = productStatsService.createEmptyProductStats(productId);

        // Then
        assertThat(result.getProductId()).isEqualTo(productId);
        assertThat(result.getReviewCount()).isEqualTo(0);
        
        verify(productStatsRepository).save(any(ProductStats.class));
    }

    private Map<Integer, Integer> createRatingDistribution() {
        Map<Integer, Integer> distribution = new HashMap<>();
        distribution.put(1, 0);
        distribution.put(2, 1);
        distribution.put(3, 2);
        distribution.put(4, 3);
        distribution.put(5, 4);
        return distribution;
    }
}