package com.ratings.query.graphql.resolver;

import com.ratings.query.graphql.types.OverallRatingStats;
import com.ratings.query.graphql.types.ProductRatingStats;
import com.ratings.query.service.ProductStatsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Optional;

/**
 * GraphQL resolver for Query type operations.
 * Handles direct queries for rating statistics.
 */
@Controller
public class QueryResolver {

    private static final Logger logger = LoggerFactory.getLogger(QueryResolver.class);

    private final ProductStatsService productStatsService;

    @Autowired
    public QueryResolver(ProductStatsService productStatsService) {
        this.productStatsService = productStatsService;
    }



    /**
     * Resolves the productRatingStats query.
     *
     * @param productId the product ID
     * @return the product rating statistics, or null if not found
     */
    @QueryMapping
    public ProductRatingStats productRatingStats(@Argument("productId") String productId) {
        logger.debug("Resolving productRatingStats query for product ID: {}", productId);
        
        try {
            Optional<ProductRatingStats> stats = productStatsService.getProductRatingStats(productId);
            
            if (stats.isPresent()) {
                logger.debug("Found rating stats for product ID: {}", productId);
                return stats.get();
            } else {
                logger.debug("No rating stats found for product ID: {}", productId);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error resolving productRatingStats for product ID: {}", productId, e);
            return null;
        }
    }

    /**
     * Resolves the topRatedProducts query.
     *
     * @param limit the maximum number of products to return (default: 10)
     * @return list of top-rated products
     */
    @QueryMapping
    public List<ProductRatingStats> topRatedProducts(@Argument("limit") Integer limit) {
        logger.debug("Resolving topRatedProducts query with limit: {}", limit);
        
        try {
            // Validate limit
            int validLimit = Math.max(1, Math.min(limit != null ? limit : 10, 100));
            
            List<ProductRatingStats> products = productStatsService.getTopRatedProducts(validLimit);
            
            logger.debug("Found {} top-rated products", products.size());
            return products;
        } catch (Exception e) {
            logger.error("Error resolving topRatedProducts query", e);
            return List.of();
        }
    }

    /**
     * Resolves the mostReviewedProducts query.
     *
     * @param limit the maximum number of products to return (default: 10)
     * @return list of most reviewed products
     */
    @QueryMapping
    public List<ProductRatingStats> mostReviewedProducts(@Argument("limit") Integer limit) {
        logger.debug("Resolving mostReviewedProducts query with limit: {}", limit);
        
        try {
            // Validate limit
            int validLimit = Math.max(1, Math.min(limit != null ? limit : 10, 100));
            
            List<ProductRatingStats> products = productStatsService.getMostReviewedProducts(validLimit);
            
            logger.debug("Found {} most reviewed products", products.size());
            return products;
        } catch (Exception e) {
            logger.error("Error resolving mostReviewedProducts query", e);
            return List.of();
        }
    }

    /**
     * Resolves the overallRatingStats query.
     *
     * @return overall rating statistics for the system
     */
    @QueryMapping
    public OverallRatingStats overallRatingStats() {
        logger.debug("Resolving overallRatingStats query");
        
        try {
            OverallRatingStats stats = productStatsService.getOverallRatingStats();
            
            logger.debug("Resolved overall rating stats: {} total products, {} total reviews", 
                        stats.getTotalProducts(), stats.getTotalReviews());
            return stats;
        } catch (Exception e) {
            logger.error("Error resolving overallRatingStats query", e);
            return new OverallRatingStats(0, 0L, null, 0L);
        }
    }

    /**
     * Query to get a product by ID (for federation and direct queries).
     * This creates a Product entity that will be populated with rating information.
     */
    @QueryMapping
    public com.ratings.query.graphql.types.Product product(@Argument("id") String id) {
        logger.debug("Resolving product query for ID: {}", id);
        return new com.ratings.query.graphql.types.Product(id);
    }
}