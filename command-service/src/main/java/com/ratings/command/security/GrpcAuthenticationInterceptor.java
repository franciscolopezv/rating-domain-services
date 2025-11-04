package com.ratings.command.security;

import com.ratings.shared.security.JwtUtil;
import io.grpc.*;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * gRPC interceptor for JWT authentication
 */
@Component
@GrpcGlobalServerInterceptor
@ConditionalOnProperty(name = "ratings.security.grpc.enabled", havingValue = "true", matchIfMissing = true)
public class GrpcAuthenticationInterceptor implements ServerInterceptor {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcAuthenticationInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        
        String methodName = call.getMethodDescriptor().getFullMethodName();
        logger.debug("Intercepting gRPC call: {}", methodName);
        
        // Skip authentication for health checks and reflection
        if (isPublicMethod(methodName)) {
            return next.startCall(call, headers);
        }
        
        try {
            String token = extractToken(headers);
            if (token == null) {
                logger.warn("No authorization token found for method: {}", methodName);
                call.close(Status.UNAUTHENTICATED.withDescription("Missing authorization token"), headers);
                return new ServerCall.Listener<ReqT>() {};
            }
            
            if (!jwtUtil.validateToken(token)) {
                logger.warn("Invalid authorization token for method: {}", methodName);
                call.close(Status.UNAUTHENTICATED.withDescription("Invalid authorization token"), headers);
                return new ServerCall.Listener<ReqT>() {};
            }
            
            // Extract username and add to context
            String username = jwtUtil.extractUsername(token);
            Context context = Context.current().withValue(SecurityContext.USERNAME_KEY, username);
            
            logger.debug("Authenticated user: {} for method: {}", username, methodName);
            
            return Contexts.interceptCall(context, call, headers, next);
            
        } catch (Exception e) {
            logger.error("Authentication error for method: {}", methodName, e);
            call.close(Status.INTERNAL.withDescription("Authentication error"), headers);
            return new ServerCall.Listener<ReqT>() {};
        }
    }
    
    private String extractToken(Metadata headers) {
        String authHeader = headers.get(Metadata.Key.of(AUTHORIZATION_HEADER, Metadata.ASCII_STRING_MARSHALLER));
        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
    
    private boolean isPublicMethod(String methodName) {
        return methodName.contains("Health") || 
               methodName.contains("Reflection") ||
               methodName.equals("grpc.health.v1.Health/Check");
    }
}