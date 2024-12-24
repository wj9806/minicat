package com.minicat.server.processor;

import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletRequestEventObject;
import com.minicat.http.*;
import com.minicat.core.ApplicationContext;
import com.minicat.core.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

/**
 * BIO模式的请求处理器
 */
public class BioProcessor extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(BioProcessor.class);
    private final ApplicationContext applicationContext;
    private final Socket socket;
    private final OutputStream hos;
    private final InputStream his;
    private long lastProcess;
    private boolean closed = false;

    public BioProcessor(ApplicationContext applicationContext, Socket socket) {
        this.applicationContext = applicationContext;
        this.socket = socket;
        try {
            this.hos = socket.getOutputStream();
            this.his = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.lastProcess = System.currentTimeMillis();
    }

    @Override
    public int process() throws Exception {
        HttpServletRequest servletRequest = null;
        try {
            // 创建Request和Response对象
            HttpServletResponse servletResponse = new ApplicationResponse(hos);
            try {
                servletRequest = buildRequest(socket, applicationContext, servletResponse);
            }catch (SocketCloseException | RequestParseException | SocketException e) {
                return -1;
            }

            this.lastProcess = System.currentTimeMillis();
            // 处理context-path
            if (!applicationContext.getContextPath().isEmpty() &&
                    !servletRequest.getRequestURI().startsWith(applicationContext.getContextPath())) {

                // 发布请求初始化事件
                applicationContext.publishEvent(new ServletRequestEventObject(
                        applicationContext, servletRequest, EventType.SERVLET_REQUEST_INITIALIZED));

                sendNotFoundResponse(hos);
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
                        logger.error("Error processing request", e);
                        sendErrorResponse(hos, e.getMessage());
                    }
                }
            }

            if (keepAlive(servletRequest))
                return 0;
            else
                return -1;
        } catch (IOException e) {
            logger.error("Error processing request", e);
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
                logger.error("Error while cleaning up request resources", e);
            }
        }
    }

    private HttpServletRequest buildRequest(Socket socket, ApplicationContext applicationContext,
                                            HttpServletResponse servletResponse) throws IOException {
        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();

        // 状态标记
        boolean headersComplete = false;
        int contentLength = -1;
        ApplicationRequest servletRequest = null;

        byte[] buffer = new byte[1024];
        int len;
        int totalBodyRead = 0;

        // 读取并解析请求
        while ((len = his.read(buffer)) != -1) {
            if (!headersComplete) {
                // 还在处理请求头
                headerOutputStream.write(buffer, 0, len);
                String headers = headerOutputStream.toString(StandardCharsets.UTF_8.name());
                int headerEnd = headers.indexOf("\r\n\r\n");

                if (headerEnd != -1) {
                    // 找到请求头结束标记
                    headersComplete = true;

                    // 解析请求头部分
                    String headerContent = headers.substring(0, headerEnd);
                    String[] lines = headerContent.split("\r\n");

                    // 创建request对象并设置基本信息
                    servletRequest = new ApplicationRequest(applicationContext, lines);
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
                    bodyOutputStream.write(buffer, 0, bytesToRead);
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

    private boolean keepAlive(HttpServletRequest servletRequest) {
        String connection = servletRequest.getHeader("connection");
        return Objects.equals(connection, "keep-alive");
    }

    private void prepareLocalInfo(Socket socket, ApplicationRequest servletRequest) {
        String localAddr = socket.getLocalAddress().getHostAddress();
        String localName = socket.getLocalAddress().getHostName();
        int localPort = socket.getLocalPort();
        servletRequest.setLocalInfo(localAddr, localName, localPort);
    }

    private void prepareServerInfo(Socket socket, ApplicationRequest servletRequest) {
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
                serverPort = socket.getLocalPort();
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

    private void prepareRemoteInfo(Socket socket, ApplicationRequest servletRequest) {
        servletRequest.setRemoteAddr(socket.getInetAddress().getHostAddress());
        servletRequest.setRemoteHost(socket.getInetAddress().getHostName());
        servletRequest.setRemotePort(socket.getPort());

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

    private void sendNotFoundResponse(OutputStream outputStream) throws Exception {
        String notFoundResponse = notFoundResponse();
        outputStream.write(notFoundResponse.getBytes());
    }

    private void sendErrorResponse(OutputStream outputStream, String message) throws Exception {
        String errorResponse = errorResponse(message);
        outputStream.write(errorResponse.getBytes());
    }

    @Override
    public void destroy() throws Exception {
        if (closed) return;

        closed = true;
        his.close();
        hos.close();
        socket.close();
    }

    public long getLastProcess() {
        return lastProcess;
    }
}
