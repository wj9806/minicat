package io.github.wj9806.minicat.server.config;

public class HttpConfig {

    private int keepAliveTime = 30;

    // Getters and Setters
    public int getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(int keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

}
