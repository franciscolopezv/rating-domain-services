package com.ratings.query.service;

import com.ratings.query.entity.ProductStats;
import com.ratings.query.graphql.types.OverallRatingStats;
import com.ratings.query.graphql.types.ProductRatingStats;
import com.ratings.query.graphql.types.RatingDistribution;
import com.ratings.query.repository.ProductStatsRepository;
import com.ratings.shared.dto.ProductStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service class for managing product rating statistics.
 * Handles business logic for querying and updating aggregated rating data.
 */
@Service
@Transactional(readOnly = true)
public class ProductStatsService {

    private static final Logger logger = LoggerFactory.getLogger(ProductStatsService.class);

    private final ProductStatsRepository productStatsRepository;
    private final RatingDistributionService ratingDistributionService;
    private final MonitoringService monitoringService;

    @Autowired
    public ProductStatsService(ProductStatsRepository productStatsRepository, 
                              RatingDistributionService ratingDistributionService,
                              MonitoringService monitoringService) {
        this.productStatsRepository = productStatsRepository;
        this.ratingDistributionService = ratingDistributionService;
        this.monitoringService = monitoringService;
    }

    /**
     * Gets product rating statistics by product ID.
     *
     * @param productId the product ID as string
     * @return optional containing the product statistics if found
     */
    public Optional<ProductStatsDto> getProductStats(String productId) {
        logger.debug("Getting product stats for product ID: {}", productId);
        
        long startTime = System.currentTimeMillis();
        
        try {
            UUID uuid = UUID.fromString(productId);
            Optional<ProductStatsDto> result = productStatsRepository.findByProductId(uuid)
                    .map(this::convertToDto);
            
            long executionTime = System.currentTimeMillis() - startTime;
            monitoringService.recordDatabaseOperation("getProductStats", executionTime);
            
            return result;
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for product ID: {}", productId);
            return Optional.empty();
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            monitoringService.recordDatabaseError("getProductStats", e.getClass().getSimpleName());
            throw e;
        }
    }

    /**
     * Gets product rating statistics for GraphQL queries.
     *
     * @param productId the product ID as string
     * @return optional containing the product rating stats if found
     */
    public Optional<ProductRatingStats> getProductRatingStats(String productId) {
        logger.debug("Getting product rating stats for GraphQL query, product ID: {}", productId);
        
        try {
            UUID uuid = UUID.fromString(productId);
            return productStatsRepository.findByProductId(uuid)
                    .map(this::convertToGraphQLType);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for product ID: {}", productId);
            return Optional.empty();
        }
    }

    /**
     * Gets top-rated products limited by count.
     *
     * @param limit the maximum number of products to return
     * @return list of top-rated products
     */
    public List<ProductRatingStats> getTopRatedProducts(int limit) {
        logger.debug("Getting top {} rated products", limit);
        
        return productStatsRepository.findTopRatedProducts(limit)
                .stream()
                .map(this::convertToGraphQLType)
                .collect(Collectors.toList());
    }

    /**
     * Gets most reviewed products limited by count.
     *
     * @param limit the maximum number of products to return
     * @return list of most reviewed products
     */
    public List<ProductRatingStats> getMostReviewedProducts(int limit) {
        logger.debug("Getting top {} most reviewed products", limit);
        
        return productStatsRepository.findMostReviewedProducts(limit)
                .stream()
                .map(this::convertToGraphQLType)
                .collect(Collectors.toList());
    }

    /**
     * Gets overall rating statistics for the system.
     *
     * @return overall rating statistics
     */
    public OverallRatingStats getOverallRatingStats() {
        logger.debug("Getting overall rating statistics");
        
        Long totalProducts = productStatsRepository.count();
        Long totalReviews = productStatsRepository.getTotalReviewCount();
        BigDecimal overallAverageRating = productStatsRepository.getOverallAverageRating();
        Long productsWithRatings = productStatsRepository.countProductsWithRatings();
        
        return new OverallRatingStats(
                totalProducts.intValue(),
                totalReviews,
                overallAverageRating,
                productsWithRatings
        );
    }

    /**
     * Creates or updates product statistics.
     * This method is used by the event projection logic.
     *
     * @param productStatsDto the product statistics to save
     * @return the saved product statistics
     */
    @Transactional
    public ProductStatsDto saveProductStats(ProductStatsDto productStatsDto) {
        logger.debug("Saving product stats for product ID: {}", productStatsDto.getProductId());
        
        UUID uuid = UUID.fromString(productStatsDto.getProductId());
        ProductStats entity = productStatsRepository.findByProductId(uuid)
                .orElse(new ProductStats(uuid));
        
        entity.setAverageRating(productStatsDto.getAverageRating());
        entity.setReviewCount(productStatsDto.getReviewCount());
        entity.setRatingDistribution(productStatsDto.getRatingDistribution());
        
        ProductStats savedEntity = productStatsRepository.save(entity);
        
        logger.info("Saved product stats for product ID: {} with {} reviews and average rating: {}", 
                   savedEntity.getProductId(), savedEntity.getReviewCount(), savedEntity.getAverageRating());
        
        return convertToDto(savedEntity);
    }

    /**
     * Checks if product statistics exist for a given product ID.
     *
     * @param productId the product ID as string
     * @return true if statistics exist
     */
    public boolean hasProductStats(String productId) {
        try {
            UUID uuid = UUID.fromString(productId);
            return productStatsRepository.existsByProductId(uuid);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid UUID format for product ID: {}", productId);
            return false;
        }
    }

    /**
     * Creates a new empty product statistics record.
     *
     * @param productId the product ID as string
     * @return the created product statistics
     */
    @Transactional
    public ProductStatsDto createEmptyProductStats(String productId) {
        logger.debug("Creating empty product stats for product ID: {}", productId);
        
        UUID uuid = UUID.fromString(productId);
        
        if (productStatsRepository.existsByProductId(uuid)) {
            logger.warn("Product stats already exist for product ID: {}", productId);
            return getProductStats(productId).orElse(null);
        }
        
        ProductStats entity = new ProductStats(uuid);
        ProductStats savedEntity = productStatsRepository.save(entity);
        
        logger.info("Created empty product stats for product ID: {}", productId);
        
        return convertToDto(savedEntity);
    }

    /**
     * Converts ProductStats entity to ProductStatsDto.
     *
     * @param entity the entity to convert
     * @return the converted DTO
     */
    private ProductStatsDto convertToDto(ProductStats entity) {
        return new ProductStatsDto(
                entity.getProductId(),
                entity.getAverageRating(),
                entity.getReviewCount(),
                entity.getRatingDistribution()
        );
    }

    /**
     * Converts ProductStats entity to ProductRatingStats GraphQL type.
     *
     * @param entity the entity to convert
     * @return the converted GraphQL type
     */
    private ProductRatingStats convertToGraphQLType(ProductStats entity) {
        RatingDistribution distribution = entity.getRatingDistribution() != null 
                ? ratingDistributionService.createEnhancedRatingDistribution(entity.getRatingDistribution())
                : ratingDistributionService.createEnhancedRatingDistribution(Map.of());
        
        return new ProductRatingStats(
                entity.getProductId(),
                entity.getAverageRating(),
                entity.getReviewCount(),
                distribution,
                entity.getLastUpdated()
        );
    }
}