package com.ratings.command.security;

import io.grpc.Context;

/**
 * Security context for gRPC calls
 */
public class SecurityContext {
    
    public static final Context.Key<String> USERNAME_KEY = Context.key("username");
    
    public static String getCurrentUsername() {
        return USERNAME_KEY.get();
    }
    
    public static boolean isAuthenticated() {
        return getCurrentUsername() != null;
    }
}