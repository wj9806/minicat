package com.minicat.core;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletSecurityElement;
import java.util.*;

public class ServletRegistrationImpl implements ServletRegistration.Dynamic {
    private final String servletName;
    private final String className;
    private final ApplicationContext applicationContext;
    private final Map<String, String> initParameters = new HashMap<>();
    private final Set<String> mappings = new HashSet<>();
    private int loadOnStartup = -1;
    private String runAsRole;
    private boolean asyncSupported = false;
    private MultipartConfigElement multipartConfig;

    public ServletRegistrationImpl(String servletName, String className, ApplicationContext applicationContext) {
        this.servletName = servletName;
        this.className = className;
        this.applicationContext = applicationContext;
    }

    @Override
    public String getName() {
        return servletName;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (name == null || value == null) {
            throw new IllegalArgumentException("Init parameter name or value cannot be null");
        }
        if (initParameters.containsKey(name)) {
            return false;
        }
        initParameters.put(name, value);
        return true;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Set<String> setInitParameters(Map<String, String> initParameters) {
        Set<String> conflicts = new HashSet<>();
        for (Map.Entry<String, String> entry : initParameters.entrySet()) {
            if (!setInitParameter(entry.getKey(), entry.getValue())) {
                conflicts.add(entry.getKey());
            }
        }
        return conflicts;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return Collections.unmodifiableMap(initParameters);
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
                ((ApplicationServletContext) applicationContext).addServletMapping(servletName, pattern);
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

    @Override
    public void setAsyncSupported(boolean isAsyncSupported) {
        this.asyncSupported = isAsyncSupported;
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    public MultipartConfigElement getMultipartConfig() {
        return multipartConfig;
    }
}
