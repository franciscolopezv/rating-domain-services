package com.ratings.shared.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for service-to-service authentication
 */
@Configuration
public class ServiceAuthenticationConfig {
    
    @Value("${ratings.security.service.secret:defaultServiceSecret}")
    private String serviceSecret;
    
    @Value("${ratings.security.service.token.expiration:7200}")
    private Long serviceTokenExpiration;
    
    public String getServiceSecret() {
        return serviceSecret;
    }
    
    public Long getServiceTokenExpiration() {
        return serviceTokenExpiration;
    }
    
    /**
     * Generate a service token for inter-service communication
     */
    public String generateServiceToken(String serviceName) {
        JwtUtil jwtUtil = new JwtUtil();
        // Use reflection to set the secret and expiration
        try {
            java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, serviceSecret);
            
            java.lang.reflect.Field expirationField = JwtUtil.class.getDeclaredField("expiration");
            expirationField.setAccessible(true);
            expirationField.set(jwtUtil, serviceTokenExpiration);
            
            return jwtUtil.generateToken("service:" + serviceName);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate service token", e);
        }
    }
    
    /**
     * Validate a service token
     */
    public boolean validateServiceToken(String token) {
        try {
            JwtUtil jwtUtil = new JwtUtil();
            // Use reflection to set the secret
            java.lang.reflect.Field secretField = JwtUtil.class.getDeclaredField("secret");
            secretField.setAccessible(true);
            secretField.set(jwtUtil, serviceSecret);
            
            String username = jwtUtil.extractUsername(token);
            return username != null && username.startsWith("service:") && jwtUtil.validateToken(token);
        } catch (Exception e) {
            return false;
        }
    }
}