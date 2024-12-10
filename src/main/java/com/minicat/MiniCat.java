package com.minicat;

import com.minicat.example.filter.LoggingFilter;
import com.minicat.example.filter.TestFilter;
import com.minicat.example.servlet.FormServlet;
import com.minicat.example.servlet.HelloServlet;
import com.minicat.example.servlet.JsonServlet;
import com.minicat.example.servlet.MultipartServlet;
import com.minicat.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniCat {
    private static final Logger logger = LoggerFactory.getLogger(MiniCat.class);

    private final HttpServer server;

    public MiniCat(int port) {
        server = new HttpServer(port);
    }

    public MiniCat()  {
        server = new HttpServer();
    }

    public void start() {
        try {
            server.init();
            server.start();
        } catch (Exception e) {
            logger.error("Server startup failed", e);
        }
    }

    public static void main(String[] args) {
        MiniCat miniCat = new MiniCat();
        miniCat.server.addServlet(new FormServlet(), "/form");
        miniCat.server.addServlet(new MultipartServlet(), "/file/upload");
        miniCat.server.addServlet(new HelloServlet(), "/hello.html");
        miniCat.server.addServlet(new JsonServlet(), "/json", "/json2", "/json/*");

        miniCat.server.addFilter(new LoggingFilter(), "/*");
        miniCat.server.addFilter(new TestFilter(), "/");
        miniCat.start();
    }
}
