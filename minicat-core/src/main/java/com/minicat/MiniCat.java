package com.minicat;

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

    public HttpServer getServer() {
        return server;
    }

    public void start() {
        try {
            server.init();
            server.start();
        } catch (Exception e) {
            logger.error("Server startup failed", e);
        }
    }
}
