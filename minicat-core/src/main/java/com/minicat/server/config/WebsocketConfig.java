package com.minicat.server.config;

public class WebsocketConfig {

    private long maxSessionIdleTimeout;

    public long getMaxSessionIdleTimeout() {
        return maxSessionIdleTimeout;
    }

    public void setMaxSessionIdleTimeout(long maxSessionIdleTimeout) {
        this.maxSessionIdleTimeout = maxSessionIdleTimeout;
    }
}
