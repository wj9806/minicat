package com.minicat.server.processor;

import com.minicat.core.ApplicationContext;
import com.minicat.core.Lifecycle;
import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletRequestEventObject;
import com.minicat.http.*;
import com.minicat.net.Sock;
import com.minicat.server.IProcessor;
import org.slf4j.Logger;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 请求处理器接口
 */
public abstract class Processor<S> implements IProcessor<S>, Lifecycle {
    static final int BUFFER_SIZE = 1024;

    protected final ApplicationContext applicationContext;
    protected final Sock<S> sock;

    protected OutputStream hos;

    public Processor(ApplicationContext applicationContext, Sock<S> sock) {
        this.applicationContext = applicationContext;
        this.sock = sock;
    }

    protected abstract HttpServletResponse buildResponse();

    /**
     * 从socket中读取数据到buf中
     */
    protected abstract int read(Object buf) throws IOException;

    /**
     * 把buf的数据写入到ByteArrayOutputStream
     */
    protected abstract void write(OutputStream os, Object buf, int off, int len) throws IOException;

    protected abstract void sendNotFoundResponse() throws Exception;

    protected abstract void sendErrorResponse(String message) throws Exception;

    protected abstract Logger logger();

    protected abstract Object allocTmpBuf();

    public Sock<S> sock() {
        return sock;
    }

    public void send(ByteBuffer buf) throws IOException {
        hos.write(buf.array());
    }

    public void flush() throws IOException {
        hos.flush();
    }

    public int process() throws Exception {
        HttpServletRequest servletRequest = null;
        try {
            // 创建Request和Response对象
            HttpServletResponse servletResponse = buildResponse();
            try {
                servletRequest = buildRequest(sock, applicationContext, servletResponse);
            } catch (SocketCloseException | RequestParseException | SocketException e) {
                return -1;
            } catch (Exception e) {
                logger().error("Error processing request", e);
                sendErrorResponse(e.getMessage());
                return -1;
            }

            sock.freshLastProcess();

            // 处理context-path
            if (!applicationContext.getContextPath().isEmpty() &&
                    !servletRequest.getRequestURI().startsWith(applicationContext.getContextPath())) {

                // 发布请求初始化事件
                applicationContext.publishEvent(new ServletRequestEventObject(
                        applicationContext, servletRequest, EventType.SERVLET_REQUEST_INITIALIZED));

                sendNotFoundResponse();
            } else {
                // 查找匹配的Servlet
                Servlet servlet = applicationContext.findMatchingServlet(servletRequest);

                // 发布请求初始化事件
                applicationContext.publishEvent(new ServletRequestEventObject(
                        applicationContext, servletRequest, EventType.SERVLET_REQUEST_INITIALIZED));

                if (servlet != null) {
                    try {
                        // 构建并执行过滤器链
                        FilterChain filterChain = applicationContext.buildFilterChain(servletRequest, servlet);
                        filterChain.doFilter(servletRequest, servletResponse);

                        if (!servletResponse.isCommitted()) {
                            servletResponse.flushBuffer();
                        }
                    } catch (Exception e) {
                        logger().error("Error processing request", e);
                        sendErrorResponse(e.getMessage());
                    }
                }
            }

            if (keepAlive(servletRequest))
                return 0;
            else
                return -1;
        } catch (IOException e) {
            logger().error("Error processing request", e);
            return -1;
        } finally {
            try {
                if (servletRequest != null) {
                    // 调用请求销毁方法
                    if (servletRequest instanceof Lifecycle) {
                        ((Lifecycle)servletRequest).destroy();
                    }
                    // 发布销毁事件
                    applicationContext.publishEvent(new ServletRequestEventObject(
                            applicationContext, servletRequest, EventType.SERVLET_REQUEST_DESTROYED));
                }
            } catch (Exception e) {
                logger().error("Error while cleaning up request resources", e);
            }
        }
    }

    protected HttpServletRequest buildRequest(Sock<S> socket, ApplicationContext applicationContext,
                                              HttpServletResponse servletResponse) throws IOException {
        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();

        // 状态标记
        boolean headersComplete = false;
        int contentLength = -1;
        ApplicationRequest servletRequest = null;

        Object buffer = allocTmpBuf();
        int len;
        int totalBodyRead = 0;

        // 读取并解析请求
        while ((len = read(buffer)) != -1) {
            if (!headersComplete) {
                // 还在处理请求头
                write(headerOutputStream, buffer, 0, len);
                String headers = headerOutputStream.toString(StandardCharsets.UTF_8.name());
                int headerEnd = headers.indexOf("\r\n\r\n");

                if (headerEnd != -1) {
                    // 找到请求头结束标记
                    headersComplete = true;

                    // 解析请求头部分
                    String headerContent = headers.substring(0, headerEnd);
                    String[] lines = headerContent.split("\r\n");

                    // 创建request对象并设置基本信息
                    servletRequest = new ApplicationRequest(applicationContext, socket, lines);
                    prepareHeaders(lines, servletRequest);
                    prepareRemoteInfo(socket, servletRequest);
                    prepareLocalInfo(socket, servletRequest);
                    prepareServerInfo(socket, servletRequest);

                    // 获取Content-Length
                    contentLength = servletRequest.getIntHeader("Content-Length");
                    if (contentLength <= 0) {
                        break;  // 没有请求体，直接结束
                    }

                    // 处理已经读取的body部分
                    int headerBytesLength = headerEnd + 4; // 包含\r\n\r\n
                    int remainingInBuffer = headerOutputStream.size() - headerBytesLength;

                    if (remainingInBuffer > 0) {
                        bodyOutputStream.write(
                                headerOutputStream.toByteArray(),
                                headerBytesLength,
                                remainingInBuffer
                        );
                        totalBodyRead += remainingInBuffer;

                        if (totalBodyRead >= contentLength) {
                            break;  // 已读完所需数据
                        }
                    }
                }
            } else {
                // 处理请求体
                int bytesToRead = Math.min(len, contentLength - totalBodyRead);
                if (bytesToRead > 0) {
                    write(bodyOutputStream, buffer, 0, bytesToRead);
                    totalBodyRead += bytesToRead;

                    if (totalBodyRead >= contentLength) {
                        break;  // 已读完所需数据
                    }
                }
            }
        }

        if (len == -1) {
            throw new SocketCloseException();
        }

        if (servletRequest == null) {
            throw new RequestParseException("Invalid HTTP request: headers not found");
        }

        // 设置请求体
        byte[] bodyContent = bodyOutputStream.size() > 0 ? bodyOutputStream.toByteArray() : new byte[0];
        servletRequest.setBody(bodyContent);

        // 如果是multipart请求，解析multipart内容
        String contentType = servletRequest.getContentType();
        if (contentType != null) {
            if (contentType.startsWith("multipart/form-data")) {
                return new MultipartHttpServletRequest(servletRequest);
            }
        }
        servletRequest.setServletResponse(servletResponse);
        return servletRequest;
    }

    private void prepareLocalInfo(Sock<S> socket, ApplicationRequest servletRequest) {
        InetSocketAddress localAddress = socket.getLocalAddress();
        String localAddr = localAddress.getAddress().getHostAddress();
        String localName = socket.getLocalAddress().getHostName();
        int localPort = localAddress.getPort();
        servletRequest.setLocalInfo(localAddr, localName, localPort);
    }

    private void prepareServerInfo(Sock<S> socket, ApplicationRequest servletRequest) {
        String serverName = "localhost";
        int serverPort = 8080;
        boolean isSecure;

        try {
            // 尝试从请求头中获取 Host
            String host = servletRequest.getHeader("Host");
            if (host != null) {
                // 解析 Host 头，格式可能是：hostname:port 或 hostname
                int colonIndex = host.indexOf(':');
                if (colonIndex != -1) {
                    serverName = host.substring(0, colonIndex);
                    serverPort = Integer.parseInt(host.substring(colonIndex + 1));
                } else {
                    serverName = host;
                    serverPort = 80;
                }
            } else {
                // 如果没有 Host 头，使用本地地址和端口
                serverName = socket.getLocalAddress().getHostName();
                serverPort = socket.getLocalAddress().getPort();
            }
        } catch (Exception e) {
            // 解析失败时使用默认值，已在变量初始化时设置
        }

        // 检查是否是安全连接
        isSecure = servletRequest.getProtocol() != null &&
                servletRequest.getProtocol().toLowerCase().startsWith("https");

        // 只调用一次 setServerInfo
        servletRequest.setServerInfo(serverName, serverPort, isSecure);
    }

    private void prepareRemoteInfo(Sock<S> socket, ApplicationRequest servletRequest) {
        InetSocketAddress remoteAddress = socket.getRemoteAddress();
        servletRequest.setRemoteAddr(remoteAddress.getAddress().getHostAddress());
        servletRequest.setRemoteHost(remoteAddress.getHostName());
        servletRequest.setRemotePort(remoteAddress.getPort());

        // 处理认证信息
        String authHeader = servletRequest.getHeader("Authorization");
        if (authHeader != null) {
            int space = authHeader.indexOf(' ');
            if (space > 0) {
                String authType = authHeader.substring(0, space).toUpperCase();
                servletRequest.setAuthType(authType);

                // 如果是 Basic 认证，设置 remoteUser
                if ("BASIC".equalsIgnoreCase(authType)) {
                    try {
                        String base64Credentials = authHeader.substring(space + 1).trim();
                        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                        String username = credentials.split(":")[0];
                        servletRequest.setRemoteUser(username);
                    } catch (Exception ignore) {
                    }
                }
            }
        }
    }

    private void prepareHeaders(String[] lines, ApplicationRequest servletRequest) {
        HttpHeaders headers = HttpHeaders.parse(lines);
        servletRequest.setHeaders(headers);
    }

    protected boolean keepAlive(HttpServletRequest servletRequest) {
        String connection = servletRequest.getHeader("connection");
        return HttpHeaders.KEEP_ALIVE.equalsIgnoreCase(connection)
                || HttpHeaders.UPGRADE.equalsIgnoreCase(connection);
    }


    String notFoundResponse() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 23\r\n\r\n" +
                "<h1>404 Not Found</h1>";
    }

    String errorResponse(String message) {
        message = message == null ? "" : message;
        return "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + message.length() + "\r\n\r\n" +
                message;
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void destroy() throws Exception {

    }
}
