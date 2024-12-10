package com.minicat.http;

import com.minicat.core.ApplicationContext;
import com.minicat.core.ServletRegistrationImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationRequest implements HttpServletRequest {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRequest.class);

    //basic info
    private final String method;
    private final String protocol;
    private final String requestURI;
    private String queryString;
    private final HttpHeaders headers = new HttpHeaders();
    final Map<String, String[]> parameters = new HashMap<>();
    private Charset charset;
    private boolean hasReadParameters;
    private String pathInfo;
    private String servletPath;

    //servlet info
    private ServletRegistrationImpl servletRegistration;
    private final ServletContext servletContext;
    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    //remote info
    private String remoteAddr;
    private String remoteHost;
    private int remotePort = -1;
    private String remoteUser;
    private String authType;

    //server info
    private String scheme;
    private String serverName;
    private int serverPort;
    private boolean secure;

    //local info
    private String localAddr;
    private String localName;
    private int localPort = -1;

    //input stream
    private RequestInputStream inputStream;
    private BufferedReader reader;
    byte[] body;

    public ApplicationRequest(ApplicationContext context, String[] lines) {
        String[] requestLine = lines[0].split(" ");

        if (requestLine.length < 3) throw new RequestParseException("requestLine != 3");

        String method = requestLine[0];
        String requestURI = requestLine[1];
        String protocol = requestLine[2];

        this.method = method;
        this.protocol = protocol;

        // Parse URI and query string
        logger.debug("Received request for URI: {}", requestURI);
        if (requestURI.contains("?")) {
            String[] parts = requestURI.split("\\?", 2);
            this.requestURI = parts[0];
            this.queryString = parts[1];
        } else {
            this.requestURI = requestURI;
        }

        this.servletContext = context;
    }

    /**
     * 设置请求头
     * @param headers 请求头对象
     */
    public void setHeaders(HttpHeaders headers) {
        for (Map.Entry<String, List<String>> entry : headers.getAll().entrySet()) {
            for (String value : entry.getValue()) {
                this.headers.add(entry.getKey(), value);
            }
        }
    }

    private void parseParameters() {
        if (hasReadParameters) return;

        doParseParameters(this.queryString);

        hasReadParameters = true;

        String contentType = getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("application/x-www-form-urlencoded")) {
            return;
        }

        if ("POST".equalsIgnoreCase(method)) {
            try {
                String encoding = getCharacterEncoding();
                if (encoding == null) {
                    encoding = "ISO-8859-1";
                }
                String bodyStr = new String(body, encoding);
                doParseParameters(bodyStr);
            } catch (Exception e) {
                logger.error("Failed to parse form parameters", e);
            }
        }
    }

    private void doParseParameters(String param) {
        if (param != null && !param.trim().isEmpty()) {
            String[] pairs = param.split("&");
            for (String pair : pairs) {
                if (pair.trim().isEmpty()) {
                    continue;
                }

                String[] keyValue = pair.split("=", 2);
                String key = decodeUrlParameter(keyValue[0]);
                String value = keyValue.length > 1 ? decodeUrlParameter(keyValue[1]) : "";

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
    }

    private String decodeUrlParameter(String value) {
        if (value == null) {
            return null;
        }
        try {
            String encoding = charset != null ? charset.name() : "ISO-8859-1";
            return URLDecoder.decode(value, encoding);
        } catch (Exception e) {
            return value;
        }
    }

    public ServletRegistrationImpl getServletRegistration() {
        return servletRegistration;
    }

    public void setServletRegistration(ServletRegistrationImpl servletRegistration) {
        this.servletRegistration = servletRegistration;
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
        parseParameters();
        String[] values = parameters.get(name);
        return values != null && values.length > 0 ? values[0] : null;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        parseParameters();
        return Collections.unmodifiableMap(parameters);
    }

    @Override
    public Enumeration<String> getParameterNames() {
        parseParameters();
        return Collections.enumeration(parameters.keySet());
    }

    @Override
    public String[] getParameterValues(String name) {
        parseParameters();
        return parameters.get(name);
    }

    // 添加请求头
    public void addHeader(String name, String value) {
        headers.add(name, value);
    }

    @Override
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return Collections.enumeration(headers.names());
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = headers.get(name);
        return values != null ? Collections.enumeration(values) : Collections.emptyEnumeration();
    }

    @Override
    public int getIntHeader(String name) {
        String value = getHeader(name);
        return value != null ? Integer.parseInt(value) : -1;
    }

    @Override
    public long getDateHeader(String name) {
        String value = getHeader(name);
        if (value == null) {
            return -1L;
        }
        try {
            return Date.parse(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Cannot parse date header '" + name + "': " + value);
        }
    }

    // 以下是必须实现但暂时不需要的方法，返回默认值或抛出异常
    @Override
    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public void setPathInfo(String pathInfo) {
        this.pathInfo = pathInfo;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    @Override
    public Cookie[] getCookies() { return new Cookie[0]; }

    @Override
    public String getPathInfo() { return pathInfo; }

    @Override
    public String getPathTranslated() { return null; }

    @Override
    public String getContextPath() {
        return servletContext.getContextPath();
    }

    @Override
    public String getRemoteUser() {
        return remoteUser;
    }

    @Override
    public boolean isUserInRole(String role) { return false; }

    @Override
    public Principal getUserPrincipal() { return null; }

    @Override
    public String getRequestedSessionId() { return null; }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer url = new StringBuffer();
        String scheme = getScheme();
        int port = getServerPort();
        String serverName = getServerName();

        url.append(scheme).append("://").append(serverName);

        // 只有在非默认端口时才添加端口号
        if (("http".equals(scheme) && port != 80) ||
            ("https".equals(scheme) && port != 443)) {
            url.append(':').append(port);
        }

        url.append(getRequestURI());

        return url;
    }

    @Override
    public String getServletPath() { return servletPath; }

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
    public Collection<Part> getParts() throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass) { return null; }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value == null) {
            removeAttribute(name);
            return;
        }
        
        attributes.put(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    @Override
    public String getCharacterEncoding() {
        return charset != null ? charset.name() : null;
    }

    @Override
    public void setCharacterEncoding(String env) throws UnsupportedEncodingException {
        if (hasReadParameters) {
            return; // 如果已经读取了参数，则不再允许更改编码
        }
        try {
            this.charset = Charset.forName(env);
        } catch (IllegalArgumentException e) {
            throw new UnsupportedEncodingException(env);
        }
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (reader != null) {
            throw new IllegalStateException("getReader() has already been called for this request");
        }
        if (inputStream == null) {
            inputStream = new RequestInputStream(body);
        }
        return inputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (inputStream != null && reader == null) {
            throw new IllegalStateException("getInputStream() has already been called for this request");
        }
        if (reader == null) {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = "ISO-8859-1";
            }
            reader = new BufferedReader(new InputStreamReader(getInputStream(), encoding));
        }
        return reader;
    }

    @Override
    public int getContentLength() {
        String contentLength = getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Integer.parseInt(contentLength);
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public long getContentLengthLong() {
        String contentLength = getHeader("Content-Length");
        if (contentLength != null) {
            try {
                return Long.parseLong(contentLength);
            } catch (NumberFormatException e) {
                return -1L;
            }
        }
        return -1L;
    }

    @Override
    public String getContentType() {
        return getHeader("Content-Type");
    }

    @Override
    public String getLocalAddr() {
        return localAddr;
    }

    @Override
    public String getLocalName() {
        return localName;
    }

    @Override
    public int getLocalPort() {
        return localPort;
    }

    public void setLocalInfo(String localAddr, String localName, int localPort) {
        this.localAddr = localAddr;
        this.localName = localName;
        this.localPort = localPort;
    }

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
    public String getRemoteAddr() {
        return remoteAddr != null ? remoteAddr : "";
    }

    @Override
    public String getRemoteHost() {
        return remoteHost != null ? remoteHost : getRemoteAddr();
    }

    @Override
    public int getRemotePort() {
        return remotePort;
    }

    public void setRemoteAddr(String remoteAddr) {
        this.remoteAddr = remoteAddr;
    }

    public void setRemoteHost(String remoteHost) {
        this.remoteHost = remoteHost;
    }

    public void setRemotePort(int remotePort) {
        this.remotePort = remotePort;
    }

    public void setRemoteUser(String remoteUser) {
        this.remoteUser = remoteUser;
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String path) { return null; }

    @Override
    public String getRealPath(String path) {
        return servletContext != null ? servletContext.getRealPath(path) : null;
    }

    @Override
    public String getScheme() {
        return scheme;
    }

    @Override
    public String getServerName() {
        return serverName;
    }

    @Override
    public int getServerPort() {
        return serverPort;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public boolean isAsyncStarted() { return false; }

    @Override
    public boolean isAsyncSupported() { return false; }

    @Override
    public AsyncContext getAsyncContext() { return null; }

    @Override
    public DispatcherType getDispatcherType() { return DispatcherType.REQUEST; }

    public void setServerInfo(String serverName, int serverPort, boolean secure) {
        this.serverName = serverName;
        this.serverPort = serverPort;
        this.secure = secure;
        this.scheme = secure ? "https" : "http";
    }

    public void destroy() {
        // 清理资源
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            logger.error("Error closing request resources", e);
        }

        // 清理属性
        attributes.clear();
        parameters.clear();
        
        // 清理引用
        servletRegistration = null;
        body = null;
        charset = null;
    }
}
