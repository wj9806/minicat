package com.minicat.server.config;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ServerConfig {

    private int port = 8080;
    private String contextPath = "/";
    private List<String> staticPath = Collections.singletonList("/static");
    private boolean showBanner = true;
    private String mode = "nio";

    private WorkerConfig worker = new WorkerConfig();
    private NioConfig nio  = new NioConfig();

    public boolean nioEnabled() {
        return Objects.equals("nio", mode);
    }

    // 内部类用于配置worker
    public static class WorkerConfig {
        private boolean enabled = true;
        private int coreSize = 10;
        private int maxSize = 50;
        private int queueSize = 100;
        private int keepAliveTime = 60;

        // Getters and Setters
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getCoreSize() {
            return coreSize;
        }

        public void setCoreSize(int coreSize) {
            this.coreSize = coreSize;
        }

        public int getMaxSize() {
            return maxSize;
        }

        public void setMaxSize(int maxSize) {
            this.maxSize = maxSize;
        }

        public int getQueueSize() {
            return queueSize;
        }

        public void setQueueSize(int queueSize) {
            this.queueSize = queueSize;
        }

        public int getKeepAliveTime() {
            return keepAliveTime;
        }

        public void setKeepAliveTime(int keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
        }
    }

    // 内部类用于配置nio
    public static class NioConfig {
        private int backlog = 50;

        // Getters and Setters
        public int getBacklog() {
            return backlog;
        }

        public void setBacklog(int backlog) {
            this.backlog = backlog;
        }
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

    public void setContextPath(String contextPath) {
        // 确保context-path以/开头，不以/结尾
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        this.contextPath = contextPath;
    }

    public List<String> getStaticPath() {
        return staticPath;
    }

    public void setStaticPath(List<String> staticPath) {
        this.staticPath = staticPath;
    }

    public boolean isShowBanner() {
        return showBanner;
    }

    public void setShowBanner(boolean showBanner) {
        this.showBanner = showBanner;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public WorkerConfig getWorker() {
        return worker;
    }

    public void setWorker(WorkerConfig worker) {
        this.worker = worker;
    }

    public NioConfig getNio() {
        return nio;
    }

    public void setNio(NioConfig nio) {
        this.nio = nio;
    }
}
