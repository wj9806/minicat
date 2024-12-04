package com.minicat.server;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

public class HttpServletRequest implements javax.servlet.http.HttpServletRequest {
    private String method;
    private String uri;
    private String protocol;
    private String requestURI;
    private String queryString;
    private Map<String, String> headers = new HashMap<>();
    private Map<String, String[]> parameters = new HashMap<>();
    private String characterEncoding = "UTF-8";
    private ServletContext servletContext;

    public HttpServletRequest(String method, String requestURI, String protocol) {
        this.method = method;
        this.protocol = protocol;
        this.uri = requestURI;
        
        // Parse URI and query string
        if (requestURI.contains("?")) {
            String[] parts = requestURI.split("\\?", 2);
            this.requestURI = parts[0];
            this.queryString = parts[1];
            parseParameters(this.queryString);
        } else {
            this.requestURI = requestURI;
        }
    }

    private void parseParameters(String queryString) {
        if (queryString == null || queryString.trim().isEmpty()) {
            return;
        }
        
        String[] pairs = queryString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            String key = keyValue[0];
            String value = keyValue.length > 1 ? keyValue[1] : "";
            
            String[] values = parameters.get(key);
            if (values == null) {
                parameters.put(key, new String[]{value});
            } else {
                String[] newValues = Arrays.copyOf(values, values.length + 1);
                newValues[values.length] = value;
                parameters.put(key, newValues);
            }
        }
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    @Override
    public String getMethod() {
        return method;
    }

    @Override
    public String getProtocol() {
        return protocol;
    }

    @Override
    public String getRequestURI() {
        return requestURI;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    // 添加请求头
    public void addHeader(String name, String value) {
        headers.put(name.toLowerCase(), value);
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.keySet());
    }

    // 以下是必须实现但暂时不需要的方法，返回默认值或抛出异常
    @Override
    public String getAuthType() { return null; }

    @Override
    public Cookie[] getCookies() { return new Cookie[0]; }

    @Override
    public long getDateHeader(String name) { return -1; }

    @Override
    public Enumeration<String> getHeaders(String name) { 
        return Collections.enumeration(Collections.singletonList(getHeader(name))); 
    }

    @Override
    public int getIntHeader(String name) { return -1; }

    @Override
    public String getPathInfo() { return null; }

    @Override
    public String getPathTranslated() { return null; }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public String getRemoteUser() { return null; }

    @Override
    public boolean isUserInRole(String role) { return false; }

    @Override
    public Principal getUserPrincipal() { return null; }

    @Override
    public String getRequestedSessionId() { return null; }

    @Override
    public StringBuffer getRequestURL() { return new StringBuffer(); }

    @Override
    public String getServletPath() { return ""; }

    @Override
    public HttpSession getSession(boolean create) { return null; }

    @Override
    public HttpSession getSession() { return null; }

    @Override
    public String changeSessionId() { return null; }

    @Override
    public boolean isRequestedSessionIdValid() { return false; }

    @Override
    public boolean isRequestedSessionIdFromCookie() { return false; }

    @Override
    public boolean isRequestedSessionIdFromURL() { return false; }

    @Override
    public boolean isRequestedSessionIdFromUrl() { return false; }

    @Override
    public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

    @Override
    public void login(String username, String password) { }

    @Override
    public void logout() { }

    @Override
    public Collection<Part> getParts() { return null; }

    @Override
    public Part getPart(String name) { return null; }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }

    @Override
    public Object getAttribute(String name) { return null; }

    @Override
    public Enumeration<String> getAttributeNames() { return null; }

    @Override
    public String getCharacterEncoding() { return characterEncoding; }

    @Override
    public void setCharacterEncoding(String env) { this.characterEncoding = env; }

    @Override
    public int getContentLength() { return 0; }

    @Override
    public long getContentLengthLong() { return 0; }

    @Override
    public String getContentType() { return null; }

    @Override
    public ServletInputStream getInputStream() { return null; }

    @Override
    public BufferedReader getReader() { return null; }

    @Override
    public String getLocalAddr() { return null; }

    @Override
    public String getLocalName() { return null; }

    @Override
    public int getLocalPort() { return 0; }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }

    @Override
    public Locale getLocale() { return Locale.getDefault(); }

    @Override
    public Enumeration<Locale> getLocales() { return null; }

    @Override
    public String getRemoteAddr() { return null; }

    @Override
    public String getRemoteHost() { return null; }

    @Override
    public int getRemotePort() { return 0; }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) { return null; }

    @Override
    public String getRealPath(String path) { return null; }

    @Override
    public String getScheme() { return "http"; }

    @Override
    public String getServerName() { return "localhost"; }

    @Override
    public int getServerPort() { return 8080; }

    @Override
    public boolean isSecure() { return false; }

    @Override
    public void removeAttribute(String name) { }

    @Override
    public void setAttribute(String name, Object o) { }

    @Override
    public boolean isAsyncStarted() { return false; }

    @Override
    public boolean isAsyncSupported() { return false; }

    @Override
    public AsyncContext getAsyncContext() { return null; }

    @Override
    public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }
}
