package com.minicat.core;

import javax.servlet.ServletContainerInitializer;
import java.util.Set;

public interface ApplicationServletContext {
    /**
     * 添加 Servlet 映射
     * @param servletName Servlet 名称
     * @param urlPattern URL 模式
     */
    void addServletMapping(String servletName, String urlPattern);

    void addServletContainerInitializer(
            ServletContainerInitializer sci, Set<Class<?>> classes);
}
