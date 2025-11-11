package com.ratings.query.config;

import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;

import java.util.Map;

/**
 * Configuration for GraphQL setup including federation support.
 */
@Configuration
public class GraphQLConfig {
    
    /**
     * Register the _Any scalar required for Apollo Federation.
     * The _Any scalar is used to represent entity representations in federation queries.
     */
    @Bean
    public RuntimeWiringConfigurer runtimeWiringConfigurer() {
        return wiringBuilder -> wiringBuilder
                .scalar(GraphQLScalarType.newScalar()
                        .name("_Any")
                        .description("Federation _Any scalar for entity representations")
                        .coercing(new Coercing<Map<String, Object>, Map<String, Object>>() {
                            @Override
                            @SuppressWarnings("unchecked")
                            public Map<String, Object> serialize(Object dataFetcherResult) throws CoercingSerializeException {
                                if (dataFetcherResult instanceof Map) {
                                    return (Map<String, Object>) dataFetcherResult;
                                }
                                throw new CoercingSerializeException("Expected a Map but got: " + dataFetcherResult.getClass());
                            }

                            @Override
                            @SuppressWarnings("unchecked")
                            public Map<String, Object> parseValue(Object input) throws CoercingParseValueException {
                                if (input instanceof Map) {
                                    return (Map<String, Object>) input;
                                }
                                throw new CoercingParseValueException("Expected a Map but got: " + input.getClass());
                            }

                            @Override
                            @SuppressWarnings("unchecked")
                            public Map<String, Object> parseLiteral(Object input) throws CoercingParseLiteralException {
                                if (input instanceof Map) {
                                    return (Map<String, Object>) input;
                                }
                                throw new CoercingParseLiteralException("Expected a Map but got: " + input.getClass());
                            }
                        })
                        .build());
    }
}