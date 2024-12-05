package com.minicat.server.config;

/**
 * Server configuration keys and default values
 */
public class ServerConfigKeys {
    // Server basic settings
    public static final String PORT = "server.port";
    public static final String CONTEXT_PATH = "server.context-path";
    public static final String STATIC_PATH = "server.static-path";
    public static final String SHOW_BANNER = "server.show-banner";

    // Thread pool settings
    public static final String THREAD_POOL_ENABLED = "server.thread-pool.enabled";
    public static final String THREAD_POOL_CORE_SIZE = "server.thread-pool.core-size";
    public static final String THREAD_POOL_MAX_SIZE = "server.thread-pool.max-size";
    public static final String THREAD_POOL_QUEUE_SIZE = "server.thread-pool.queue-size";
    public static final String THREAD_POOL_KEEP_ALIVE_TIME = "server.thread-pool.keep-alive-time";
    
    // Default values
    public static final String DEFAULT_PORT = "8080";
    public static final String DEFAULT_CONTEXT_PATH = "/";
    public static final String DEFAULT_STATIC_PATH = "/static";
    public static final String DEFAULT_SHOW_BANNER = "true";
    
    // Default thread pool values
    public static final String DEFAULT_THREAD_POOL_ENABLED = "true";
    public static final String DEFAULT_THREAD_POOL_CORE_SIZE = "10";
    public static final String DEFAULT_THREAD_POOL_MAX_SIZE = "50";
    public static final String DEFAULT_THREAD_POOL_QUEUE_SIZE = "100";
    public static final String DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME = "60";
    
    private ServerConfigKeys() {
        // Prevent instantiation
    }
}
