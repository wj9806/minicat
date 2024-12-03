package com.minicat.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.minicat.config.ServerConfigKeys.*;

public class ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private static final Properties properties = new Properties();
    private static ServerConfig instance;
    
    private int port;
    private String contextPath;
    private String staticPath;
    private boolean showBanner;
    private boolean threadPoolEnabled;
    private int threadPoolCoreSize;
    private int threadPoolMaxSize;
    private int threadPoolQueueSize;
    private long threadPoolKeepAliveTime;
    
    private ServerConfig() {
        loadProperties();
    }
    
    public static ServerConfig getInstance() {
        if (instance == null) {
            instance = new ServerConfig();
        }
        return instance;
    }
    
    private void loadProperties() {
        try (InputStream input = getClass().getResourceAsStream("/server.properties")) {
            if (input == null) {
                logger.error("Unable to find server.properties");
                return;
            }
            properties.load(input);
            
            // 读取配置，如果没有配置则使用默认值
            port = Integer.parseInt(properties.getProperty(PORT, DEFAULT_PORT));
            contextPath = properties.getProperty(CONTEXT_PATH, DEFAULT_CONTEXT_PATH);
            staticPath = properties.getProperty(STATIC_PATH, DEFAULT_STATIC_PATH);
            showBanner = Boolean.parseBoolean(properties.getProperty(SHOW_BANNER, DEFAULT_SHOW_BANNER));

            // 线程池配置
            threadPoolEnabled = Boolean.parseBoolean(properties.getProperty(THREAD_POOL_ENABLED, DEFAULT_THREAD_POOL_ENABLED));
            threadPoolCoreSize = Integer.parseInt(properties.getProperty(THREAD_POOL_CORE_SIZE, DEFAULT_THREAD_POOL_CORE_SIZE));
            threadPoolMaxSize = Integer.parseInt(properties.getProperty(THREAD_POOL_MAX_SIZE, DEFAULT_THREAD_POOL_MAX_SIZE));
            threadPoolQueueSize = Integer.parseInt(properties.getProperty(THREAD_POOL_QUEUE_SIZE, DEFAULT_THREAD_POOL_QUEUE_SIZE));
            threadPoolKeepAliveTime = Long.parseLong(properties.getProperty(THREAD_POOL_KEEP_ALIVE_TIME, DEFAULT_THREAD_POOL_KEEP_ALIVE_TIME));

            // 确保context-path以/开头，不以/结尾
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        } catch (IOException e) {
            logger.error("Error loading server.properties", e);
        }
    }

    public int getPort() {
        return port;
    }

    public String getContextPath() {
        return contextPath;
    }

    public String getStaticPath() {
        return staticPath;
    }

    public boolean isShowBanner() {
        return showBanner;
    }

    public boolean isThreadPoolEnabled() {
        return threadPoolEnabled;
    }

    public int getThreadPoolCoreSize() {
        return threadPoolCoreSize;
    }

    public int getThreadPoolMaxSize() {
        return threadPoolMaxSize;
    }

    public int getThreadPoolQueueSize() {
        return threadPoolQueueSize;
    }

    public long getThreadPoolKeepAliveTime() {
        return threadPoolKeepAliveTime;
    }
}
