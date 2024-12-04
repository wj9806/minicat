package com.minicat;

import com.minicat.example.HelloServlet;
import com.minicat.example.JsonServlet;
import com.minicat.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap {
    private static final Logger logger = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args) {
        try {
            HttpServer server = new HttpServer();

            // 注册具体的Servlet（优先级高）
            server.addServlet("/hello", new HelloServlet());
            server.addServlet("/json", new JsonServlet());
            server.start();
        } catch (Exception e) {
            logger.error("Server startup failed", e);
        }
    }
}
