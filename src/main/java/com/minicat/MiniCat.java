package com.minicat;

import com.minicat.example.HelloServlet;
import com.minicat.example.JsonServlet;
import com.minicat.example.MultipartServlet;
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
        miniCat.server.addServlet(new MultipartServlet(), "/file/upload");
        miniCat.server.addServlet(new HelloServlet(), "/hello.html");
        miniCat.server.addServlet(new JsonServlet(), "/json", "/json2", "/json/*");
        miniCat.start();
    }
}
