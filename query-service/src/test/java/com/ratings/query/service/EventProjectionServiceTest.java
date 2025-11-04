package com.ratings.query.service;

import com.ratings.query.entity.ProductStats;
import com.ratings.query.exception.DatabaseException;
import com.ratings.query.exception.EventProjectionException;
import com.ratings.query.repository.ProductStatsRepository;
import com.ratings.shared.events.RatingSubmittedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventProjectionServiceTest {

    @Mock
    private ProductStatsRepository productStatsRepository;

    @Mock
    private MonitoringService monitoringService;

    private EventProjectionService eventProjectionService;

    @BeforeEach
    void setUp() {
        eventProjectionService = new EventProjectionService(productStatsRepository, monitoringService);
    }

    @Test
    void projectRatingSubmittedEvent_WhenNewProduct_CreatesNewStats() {
        // Given
        RatingSubmittedEvent event = createRatingEvent("product-1", 5);
        ProductStats savedStats = new ProductStats("product-1");
        
        when(productStatsRepository.findByProductId("product-1")).thenReturn(Optional.empty());
        when(productStatsRepository.save(any(ProductStats.class))).thenReturn(savedStats);

        // When
        eventProjectionService.projectRatingSubmittedEvent(event);

        // Then
        verify(productStatsRepository).findByProductId("product-1");
        verify(productStatsRepository).save(any(ProductStats.class));
        verify(monitoringService).recordKafkaEvent(eq("RatingSubmittedEvent"), anyLong());
    }

    @Test
    void projectRatingSubmittedEvent_WhenExistingProduct_UpdatesStats() {
        // Given
        RatingSubmittedEvent event = createRatingEvent("product-1", 4);
        
        Map<Integer, Integer> existingDistribution = new HashMap<>();
        existingDistribution.put(1, 0);
        existingDistribution.put(2, 0);
        existingDistribution.put(3, 0);
        existingDistribution.put(4, 1);
        existingDistribution.put(5, 2);
        
        ProductStats existingStats = new ProductStats("product-1", BigDecimal.valueOf(4.67), 3, existingDistribution);
        ProductStats savedStats = new ProductStats("product-1");
        
        when(productStatsRepository.findByProductId("product-1")).thenReturn(Optional.of(existingStats));
        when(productStatsRepository.save(any(ProductStats.class))).thenReturn(savedStats);

        // When
        eventProjectionService.projectRatingSubmittedEvent(event);

        // Then
        verify(productStatsRepository).save(any(ProductStats.class));
        verify(monitoringService).recordKafkaEvent(eq("RatingSubmittedEvent"), anyLong());
    }

    @Test
    void projectRatingSubmittedEvent_WhenDatabaseError_ThrowsEventProjectionException() {
        // Given
        RatingSubmittedEvent event = createRatingEvent("product-1", 3);
        
        when(productStatsRepository.findByProductId("product-1")).thenReturn(Optional.empty());
        when(productStatsRepository.save(any(ProductStats.class))).thenThrow(new DataAccessException("Database error") {});

        // When & Then
        assertThatThrownBy(() -> eventProjectionService.projectRatingSubmittedEvent(event))
                .isInstanceOf(DatabaseException.class)
                .hasMessageContaining("Database error during event projection");
        
        verify(monitoringService).recordKafkaEventError(eq("RatingSubmittedEvent"), anyString());
    }

    @Test
    void recalculateProductStats_WhenProductNotExists_CreatesEmptyStats() {
        // Given
        String productId = "new-product";
        ProductStats newStats = new ProductStats(productId);
        
        when(productStatsRepository.findByProductId(productId)).thenReturn(Optional.empty());
        when(productStatsRepository.save(any(ProductStats.class))).thenReturn(newStats);

        // When
        eventProjectionService.recalculateProductStats(productId);

        // Then
        verify(productStatsRepository).save(any(ProductStats.class));
    }

    @Test
    void recalculateProductStats_WhenProductExists_DoesNothing() {
        // Given
        String productId = "existing-product";
        ProductStats existingStats = new ProductStats(productId);
        
        when(productStatsRepository.findByProductId(productId)).thenReturn(Optional.of(existingStats));

        // When
        eventProjectionService.recalculateProductStats(productId);

        // Then
        verify(productStatsRepository, never()).save(any(ProductStats.class));
    }

    private RatingSubmittedEvent createRatingEvent(String productId, Integer rating) {
        return RatingSubmittedEvent.create(
                "submission-" + System.currentTimeMillis(),
                productId,
                rating,
                "user-123"
        );
    }
}