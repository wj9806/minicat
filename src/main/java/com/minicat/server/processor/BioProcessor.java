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
                    HttpServlet servlet = applicationContext.findMatchingServlet(uri);
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
        String[] requestLine = lines[0].split(" ");
        String method = requestLine[0];
        String uri = requestLine[1];
        String protocol = requestLine[2];

        HttpServletRequest servletRequest = new HttpServletRequest(method, uri, protocol);
        servletRequest.setServletContext(applicationContext);
        // 设置远程客户端信息
        servletRequest.setRemoteAddr(socket.getInetAddress().getHostAddress());
        servletRequest.setRemoteHost(socket.getInetAddress().getHostName());
        servletRequest.setRemotePort(socket.getPort());

        // 解析并设置请求头
        HttpHeaders headers = HttpHeaders.parse(lines);
        servletRequest.setHeaders(headers);

        // 处理Basic认证的remoteUser
        String authHeader = servletRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Basic ")) {
            try {
                String base64Credentials = authHeader.substring("Basic ".length()).trim();
                String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
                // credentials = username:password
                String username = credentials.split(":")[0];
                servletRequest.setRemoteUser(username);
            } catch (Exception ignore) {
            }
        }

        servletRequest.setServletContext(applicationContext);
        return servletRequest;
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
