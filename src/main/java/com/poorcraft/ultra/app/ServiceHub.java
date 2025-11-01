package com.poorcraft.ultra.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Minimal dependency injection container / service registry.
 * Provides centralized access to application services and configuration.
 * Phase 0: Lightweight implementation with clear extension points for later phases.
 */
public class ServiceHub {
    private static final Logger logger = LoggerFactory.getLogger(ServiceHub.class);
    
    private final Map<Class<?>, Object> services = new HashMap<>();
    private final ClientConfig config;
    
    /**
     * Creates a new ServiceHub with the given configuration.
     * 
     * @param config Client configuration
     */
    public ServiceHub(ClientConfig config) {
        this.config = config;
        
        // Register core services
        register(ClientConfig.class, config);
        register(ServiceHub.class, this);
        
        logger.info("ServiceHub initialized with {} services", services.size());
    }
    
    /**
     * Registers a service instance for the given type.
     * 
     * @param type Service type/interface
     * @param instance Service instance
     * @param <T> Service type
     */
    public <T> void register(Class<T> type, T instance) {
        if (services.containsKey(type)) {
            logger.warn("Overwriting existing service: {}", type.getSimpleName());
        }
        services.put(type, instance);
        logger.debug("Registered service: {}", type.getSimpleName());
    }
    
    /**
     * Retrieves a service instance by type.
     * 
     * @param type Service type/interface
     * @param <T> Service type
     * @return Service instance
     * @throws IllegalArgumentException if service not found
     */
    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> type) {
        Object service = services.get(type);
        if (service == null) {
            throw new IllegalArgumentException("Service not found: " + type.getSimpleName());
        }
        return (T) service;
    }
    
    /**
     * Checks if a service is registered.
     * 
     * @param type Service type/interface
     * @return true if service is registered
     */
    public boolean has(Class<?> type) {
        return services.containsKey(type);
    }
    
    /**
     * Gets the client configuration.
     * Convenience method for get(ClientConfig.class).
     * 
     * @return Client configuration
     */
    public ClientConfig getConfig() {
        return config;
    }
    
    /**
     * Creates a logger factory method for consistent logging across services.
     * Future: Could be extended to provide custom logger implementations.
     * 
     * @param clazz Class to create logger for
     * @return Logger instance
     */
    public Logger createLogger(Class<?> clazz) {
        return LoggerFactory.getLogger(clazz);
    }
}
