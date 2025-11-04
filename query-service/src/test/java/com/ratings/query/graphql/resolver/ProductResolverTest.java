package com.ratings.query.graphql.resolver;

import com.ratings.query.graphql.types.Product;
import com.ratings.query.graphql.types.RatingDistribution;
import com.ratings.query.service.ProductStatsService;
import com.ratings.query.service.RatingDistributionService;
import com.ratings.shared.dto.ProductStatsDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductResolverTest {

    @Mock
    private ProductStatsService productStatsService;

    @Mock
    private RatingDistributionService ratingDistributionService;

    private ProductResolver productResolver;

    @BeforeEach
    void setUp() {
        productResolver = new ProductResolver(productStatsService, ratingDistributionService);
    }

    @Test
    void product_ReturnsProductWithId() {
        // Given
        String productId = "product-123";
        Map<String, Object> representation = Map.of("id", productId);

        // When
        Product result = productResolver.product(representation);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(productId);
    }

    @Test
    void averageRating_WhenProductHasRatings_ReturnsAverageRating() {
        // Given
        Product product = new Product("product-1");
        ProductStatsDto stats = new ProductStatsDto("product-1", BigDecimal.valueOf(4.5), 10, createRatingDistribution());
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.of(stats));

        // When
        BigDecimal result = productResolver.averageRating(product);

        // Then
        assertThat(result).isEqualTo(BigDecimal.valueOf(4.5));
    }

    @Test
    void averageRating_WhenProductHasNoRatings_ReturnsNull() {
        // Given
        Product product = new Product("product-1");
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.empty());

        // When
        BigDecimal result = productResolver.averageRating(product);

        // Then
        assertThat(result).isNull();
    }

    @Test
    void reviewCount_WhenProductHasRatings_ReturnsReviewCount() {
        // Given
        Product product = new Product("product-1");
        ProductStatsDto stats = new ProductStatsDto("product-1", BigDecimal.valueOf(4.5), 15, createRatingDistribution());
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.of(stats));

        // When
        Integer result = productResolver.reviewCount(product);

        // Then
        assertThat(result).isEqualTo(15);
    }

    @Test
    void reviewCount_WhenProductHasNoRatings_ReturnsZero() {
        // Given
        Product product = new Product("product-1");
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.empty());

        // When
        Integer result = productResolver.reviewCount(product);

        // Then
        assertThat(result).isEqualTo(0);
    }

    @Test
    void ratingDistribution_WhenProductHasRatings_ReturnsDistribution() {
        // Given
        Product product = new Product("product-1");
        Map<Integer, Integer> distribution = createRatingDistribution();
        ProductStatsDto stats = new ProductStatsDto("product-1", BigDecimal.valueOf(4.5), 10, distribution);
        RatingDistribution expectedDistribution = new RatingDistribution(distribution);
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.of(stats));
        when(ratingDistributionService.createEnhancedRatingDistribution(distribution)).thenReturn(expectedDistribution);

        // When
        RatingDistribution result = productResolver.ratingDistribution(product);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(10);
    }

    @Test
    void ratingDistribution_WhenProductHasNoRatings_ReturnsEmptyDistribution() {
        // Given
        Product product = new Product("product-1");
        RatingDistribution emptyDistribution = new RatingDistribution();
        
        when(productStatsService.getProductStats("product-1")).thenReturn(Optional.empty());
        when(ratingDistributionService.createEnhancedRatingDistribution(any())).thenReturn(emptyDistribution);

        // When
        RatingDistribution result = productResolver.ratingDistribution(product);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(0);
    }

    @Test
    void ratingDistribution_WhenExceptionOccurs_ReturnsEmptyDistribution() {
        // Given
        Product product = new Product("product-1");
        RatingDistribution emptyDistribution = new RatingDistribution();
        
        when(productStatsService.getProductStats("product-1")).thenThrow(new RuntimeException("Database error"));
        lenient().when(ratingDistributionService.createEnhancedRatingDistribution(any(Map.class))).thenReturn(emptyDistribution);

        // When
        RatingDistribution result = productResolver.ratingDistribution(product);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getTotal()).isEqualTo(0);
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