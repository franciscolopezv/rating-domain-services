package com.ratings.command.config;

import io.grpc.ServerInterceptor;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for gRPC server settings and interceptors.
 */
@Configuration
public class GrpcConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(GrpcConfig.class);
    
    /**
     * Global interceptor for logging gRPC requests and responses.
     * Temporarily disabled to troubleshoot gRPC issues.
     */
    // @GrpcGlobalServerInterceptor
    public ServerInterceptor loggingInterceptor() {
        return new LoggingServerInterceptor();
    }
    
    /**
     * Custom server interceptor for request/response logging.
     */
    private static class LoggingServerInterceptor implements ServerInterceptor {
        
        @Override
        public <ReqT, RespT> io.grpc.ServerCall.Listener<ReqT> interceptCall(
                io.grpc.ServerCall<ReqT, RespT> call,
                io.grpc.Metadata headers,
                io.grpc.ServerCallHandler<ReqT, RespT> next) {
            
            String methodName = call.getMethodDescriptor().getFullMethodName();
            logger.info("gRPC call started: {}", methodName);
            
            return new ForwardingServerCallListener<ReqT>() {
                @Override
                protected io.grpc.ServerCall.Listener<ReqT> delegate() {
                    return next.startCall(new ForwardingServerCall<ReqT, RespT>() {
                        @Override
                        protected io.grpc.ServerCall<ReqT, RespT> delegate() {
                            return call;
                        }
                        
                        @Override
                        public void close(io.grpc.Status status, io.grpc.Metadata trailers) {
                            if (status.isOk()) {
                                logger.info("gRPC call completed successfully: {}", methodName);
                            } else {
                                logger.warn("gRPC call failed: {} - {}", methodName, status);
                            }
                            super.close(status, trailers);
                        }
                    }, headers);
                }
            };
        }
    }
    
    /**
     * Base class for forwarding server call listeners.
     */
    private abstract static class ForwardingServerCallListener<ReqT> extends io.grpc.ServerCall.Listener<ReqT> {
        protected abstract io.grpc.ServerCall.Listener<ReqT> delegate();
        
        @Override
        public void onMessage(ReqT message) {
            delegate().onMessage(message);
        }
        
        @Override
        public void onHalfClose() {
            delegate().onHalfClose();
        }
        
        @Override
        public void onCancel() {
            delegate().onCancel();
        }
        
        @Override
        public void onComplete() {
            delegate().onComplete();
        }
        
        @Override
        public void onReady() {
            delegate().onReady();
        }
    }
    
    /**
     * Base class for forwarding server calls.
     */
    private abstract static class ForwardingServerCall<ReqT, RespT> extends io.grpc.ServerCall<ReqT, RespT> {
        protected abstract io.grpc.ServerCall<ReqT, RespT> delegate();
        
        @Override
        public void request(int numMessages) {
            delegate().request(numMessages);
        }
        
        @Override
        public void sendHeaders(io.grpc.Metadata headers) {
            delegate().sendHeaders(headers);
        }
        
        @Override
        public void sendMessage(RespT message) {
            delegate().sendMessage(message);
        }
        
        @Override
        public void close(io.grpc.Status status, io.grpc.Metadata trailers) {
            delegate().close(status, trailers);
        }
        
        @Override
        public boolean isCancelled() {
            return delegate().isCancelled();
        }
        
        @Override
        public io.grpc.MethodDescriptor<ReqT, RespT> getMethodDescriptor() {
            return delegate().getMethodDescriptor();
        }
    }
}