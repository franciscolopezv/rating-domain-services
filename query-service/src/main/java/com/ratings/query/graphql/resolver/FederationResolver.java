package com.ratings.query.graphql.resolver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * GraphQL resolver for Apollo Federation support.
 * Provides the _service query required by Apollo Federation Gateway.
 */
@Controller
public class FederationResolver {

    private static final Logger logger = LoggerFactory.getLogger(FederationResolver.class);

    private final ResourceLoader resourceLoader;
    private String cachedSdl;

    @Autowired
    public FederationResolver(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Resolves the _service query for Apollo Federation.
     * Returns the GraphQL Schema Definition Language (SDL) for this service.
     *
     * @return Map containing the SDL
     */
    @QueryMapping(name = "_service")
    public Map<String, String> service() {
        logger.debug("Resolving _service query for Apollo Federation");
        
        try {
            // Load and cache the SDL if not already cached
            if (cachedSdl == null) {
                Resource schemaResource = resourceLoader.getResource("classpath:graphql/schema.graphqls");
                cachedSdl = schemaResource.getContentAsString(StandardCharsets.UTF_8);
                logger.info("Loaded GraphQL schema for federation: {} characters", cachedSdl.length());
            }
            
            return Map.of("sdl", cachedSdl);
            
        } catch (IOException e) {
            logger.error("Error loading GraphQL schema for federation", e);
            // Return empty SDL on error to prevent federation gateway from failing
            return Map.of("sdl", "");
        }
    }

    /**
     * Resolves the _entities query for Apollo Federation.
     * This is called by the federation gateway to resolve entity references.
     *
     * @param representations List of entity representations to resolve
     * @return List of resolved entities
     */
    @QueryMapping(name = "_entities")
    public java.util.List<Object> entities(java.util.List<Map<String, Object>> representations) {
        logger.debug("Resolving _entities query for {} representations", representations.size());
        
        java.util.List<Object> entities = new java.util.ArrayList<>();
        
        for (Map<String, Object> representation : representations) {
            String typename = (String) representation.get("__typename");
            
            if ("Product".equals(typename)) {
                // Extract the product ID from the representation
                String id = (String) representation.get("id");
                logger.debug("Resolving Product entity for ID: {}", id);
                
                // Create a Product entity with the ID
                // The ProductResolver will handle populating the rating fields
                entities.add(new com.ratings.query.graphql.types.Product(id));
            } else {
                logger.warn("Unknown entity type in federation: {}", typename);
                entities.add(null);
            }
        }
        
        return entities;
    }
}