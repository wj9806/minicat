package com.minicat.server.processor;

/**
 * 请求处理器接口
 */
public abstract class Processor {
    /**
     * 处理请求
     * @throws Exception 处理过程中可能出现的异常
     */
    protected abstract void process() throws Exception;

    String notFoundResponse() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: 23\r\n\r\n" +
                "<h1>404 Not Found</h1>";
    }

    String errorResponse(String message) {
        return "HTTP/1.1 500 Internal Server Error\r\n" +
                "Content-Type: text/html\r\n" +
                "Content-Length: " + message.length() + "\r\n\r\n" +
                message;
    }
}
