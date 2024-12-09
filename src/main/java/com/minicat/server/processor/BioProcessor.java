package com.minicat.server.processor;

import com.minicat.http.*;
import com.minicat.core.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * BIO模式的请求处理器
 */
public class BioProcessor extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(BioProcessor.class);
    private final ApplicationContext applicationContext;
    private final Socket socket;

    public BioProcessor(ApplicationContext applicationContext, Socket socket) {
        this.applicationContext = applicationContext;
        this.socket = socket;
    }

    @Override
    public void process() throws Exception {
        try {
            OutputStream outputStream = socket.getOutputStream();

            // 创建Request和Response对象
            javax.servlet.http.HttpServletRequest servletRequest = null;
            try {
                servletRequest = buildRequest(socket, applicationContext);
            } catch (RequestParseException e) {
                return;
            }

            // 处理context-path
            if (!applicationContext.getContextPath().isEmpty() && !servletRequest.getRequestURI().startsWith(applicationContext.getContextPath())) {
                sendNotFoundResponse(outputStream);
                return;
            }

            HttpServletResponse servletResponse = new HttpServletResponse(outputStream);

            // 查找匹配的Servlet
            Servlet servlet = applicationContext.findMatchingServlet(servletRequest);

            if (servlet != null) {
                try {
                    servlet.service(servletRequest, servletResponse);
                    if (!servletResponse.isCommitted()) {
                        servletResponse.flushBuffer();
                    }
                } catch (Exception e) {
                    logger.error("Error processing request", e);
                    sendErrorResponse(outputStream, e.getMessage());
                }
            }
        } catch (IOException e) {
            logger.error("Error processing request", e);
        }
    }

    private javax.servlet.http.HttpServletRequest buildRequest(Socket socket, ApplicationContext applicationContext) throws IOException {
        InputStream inputStream = socket.getInputStream();
        ByteArrayOutputStream headerOutputStream = new ByteArrayOutputStream();
        ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();

        // 状态标记
        boolean headersComplete = false;
        int contentLength = -1;
        HttpServletRequest servletRequest = null;

        byte[] buffer = new byte[1024];
        int len;
        int totalBodyRead = 0;

        // 读取并解析请求
        while ((len = inputStream.read(buffer)) != -1) {
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
                    servletRequest = new HttpServletRequest(applicationContext, lines);
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

        if (servletRequest == null) {
            throw new RequestParseException("Invalid HTTP request: headers not found");
        }

        // 设置请求体
        byte[] bodyContent = bodyOutputStream.size() > 0 ? bodyOutputStream.toByteArray() : new byte[0];
        servletRequest.setBody(bodyContent);

        // 如果是multipart请求，解析multipart内容
        String contentType = servletRequest.getContentType();
        if (contentType != null && contentType.startsWith("multipart/form-data")) {
            return new MultipartHttpServletRequest(servletRequest);
        }

        return servletRequest;
    }

    private void prepareLocalInfo(Socket socket, HttpServletRequest servletRequest) {
        String localAddr = socket.getLocalAddress().getHostAddress();
        String localName = socket.getLocalAddress().getHostName();
        int localPort = socket.getLocalPort();
        servletRequest.setLocalInfo(localAddr, localName, localPort);
    }

    private void prepareServerInfo(Socket socket, HttpServletRequest servletRequest) {
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

    private void prepareRemoteInfo(Socket socket, HttpServletRequest servletRequest) {
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

    private void prepareHeaders(String[] lines, HttpServletRequest servletRequest) {
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
}
