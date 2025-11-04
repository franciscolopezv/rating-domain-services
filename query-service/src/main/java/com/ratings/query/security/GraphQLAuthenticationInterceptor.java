package com.ratings.query.security;

import com.ratings.shared.security.JwtUtil;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

/**
 * GraphQL authentication interceptor for JWT validation
 */
@Component
public class GraphQLAuthenticationInterceptor extends DataFetcherExceptionResolverAdapter {
    
    private static final Logger logger = LoggerFactory.getLogger(GraphQLAuthenticationInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtUtil jwtUtil;
    
    public String extractAndValidateToken() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return null;
            }
            
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader(AUTHORIZATION_HEADER);
            
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                return null;
            }
            
            String token = authHeader.substring(BEARER_PREFIX.length());
            
            if (jwtUtil.validateToken(token)) {
                return jwtUtil.extractUsername(token);
            }
            
            return null;
            
        } catch (Exception e) {
            logger.warn("Error extracting JWT token: {}", e.getMessage());
            return null;
        }
    }
    
    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        if (ex instanceof SecurityException) {
            return GraphqlErrorBuilder.newError()
                    .errorType(ErrorType.UNAUTHORIZED)
                    .message("Authentication required")
                    .path(env.getExecutionStepInfo().getPath())
                    .build();
        }
        return null;
    }
    
    /**
     * Create a secured data fetcher that requires authentication
     */
    public <T> DataFetcher<T> requireAuthentication(DataFetcher<T> originalFetcher) {
        return environment -> {
            String username = extractAndValidateToken();
            if (username == null) {
                throw new SecurityException("Authentication required");
            }
            
            // Add username to the environment context
            environment.getGraphQlContext().put("username", username);
            
            return originalFetcher.get(environment);
        };
    }
    
    /**
     * Create an optional authentication data fetcher
     */
    public <T> DataFetcher<T> optionalAuthentication(DataFetcher<T> originalFetcher) {
        return environment -> {
            String username = extractAndValidateToken();
            if (username != null) {
                environment.getGraphQlContext().put("username", username);
            }
            
            return originalFetcher.get(environment);
        };
    }
}