package com.ratings.query.graphql.resolver;

import com.ratings.query.graphql.types.Product;
import com.ratings.query.graphql.types.RatingDistribution;
import com.ratings.query.service.ProductStatsService;
import com.ratings.query.service.RatingDistributionService;
import com.ratings.shared.dto.ProductStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.SchemaMapping;
import org.springframework.stereotype.Controller;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

/**
 * GraphQL resolver for Product type federation.
 * Extends the Product entity with rating information from the ratings domain.
 */
@Controller
public class ProductResolver {

    private static final Logger logger = LoggerFactory.getLogger(ProductResolver.class);

    private final ProductStatsService productStatsService;
    private final RatingDistributionService ratingDistributionService;

    @Autowired
    public ProductResolver(ProductStatsService productStatsService, RatingDistributionService ratingDistributionService) {
        this.productStatsService = productStatsService;
        this.ratingDistributionService = ratingDistributionService;
    }

    /**
     * Resolves the Product entity by ID for federation.
     * This method is called by the GraphQL federation framework to resolve Product references.
     *
     * @param representation the representation containing the product ID
     * @return the Product entity with ID set
     */
    @SchemaMapping(typeName = "Product")
    public Product product(Map<String, Object> representation) {
        String id = (String) representation.get("id");
        logger.debug("Resolving Product entity for federation with ID: {}", id);
        return new Product(id);
    }

    /**
     * Resolves the averageRating field for the Product type.
     *
     * @param product the Product entity
     * @return the average rating, or null if no ratings exist
     */
    @SchemaMapping(typeName = "Product", field = "averageRating")
    public BigDecimal averageRating(Product product) {
        logger.debug("Resolving averageRating for product ID: {}", product.getId());
        
        try {
            Optional<ProductStatsDto> stats = productStatsService.getProductStats(product.getId());
            BigDecimal rating = stats.map(ProductStatsDto::getAverageRating).orElse(null);
            
            logger.debug("Resolved averageRating for product ID {}: {}", product.getId(), rating);
            return rating;
        } catch (Exception e) {
            logger.error("Error resolving averageRating for product ID: {}", product.getId(), e);
            return null;
        }
    }

    /**
     * Resolves the reviewCount field for the Product type.
     *
     * @param product the Product entity
     * @return the review count, or 0 if no reviews exist
     */
    @SchemaMapping(typeName = "Product", field = "reviewCount")
    public Integer reviewCount(Product product) {
        logger.debug("Resolving reviewCount for product ID: {}", product.getId());
        
        try {
            Optional<ProductStatsDto> stats = productStatsService.getProductStats(product.getId());
            Integer count = stats.map(ProductStatsDto::getReviewCount).orElse(0);
            
            logger.debug("Resolved reviewCount for product ID {}: {}", product.getId(), count);
            return count;
        } catch (Exception e) {
            logger.error("Error resolving reviewCount for product ID: {}", product.getId(), e);
            return 0;
        }
    }

    /**
     * Resolves the ratingDistribution field for the Product type.
     *
     * @param product the Product entity
     * @return the rating distribution, or empty distribution if no ratings exist
     */
    @SchemaMapping(typeName = "Product", field = "ratingDistribution")
    public RatingDistribution ratingDistribution(Product product) {
        logger.debug("Resolving ratingDistribution for product ID: {}", product.getId());
        
        try {
            Optional<ProductStatsDto> stats = productStatsService.getProductStats(product.getId());
            
            if (stats.isPresent() && stats.get().getRatingDistribution() != null) {
                Map<Integer, Integer> distribution = stats.get().getRatingDistribution();
                RatingDistribution result = ratingDistributionService.createEnhancedRatingDistribution(distribution);
                
                logger.debug("Resolved ratingDistribution for product ID {}: {}", product.getId(), distribution);
                return result;
            } else {
                logger.debug("No rating distribution found for product ID: {}, returning empty distribution", product.getId());
                return ratingDistributionService.createEnhancedRatingDistribution(Map.of());
            }
        } catch (Exception e) {
            logger.error("Error resolving ratingDistribution for product ID: {}", product.getId(), e);
            return new RatingDistribution();
        }
    }
}