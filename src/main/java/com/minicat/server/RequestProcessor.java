package com.minicat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.http.HttpServlet;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RequestProcessor {
    private static final Logger logger = LoggerFactory.getLogger(RequestProcessor.class);
    private final ServletContext servletContext;

    public RequestProcessor(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public void process(Socket socket) throws Exception {
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
        String notFoundResponse = "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 23\r\n\r\n" +
                "<h1>404 Not Found</h1>";
        outputStream.write(notFoundResponse.getBytes());
    }

    private void sendErrorResponse(OutputStream outputStream, String message) throws Exception {
        String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + message.length() + "\r\n\r\n" +
                message;
        outputStream.write(errorResponse.getBytes());
    }
}
