package com.minicat.core;

import com.minicat.http.ApplicationRequest;
import com.minicat.server.*;
import com.minicat.server.config.ServerConfig;
import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletContextAttributeEventObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationContext implements javax.servlet.ServletContext, ApplicationServletContext, Lifecycle {
    private final String contextPath;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParameters = new HashMap<>();
    private final Map<String, Servlet> servletMap = new HashMap<>();
    private final Map<String, String> servletUrlPatterns = new HashMap<>();
    private final Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();
    private final ServerConfig config;
    private final String serverInfo = "MiniCat/1.0";
    private final String staticPath;
    private final InternalContext internalContext;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class.getName());

    public ApplicationContext(ServerConfig config) {
        this.config = config;
        this.contextPath = config.getContextPath();
        this.staticPath = config.getStaticPath();
        this.internalContext = new InternalContext();
    }

    public Servlet findMatchingServlet(javax.servlet.http.HttpServletRequest request) {
        ApplicationRequest servletRequest;
        if (request instanceof HttpServletRequestWrapper) {
            servletRequest = (ApplicationRequest)((HttpServletRequestWrapper)request).getRequest();
        } else {
            servletRequest = (ApplicationRequest)request;
        }

        String path = servletRequest.getRequestURI();
        
        // 移除上下文路径
        if (path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }

        // 移除查询参数
        if (path.contains("?")) {
            path = path.substring(0, path.indexOf("?"));
        }

        // 1. 精确路径匹配
        String servletName = servletUrlPatterns.get(path);
        if (servletName != null) {
            servletRequest.setServletPath(path);
            servletRequest.setPathInfo(null);
            Servlet servlet = servletMap.get(servletName);
            servletRequest.setServletWrapper(servlet);
            return servlet;
        }

        // 2. 最长路径前缀匹配 (/xxx/*)
        Map.Entry<String, String> matchedEntry = getMatchedPatternAndServlet(path);
        if (matchedEntry != null) {
            String pattern = matchedEntry.getKey();
            String prefix = pattern.substring(0, pattern.length() - 2);
            servletRequest.setServletPath(prefix);
            if (path.length() > prefix.length()) {
                servletRequest.setPathInfo(path.substring(prefix.length()));
            } else {
                servletRequest.setPathInfo(null);
            }
            Servlet servlet = servletMap.get(matchedEntry.getValue());
            servletRequest.setServletWrapper(servlet);
            return servlet;
        }

        // 3. 扩展名匹配 (*.xxx)
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = "*" + path.substring(lastDot);
            servletName = servletUrlPatterns.get(extension);
            if (servletName != null) {
                servletRequest.setServletPath(path);
                servletRequest.setPathInfo(null);
                Servlet servlet = servletMap.get(servletName);
                servletRequest.setServletWrapper(servlet);
                return servlet;
            }
        }

        // 4. 默认 servlet (/)
        servletName = servletUrlPatterns.get("/");
        if (servletName != null) {
            servletRequest.setServletPath("");
            servletRequest.setPathInfo(path);
            Servlet servlet = servletMap.get(servletName);
            servletRequest.setServletWrapper(servlet);
            return servlet;
        }

        // 5. 未匹配
        servletRequest.setServletPath("");
        servletRequest.setPathInfo(null);
        Servlet servlet = servletMap.get("default");
        servletRequest.setServletWrapper(servlet);
        return servlet;
    }

    private Map.Entry<String, String> getMatchedPatternAndServlet(String path) {
        String longestMatch = "";
        Map.Entry<String, String> matchedEntry = null;
        for (Map.Entry<String, String> entry : servletUrlPatterns.entrySet()) {
            String pattern = entry.getKey();
            if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (path.startsWith(prefix) && prefix.length() > longestMatch.length()) {
                    longestMatch = prefix;
                    matchedEntry = entry;
                }
            }
        }
        return matchedEntry;
    }

    @Override
    public String getContextPath() {
        return contextPath;
    }

    @Override
    public ApplicationContext getContext(String uripath) {
        return this;
    }

    @Override
    public int getMajorVersion() {
        return 3;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public int getEffectiveMajorVersion() {
        return 0;
    }

    @Override
    public int getEffectiveMinorVersion() {
        return 0;
    }

    @Override
    public String getMimeType(String file) {
        if (file == null) return null;
        String ext = file.toLowerCase();
        if (ext.endsWith(".html") || ext.endsWith(".htm")) return "text/html";
        if (ext.endsWith(".css")) return "text/css";
        if (ext.endsWith(".js")) return "application/javascript";
        if (ext.endsWith(".json")) return "application/json";
        if (ext.endsWith(".png")) return "image/png";
        if (ext.endsWith(".jpg") || ext.endsWith(".jpeg")) return "image/jpeg";
        if (ext.endsWith(".gif")) return "image/gif";
        if (ext.endsWith(".xml")) return "application/xml";
        if (ext.endsWith(".pdf")) return "application/pdf";
        if (ext.endsWith(".mp4")) return "video/mp4";
        if (ext.endsWith(".webm")) return "video/webm";
        if (ext.endsWith(".ogg")) return "video/ogg";
        if (ext.endsWith(".mov")) return "video/quicktime";
        if (ext.endsWith(".avi")) return "video/x-msvideo";
        if (ext.endsWith(".wmv")) return "video/x-ms-wmv";
        if (ext.endsWith(".flv")) return "video/x-flv";
        if (ext.endsWith(".mkv")) return "video/x-matroska";
        return "application/octet-stream";
    }

    @Override
    public Set<String> getResourcePaths(String path) {
        // TODO: 实现资源路径获取
        return null;
    }

    @Override
    public URL getResource(String path) throws MalformedURLException {
        // TODO: 实现资源URL获取
        return null;
    }

    @Override
    public InputStream getResourceAsStream(String path) {
        // TODO: 实现资源流获取
        return null;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) {
        // TODO: 实现请求转发
        return null;
    }

    @Override
    public RequestDispatcher getNamedDispatcher(String name) {
        return null;
    }

    @Override
    public Servlet getServlet(String name) throws ServletException {
        return servletMap.get(name);
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        return Collections.enumeration(servletMap.values());
    }

    @Override
    public Enumeration<String> getServletNames() {
        return Collections.enumeration(servletMap.keySet());
    }

    @Override
    public void log(String msg) {
        System.out.println("[ServletContext] " + msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        System.out.println("[ServletContext] " + msg);
        exception.printStackTrace();
    }

    @Override
    public void log(String message, Throwable throwable) {
        System.out.println("[ServletContext] " + message);
        throwable.printStackTrace();
    }

    @Override
    public String getRealPath(String path) {
        if (path == null) return null;
        return staticPath + (path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public String getServerInfo() {
        return serverInfo;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    @Override
    public boolean setInitParameter(String name, String value) {
        if (name == null || value == null) return false;
        if (initParameters.containsKey(name)) return false;
        initParameters.put(name, value);
        return true;
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object object) {
        if (name == null) throw new IllegalArgumentException("Attribute name cannot be null");
        
        if (object == null) {
            removeAttribute(name);
            return;
        }
        
        Object oldValue = attributes.put(name, object);
        
        // 发布事件
        if (oldValue == null) {
            // 新增属性
            internalContext.publishEvent(new ServletContextAttributeEventObject(
                this, name, object, EventType.ATTRIBUTE_ADDED));
        } else if (!object.equals(oldValue)) {
            // 修改属性
            internalContext.publishEvent(new ServletContextAttributeEventObject(
                this, name, oldValue, EventType.ATTRIBUTE_REPLACED));
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object oldValue = attributes.remove(name);
        if (oldValue != null) {
            // 删除属性
            internalContext.publishEvent(new ServletContextAttributeEventObject(
                this, name, oldValue, EventType.ATTRIBUTE_REMOVED));
        }
    }

    @Override
    public String getServletContextName() {
        return "MiniCat";
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, String className) {
        if (servletName == null || className == null) {
            throw new IllegalArgumentException("Servlet name or class name cannot be null");
        }

        if (servletRegistrations.containsKey(servletName)) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!Servlet.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + className + " is not a Servlet");
            }

            Servlet servlet = (Servlet) clazz.getConstructor().newInstance();
            return addServlet(servletName, servlet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate servlet: " + className, e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Servlet servlet) {
        if (servletName == null || servlet == null) {
            throw new IllegalArgumentException("Servlet name or servlet instance cannot be null");
        }

        if (servletRegistrations.containsKey(servletName)) {
            return null;
        }

        try {
            ServletRegistrationImpl registration = new ServletRegistrationImpl(servletName, servlet.getClass().getName(), this);
            MultipartConfig annotation = servlet.getClass().getAnnotation(MultipartConfig.class);
            if (annotation != null) {
                MultipartConfigElement element = new MultipartConfigElement(annotation);
                registration.setMultipartConfig(element);
            }

            servletRegistrations.put(servletName, registration);

            servletMap.put(servletName, new ServletWrapper(servlet, registration));
            return registration;
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize servlet: " + servletName, e);
        }
    }

    @Override
    public ServletRegistration.Dynamic addServlet(String servletName, Class<? extends Servlet> servletClass) {
        if (servletName == null || servletClass == null) {
            throw new IllegalArgumentException("Servlet name or servlet class cannot be null");
        }

        if (servletRegistrations.containsKey(servletName)) {
            return null;
        }

        try {
            Servlet servlet = servletClass.getConstructor().newInstance();
            return addServlet(servletName, servlet);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate servlet: " + servletClass.getName(), e);
        }
    }

    @Override
    public <T extends Servlet> T createServlet(Class<T> clazz) throws ServletException {
        if (clazz == null) {
            throw new IllegalArgumentException("Servlet class cannot be null");
        }

        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ServletException("Failed to instantiate servlet", e);
        }
    }

    @Override
    public ServletRegistration getServletRegistration(String servletName) {
        return servletRegistrations.get(servletName);
    }

    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return Collections.unmodifiableMap(servletRegistrations);
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, String className) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        return null;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        return null;
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        return null;
    }

    @Override
    public FilterRegistration getFilterRegistration(String filterName) {
        return null;
    }

    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
    }

    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }

    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> sessionTrackingModes) {
    }

    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }

    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }

    @Override
    public void addListener(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Object obj = clazz.getConstructor().newInstance();

            if (!(obj instanceof EventListener)) {
                throw new IllegalArgumentException(className + " is not instance of the EventListener");
            }

            EventListener listener = (EventListener) obj;
            addListener(listener);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Listener class not found: " + className, e);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate listener", e);
        }
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        internalContext.addListener(t);
    }

    @Override
    public void addListener(Class<? extends EventListener> listenerClass) {
        try {
            EventListener listener = listenerClass.getConstructor().newInstance();
            addListener(listener);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate listener", e);
        }
    }

    @Override
    public <T extends EventListener> T createListener(Class<T> clazz) throws ServletException {
        try {
            T listener = clazz.getConstructor().newInstance();
            if (listener instanceof ServletContextListener ||
                    listener instanceof ServletContextAttributeListener ||
                    listener instanceof ServletRequestListener ||
                    listener instanceof ServletRequestAttributeListener ||
                    listener instanceof HttpSessionListener ||
                    listener instanceof HttpSessionIdListener ||
                    listener instanceof HttpSessionAttributeListener) {
                return listener;
            }
            throw new IllegalArgumentException("wrong Listener type: " + clazz.getName());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }

    @Override
    public ClassLoader getClassLoader() {
        return getClass().getClassLoader();
    }

    @Override
    public void declareRoles(String... roleNames) {
    }

    @Override
    public String getVirtualServerName() {
        return "minicat";
    }

    @Override
    public void addServletMapping(String servletName, String urlPattern) {
        if (servletMap.containsKey(servletName)) {
            servletUrlPatterns.put(urlPattern, servletName);
        }
    }

    private void destroyServlet() {
        for (Map.Entry<String, Servlet> entry : servletMap.entrySet()) {
            try {
                Servlet servlet = entry.getValue();
                servlet.destroy();
            } catch (Exception e) {
                logger.error("Error destroying servlet: " + e.getMessage());
            }
        }
        servletMap.clear();
    }

    @Override
    public void init() throws Exception {
        initServlet();
    }

    private void initServlet() throws ServletException {
        StaticResourceServlet staticServlet = new StaticResourceServlet(this.config);
        ServletRegistration.Dynamic registration = addServlet("default", staticServlet);
        registration.addMapping("/");

        for (Map.Entry<String, Servlet> entry : servletMap.entrySet()) {
            Servlet servlet = entry.getValue();
            servlet.init(new ServletConfig() {
                @Override
                public String getServletName() {
                    return entry.getKey();
                }

                @Override
                public ApplicationContext getServletContext() {
                    return ApplicationContext.this;
                }

                @Override
                public String getInitParameter(String name) {
                    return null;
                }

                @Override
                public Enumeration<String> getInitParameterNames() {
                    return Collections.emptyEnumeration();
                }
            });
        }
    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void destroy() throws Exception {
        destroyServlet();
    }
}
