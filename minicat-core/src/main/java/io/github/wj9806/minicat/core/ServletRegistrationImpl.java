package io.github.wj9806.minicat.core;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import java.util.*;

public class ServletRegistrationImpl extends RegistrationBase implements ServletRegistration.Dynamic {
    private final Servlet servlet;
    private final ApplicationContext applicationContext;
    private final Set<String> mappings = new HashSet<>();
    private boolean loaded;
    private int loadOnStartup = -1;
    private String runAsRole;
    private MultipartConfigElement multipartConfig;
    private final ServletConfigImpl servletConfig;

    public ServletRegistrationImpl(String servletName, Servlet servlet,
                                   ApplicationContext applicationContext, ServletConfigImpl servletConfig) {
        super(servletName, servlet.getClass().getName());
        this.applicationContext = applicationContext;
        this.servlet = servlet;
        this.servletConfig = servletConfig;

        WebServlet webServlet = servlet.getClass().getAnnotation(WebServlet.class);
        if (webServlet != null) {
            handleWebInitParams(webServlet.initParams());
            handleMapping(webServlet.urlPatterns(), webServlet.value());
        }
        servletConfig.setInitParameters(getInitParameters());

        MultipartConfig multipartConfig = servlet.getClass().getAnnotation(MultipartConfig.class);
        if (multipartConfig != null) {
            MultipartConfigElement element = new MultipartConfigElement(multipartConfig);
            this.setMultipartConfig(element);
        }
    }

    private void handleMapping(String[] urlPatterns, String[] value) {
        if (value != null && value.length > 0) {
            addMapping(value);
        } else if (urlPatterns != null && urlPatterns.length > 0) {
            addMapping(urlPatterns);
        }
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public Servlet getServlet() {
        return servlet;
    }

    public ServletConfigImpl getServletConfig() {
        return servletConfig;
    }

    @Override
    public Set<String> addMapping(String... urlPatterns) {
        if (urlPatterns == null || urlPatterns.length == 0) {
            throw new IllegalArgumentException("URL patterns cannot be null or empty");
        }

        Set<String> conflicts = new HashSet<>();
        for (String pattern : urlPatterns) {
            if (pattern == null || pattern.isEmpty()) {
                throw new IllegalArgumentException("URL pattern cannot be null or empty");
            }
            
            // 验证URL模式格式
            if (!isValidUrlPattern(pattern)) {
                throw new IllegalArgumentException("Invalid URL pattern: " + pattern);
            }

            if (!mappings.add(pattern)) {
                conflicts.add(pattern);
            }
        }

        // 如果没有冲突，添加映射
        if (conflicts.isEmpty()) {
            for (String pattern : urlPatterns) {
                ((ApplicationServletContext) applicationContext).addServletMapping(name, pattern);
            }
        }

        return conflicts;
    }

    private boolean isValidUrlPattern(String pattern) {
        // 1. 验证空或 null
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        // 2. 验证根路径
        if (pattern.equals("/")) {
            return true;
        }

        // 3. 验证扩展名匹配 (*.xxx)
        if (pattern.startsWith("*.")) {
            // 不允许路径分隔符
            return !pattern.substring(2).contains("/") && !pattern.substring(2).contains("*");
        }

        // 4. 必须以 / 开头
        if (!pattern.startsWith("/")) {
            return false;
        }

        // 5. 验证路径通配符匹配 (/xxx/*)
        if (pattern.contains("*")) {
            // 通配符必须在末尾，并且格式为 /*
            return pattern.endsWith("/*") && pattern.indexOf("*") == pattern.length() - 1;
        }

        // 6. 精确路径匹配
        return true;
    }

    @Override
    public Collection<String> getMappings() {
        return Collections.unmodifiableSet(mappings);
    }

    @Override
    public String getRunAsRole() {
        return runAsRole;
    }

    @Override
    public void setLoadOnStartup(int loadOnStartup) {
        this.loadOnStartup = loadOnStartup;
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement constraint) {
        return Collections.emptySet();
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfig) {
        this.multipartConfig = multipartConfig;
    }

    @Override
    public void setRunAsRole(String roleName) {
        this.runAsRole = roleName;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public MultipartConfigElement getMultipartConfig() {
        return multipartConfig;
    }
}
