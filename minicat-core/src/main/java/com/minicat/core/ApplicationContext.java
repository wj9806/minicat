package com.minicat.core;

import com.minicat.asm.ClassFileInfo;
import com.minicat.asm.ClassParser;
import com.minicat.core.event.ServletContextEventObject;
import com.minicat.http.ApplicationRequest;
import com.minicat.http.DefaultSessionManager;
import com.minicat.http.SessionManager;
import com.minicat.loader.WebappClassLoader;
import com.minicat.server.*;
import com.minicat.server.config.ServerConfig;
import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletContextAttributeEventObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.http.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ApplicationContext implements javax.servlet.ServletContext, ApplicationServletContext, Lifecycle {
    private final String contextPath;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();
    private final Map<String, String> initParameters = new HashMap<>();

    private final Map<String, String> servletUrlPatterns = new HashMap<>();
    private final Map<String, ServletRegistrationImpl> servletRegistrations = new HashMap<>();

    private final Map<String, FilterRegistrationImpl> filterRegistrations = new HashMap<>();
    private final List<FilterRegistrationImpl> filterChain = new ArrayList<>();

    private final SessionManager sessionManager;
    private final ServerConfig config;
    private final String staticPath;
    private final Map<ServletContainerInitializer,Set<Class<?>>> initializers = new LinkedHashMap<>();
    private final InternalEventMulticast internalEventMulticast;
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class.getName());

    private WebappClassLoader classLoader;

    public ApplicationContext(ServerConfig config) {
        this.classLoader = new WebappClassLoader(ClassLoader.getSystemClassLoader());
        Thread.currentThread().setContextClassLoader(classLoader);
        this.config = config;
        this.contextPath = config.getContextPath();
        this.staticPath = config.getStaticPath();
        this.internalEventMulticast = new InternalEventMulticast();
        this.sessionManager = new DefaultSessionManager();
        initContainerInitializers();
    }

    private void initContainerInitializers() {
        ServiceLoader<ServletContainerInitializer> load = ServiceLoader.load(ServletContainerInitializer.class);
        for (ServletContainerInitializer initializer : load) {
            HandlesTypes handlesTypes = initializer.getClass().getAnnotation(HandlesTypes.class);
            if (handlesTypes == null) {
                initializers.put(initializer, new HashSet<>());
                continue;
            }
            Class<?>[] value = handlesTypes.value();

            initializers.put(initializer, Arrays.stream(value).collect(Collectors.toSet()));
        }
    }

    public void addServletContainerInitializer(
            ServletContainerInitializer sci, Set<Class<?>> classes) {
        initializers.put(sci, classes);
    }

    public Servlet findMatchingServlet(javax.servlet.http.HttpServletRequest request) throws ServletException {
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

            return prepareServletForReq(servletName, servletRequest);
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
            return prepareServletForReq(matchedEntry.getValue(), servletRequest);
        }

        // 3. 扩展名匹配 (*.xxx)
        int lastDot = path.lastIndexOf('.');
        if (lastDot >= 0) {
            String extension = "*" + path.substring(lastDot);
            servletName = servletUrlPatterns.get(extension);
            if (servletName != null) {
                servletRequest.setServletPath(path);
                servletRequest.setPathInfo(null);
                return prepareServletForReq(servletName, servletRequest);
            }
        }

        // 4. 默认 servlet (/)
        servletName = servletUrlPatterns.get("/");
        if (servletName != null) {
            servletRequest.setServletPath("");
            servletRequest.setPathInfo(path);
            return prepareServletForReq(servletName, servletRequest);
        }

        // 5. 未匹配
        servletRequest.setServletPath("");
        servletRequest.setPathInfo(null);
        return prepareServletForReq("default", servletRequest);
    }

    private Servlet prepareServletForReq(String servletName, ApplicationRequest servletRequest) throws ServletException {
        ServletRegistrationImpl registration = servletRegistrations.get(servletName);
        Servlet servlet = registration.getServlet();
        servletRequest.setServletRegistration(registration);
        initServlet(registration);
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
        ServletRegistrationImpl servletRegistration = servletRegistrations.get(name);
        return servletRegistration == null ? null : servletRegistration.getServlet();
    }

    @Override
    public Enumeration<Servlet> getServlets() {
        List<Servlet> servlets = servletRegistrations.values().stream()
                .map(ServletRegistrationImpl::getServlet).collect(Collectors.toList());
        return Collections.enumeration(servlets);
    }

    @Override
    public Enumeration<String> getServletNames() {
        return Collections.enumeration(servletRegistrations.keySet());
    }

    @Override
    public void log(String msg) {
        logger.info("{}", msg);
    }

    @Override
    public void log(Exception exception, String msg) {
        logger.error(msg, exception);
    }

    @Override
    public void log(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    @Override
    public String getRealPath(String path) {
        if (path == null) return null;
        return staticPath + (path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public String getServerInfo() {
        return "MiniCat" + Version.VERSION;
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

    public SessionManager getSessionManager() {
        return sessionManager;
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
            publishEvent(new ServletContextAttributeEventObject(
                this, name, object, EventType.SERVLET_CONTEXT_ATTRIBUTE_ADDED));
        } else if (!object.equals(oldValue)) {
            // 修改属性
            publishEvent(new ServletContextAttributeEventObject(
                this, name, oldValue, EventType.SERVLET_CONTEXT_ATTRIBUTE_REPLACED));
        }
    }

    @Override
    public void removeAttribute(String name) {
        Object oldValue = attributes.remove(name);
        if (oldValue != null) {
            // 删除属性
            publishEvent(new ServletContextAttributeEventObject(
                this, name, oldValue, EventType.SERVLET_CONTEXT_ATTRIBUTE_REMOVED));
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
            ServletConfigImpl servletConfig = new ServletConfigImpl(servletName, this);
            ServletRegistrationImpl registration = new ServletRegistrationImpl(servletName, servlet,
                    this, servletConfig);
            servletRegistrations.put(servletName, registration);
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
        if (filterName == null || className == null) {
            throw new IllegalArgumentException("Filter name or class name cannot be null");
        }

        if (filterRegistrations.containsKey(filterName)) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (!Filter.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + className + " is not a Filter");
            }

            Filter filter = (Filter) clazz.getConstructor().newInstance();
            return addFilter(filterName, filter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate filter: " + className, e);
        }
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Filter filter) {
        if (filterName == null || filter == null) {
            throw new IllegalArgumentException("Filter name or filter instance cannot be null");
        }

        if (filterRegistrations.containsKey(filterName)) {
            return null;
        }

        FilterConfigImpl filterConfig = new FilterConfigImpl(filterName, this);
        FilterRegistrationImpl registration = new FilterRegistrationImpl(filterName, filter, filterConfig);
        filterRegistrations.put(filterName, registration);
        filterChain.add(registration);
        return registration;
    }

    @Override
    public FilterRegistration.Dynamic addFilter(String filterName, Class<? extends Filter> filterClass) {
        if (filterName == null || filterClass == null) {
            throw new IllegalArgumentException("Filter name or filter class cannot be null");
        }

        if (filterRegistrations.containsKey(filterName)) {
            return null;
        }

        try {
            Filter filter = filterClass.getConstructor().newInstance();
            return addFilter(filterName, filter);
        } catch (Exception e) {
            throw new RuntimeException("Failed to instantiate filter: " + filterClass.getName(), e);
        }
    }

    @Override
    public <T extends Filter> T createFilter(Class<T> clazz) throws ServletException {
        if (clazz == null) {
            throw new IllegalArgumentException("Filter class cannot be null");
        }

        try {
            return clazz.getConstructor().newInstance();
        } catch (Exception e) {
            throw new ServletException("Failed to instantiate filter", e);
        }
    }

    @Override
    public FilterRegistrationImpl getFilterRegistration(String filterName) {
        return filterRegistrations.get(filterName);
    }

    @Override
    public Map<String, ? extends FilterRegistrationImpl> getFilterRegistrations() {
        return Collections.unmodifiableMap(filterRegistrations);
    }

    public FilterChain buildFilterChain(HttpServletRequest request, Servlet servlet) {
        String requestURI = request.getRequestURI();
        List<Filter> matchedFilters = new ArrayList<>();
        
        for (FilterRegistrationImpl registration : filterChain) {
            if (registration.matches(requestURI)) {
                matchedFilters.add(registration.getFilter());
            }
        }
        
        return new FilterChainImpl(matchedFilters, servlet);
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

    public void publishEvent(EventObject event) {
        internalEventMulticast.publishEvent(event);
    }

    @Override
    public <T extends EventListener> void addListener(T t) {
        internalEventMulticast.addListener(t);
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
        servletUrlPatterns.put(urlPattern, servletName);
    }

    @Override
    public void init() throws Exception {
        onStartup();
        publishEvent(new ServletContextEventObject(this, EventType.SERVLET_CONTEXT_INITIALIZED));
        initServlet();
        initFilter();
    }

    private void onStartup() throws ServletException {
        for (Map.Entry<ServletContainerInitializer, Set<Class<?>>> entry : initializers.entrySet()) {
            Set<Class<?>> foundClasses = new HashSet<>();
            for (Class<?> clazz : entry.getValue()) {
                foundClasses.addAll(loadClass(clazz));
            }
            entry.getKey().onStartup(foundClasses, this);
        }
    }

    private Set<Class<?>> loadClass(Class<?> clazz) {
        Set<Class<?>> result = new HashSet<>();
        result.add(clazz); // 添加当前类本身

        try {
            // 获取系统类加载器（或者应用程序类加载器）
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

            // 获取类路径（这通常不包括 JAR 文件内的类，需要额外处理）

            URL[] urLs = ((URLClassLoader) classLoader).getURLs();
            List<File> dirs = new ArrayList<>();
            for (URL url : urLs) {
                File dir = new File(url.getFile());
                if (dir.exists() && dir.isDirectory()) {
                    dirs.add(dir);
                }
            }
            // 遍历类路径下的所有目录，查找类文件
            for (File dir : dirs) {
                findAndAddClasses(dir, clazz.getPackage().getName(), clazz, result);
            }

            // 注意：这里没有处理 JAR 文件内的类，需要额外的逻辑来解压缩 JAR 并扫描其中的类文件

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private void findAndAddClasses(File directory, String packageName, Class<?> clazz, Set<Class<?>> result) throws IOException, ClassNotFoundException, NoSuchMethodException {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                findAndAddClasses(file, packageName + "." + file.getName(), clazz, result);
            } else if (file.getName().endsWith(".class")) {

                ClassFileInfo classFileInfo = ClassParser.parseClass(Files.newInputStream(file.toPath()));
                String cn = classFileInfo.getClassName();
                WebappClassLoader classLoader = (WebappClassLoader)Thread.currentThread().getContextClassLoader();
                if (isCandidate(classFileInfo, clazz)) {
                    Class<?> loadedClass = classLoader.loadClass(cn, Files.newInputStream(file.toPath()));
                    if (clazz.isAssignableFrom(loadedClass) && loadedClass != clazz) {
                        result.add(loadedClass);
                    }
                }
            }
        }
    }

    private boolean isCandidate(ClassFileInfo classFileInfo, Class<?> clazz) {
        String className = classFileInfo.getClassName();
        String superClassName = classFileInfo.getSuperClassName();
        Set<String> interfaceNames = classFileInfo.getInterfaceNames();

        String curName = clazz.getName();

        if (curName.equals(className)) {
            return true;
        }

        if (curName.equals(superClassName)) {
            return true;
        }

        for (String interfaceName : interfaceNames) {
            if (curName.equals(interfaceName)) {
                return true;
            }
        }
        return false;
    }

    private void initFilter() throws ServletException {
        for (FilterRegistrationImpl registration : filterChain) {
            registration.getFilter().init(registration.getFilterConfig());
        }
    }

    private void initServlet() throws ServletException {
        StaticResourceServlet staticServlet = new StaticResourceServlet(this.config);
        ServletRegistration.Dynamic staticResourceRegistration = addServlet("default", staticServlet);
        staticResourceRegistration.addMapping("/*");
        staticResourceRegistration.setLoadOnStartup(0);

        List<ServletRegistrationImpl> registrations = servletRegistrations.values().stream()
                .filter(r -> r.getLoadOnStartup() >= 0)
                .sorted(Comparator.comparing(ServletRegistrationImpl::getLoadOnStartup))
                .collect(Collectors.toList());

        for (ServletRegistrationImpl registration : registrations) {
            initServlet(registration);
        }
    }

    private void initServlet(ServletRegistrationImpl registration) throws ServletException {
        if (!registration.isLoaded()) {
            Servlet servlet = registration.getServlet();
            servlet.init(registration.getServletConfig());
            registration.setLoaded(true);
        }
    }

    @Override
    public void start() throws Exception {
        this.sessionManager.start();
    }

    @Override
    public void stop() throws Exception {
        this.sessionManager.stop();
    }

    @Override
    public void destroy() throws Exception {
        destroyServlet();
        destroyFilter();
        sessionManager.destroy();
        publishEvent(new ServletContextEventObject(this, EventType.SERVLET_CONTEXT_DESTROYED));
    }

    private void destroyServlet() {
        for (Map.Entry<String, ServletRegistrationImpl> entry : servletRegistrations.entrySet()) {
            try {
                Servlet servlet = entry.getValue().getServlet();
                servlet.destroy();
            } catch (Exception e) {
                logger.error("Error destroying servlet: {}", e.getMessage());
            }
        }
        servletRegistrations.clear();
    }

    private void destroyFilter() {
        for (FilterRegistrationImpl registration : filterChain) {
            try {
                Filter filter = registration.getFilter();
                filter.destroy();
            } catch (Exception e) {
                logger.error("Error destroying filter: {}", e.getMessage());
            }
        }
        filterChain.clear();
    }
}
