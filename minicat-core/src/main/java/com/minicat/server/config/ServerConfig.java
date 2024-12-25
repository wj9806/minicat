package com.minicat.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import static com.minicat.server.config.ServerConfigKeys.*;

public class ServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(ServerConfig.class);
    private static final Properties properties = new Properties();
    private static ServerConfig instance;
    
    private int port = 8080;
    private String contextPath;
    private String staticPath;
    private boolean showBanner;
    private boolean workerEnabled;
    private int workerCoreSize;
    private int workerMaxSize;
    private int workerQueueSize;
    private long workerKeepAliveTime;
    private int httpKeepAliveTime;
    private String mode = "nio";
    
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
        try (InputStream input = getClass().getResourceAsStream("/minicat.properties")) {
            if (input == null) {
                logger.error("Unable to find minicat.properties");
                return;
            }
            properties.load(input);
            
            // 读取配置，如果没有配置则使用默认值
            port = Integer.parseInt(properties.getProperty(PORT, DEFAULT_PORT));
            contextPath = properties.getProperty(CONTEXT_PATH, DEFAULT_CONTEXT_PATH);
            staticPath = properties.getProperty(STATIC_PATH, DEFAULT_STATIC_PATH);
            mode = properties.getProperty(SERVER_MODE, DEFAULT_SERVER_MODE);
            showBanner = Boolean.parseBoolean(properties.getProperty(SHOW_BANNER, DEFAULT_SHOW_BANNER));
            httpKeepAliveTime = Integer.parseInt(properties.getProperty(HTTP_KEEP_ALIVE_TIME, DEFAULT_HTTP_KEEP_ALIVE_TIME));

            // 线程池配置
            workerEnabled = Boolean.parseBoolean(properties.getProperty(WORKER_ENABLED, DEFAULT_WORKER_ENABLED));
            workerCoreSize = Integer.parseInt(properties.getProperty(WORKER_CORE_SIZE, DEFAULT_WORKER_CORE_SIZE));
            workerMaxSize = Integer.parseInt(properties.getProperty(WORKER_MAX_SIZE, DEFAULT_WORKER_MAX_SIZE));
            workerQueueSize = Integer.parseInt(properties.getProperty(WORKER_QUEUE_SIZE, DEFAULT_WORKER_QUEUE_SIZE));
            workerKeepAliveTime = Long.parseLong(properties.getProperty(WORKER_KEEP_ALIVE_TIME, DEFAULT_WORKER_KEEP_ALIVE_TIME));

            // 确保context-path以/开头，不以/结尾
            if (!contextPath.startsWith("/")) {
                contextPath = "/" + contextPath;
            }
            if (contextPath.endsWith("/")) {
                contextPath = contextPath.substring(0, contextPath.length() - 1);
            }
        } catch (IOException e) {
            logger.error("Error loading minicat.properties", e);
        }
    }

    public void setShowBanner(boolean showBanner) {
        this.showBanner = showBanner;
    }

    public void setProperties(String key, String value) {
        properties.setProperty(key, value);
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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

    public boolean isWorkerEnabled() {
        return workerEnabled;
    }

    public int getWorkerCoreSize() {
        return workerCoreSize;
    }

    public int getWorkerMaxSize() {
        return workerMaxSize;
    }

    public int getWorkerQueueSize() {
        return workerQueueSize;
    }

    public long getWorkerKeepAliveTime() {
        return workerKeepAliveTime;
    }

    public int getHttpKeepAliveTime() {
        return httpKeepAliveTime;
    }

    public String getMode() {
        return mode;
    }

    public boolean nioEnabled() {
        return "nio".equalsIgnoreCase(mode);
    }
}
