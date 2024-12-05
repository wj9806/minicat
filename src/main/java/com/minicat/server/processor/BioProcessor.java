package com.minicat.server.processor;

import com.minicat.http.HttpServletRequest;
import com.minicat.http.HttpServletResponse;
import com.minicat.http.HttpHeaders;
import com.minicat.core.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        InputStream inputStream;
        OutputStream outputStream;
        try {
            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            // 解析HTTP请求
            StringBuilder requestBuilder = new StringBuilder();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = inputStream.read(buffer)) != -1) {
                requestBuilder.append(new String(buffer, 0, len));
                if (requestBuilder.toString().contains("\r\n\r\n")) {
                    break;
                }
            }

            String request = requestBuilder.toString();
            String[] lines = request.split("\r\n");
            if (lines.length > 0) {
                String[] requestLine = lines[0].split(" ");
                if (requestLine.length >= 3) {
                    String uri = requestLine[1];
                    logger.debug("Received request for URI: {}", uri);

                    // 处理context-path
                    if (!applicationContext.getContextPath().isEmpty() && !uri.startsWith(applicationContext.getContextPath())) {
                        sendNotFoundResponse(outputStream);
                        return;
                    }

                    // 创建Request和Response对象
                    HttpServletRequest servletRequest = buildRequest(socket, applicationContext, lines);
                    HttpServletResponse servletResponse = new HttpServletResponse(outputStream);

                    // 查找匹配的Servlet
                    HttpServlet servlet = applicationContext.findMatchingServlet(servletRequest);

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
                }
            }
        } catch (IOException e) {
            logger.error("Error processing request", e);
        }
    }

    private HttpServletRequest buildRequest(Socket socket, ApplicationContext applicationContext, String[] lines) {
        HttpServletRequest servletRequest = new HttpServletRequest(applicationContext, lines);

        // 解析并设置请求头
        prepareHeaders(lines, servletRequest);

        // 设置远程客户端信息
        prepareRemoteInfo(socket, servletRequest);

        // 设置本地连接信息
        prepareLocalInfo(socket, servletRequest);

        // 设置服务器信息
        prepareServerInfo(socket, servletRequest);

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
