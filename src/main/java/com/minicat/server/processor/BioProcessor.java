package com.minicat.server.processor;

import com.minicat.server.HttpServletRequest;
import com.minicat.server.HttpServletResponse;
import com.minicat.server.ServletContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServlet;
import java.io.*;
import java.net.Socket;

/**
 * BIO模式的请求处理器
 */
public class BioProcessor extends Processor {
    private static final Logger logger = LoggerFactory.getLogger(BioProcessor.class);
    private final ServletContext servletContext;
    private final Socket socket;
    
    public BioProcessor(ServletContext servletContext, Socket socket) {
        this.servletContext = servletContext;
        this.socket = socket;
    }
    
    @Override
    public void process() throws Exception {
        try (InputStream inputStream = socket.getInputStream();
             OutputStream outputStream = socket.getOutputStream()) {

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
                    String method = requestLine[0];
                    String uri = requestLine[1];
                    String protocol = requestLine[2];

                    logger.debug("Received request for URI: {}", uri);

                    // 处理context-path
                    if (!servletContext.getContextPath().isEmpty() && !uri.startsWith(servletContext.getContextPath())) {
                        sendNotFoundResponse(outputStream);
                        return;
                    }

                    // 创建Request和Response对象
                    HttpServletRequest servletRequest = new HttpServletRequest(method, uri, protocol);
                    servletRequest.setServletContext(servletContext);
                    HttpServletResponse servletResponse = new HttpServletResponse(outputStream);
                    servletResponse.setServletContext(servletContext);

                    // 查找匹配的Servlet
                    HttpServlet servlet = servletContext.findMatchingServlet(uri);
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
        }
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
