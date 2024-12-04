package com.minicat.server;

public interface MiniCatServletContext {
    /**
     * 添加 Servlet 映射
     * @param servletName Servlet 名称
     * @param urlPattern URL 模式
     */
    void addServletMapping(String servletName, String urlPattern);
}
