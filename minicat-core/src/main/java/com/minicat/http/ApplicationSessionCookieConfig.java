package com.minicat.http;

import javax.servlet.SessionCookieConfig;

public class ApplicationSessionCookieConfig implements SessionCookieConfig {
    private String name = "JSESSIONID"; // 默认cookie名称
    private String domain;
    private String path = "/";          // 默认路径
    private String comment;
    private boolean httpOnly = false;
    private boolean secure = false;
    private int maxAge = -1;            // 默认为-1，表示cookie在会话结束时过期

    @Override
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Cookie name cannot be null or empty");
        }
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String getDomain() {
        return this.domain;
    }

    @Override
    public void setPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("Cookie path cannot be null");
        }
        this.path = path;
    }

    @Override
    public String getPath() {
        return this.path;
    }

    @Override
    public void setComment(String comment) {
        this.comment = comment;
    }

    @Override
    public String getComment() {
        return this.comment;
    }

    @Override
    public void setHttpOnly(boolean httpOnly) {
        this.httpOnly = httpOnly;
    }

    @Override
    public boolean isHttpOnly() {
        return this.httpOnly;
    }

    @Override
    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    @Override
    public boolean isSecure() {
        return this.secure;
    }

    @Override
    public void setMaxAge(int maxAge) {
        this.maxAge = maxAge;
    }

    @Override
    public int getMaxAge() {
        return this.maxAge;
    }
}
