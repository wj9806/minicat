package io.github.wj9806.minicat;

import io.github.wj9806.minicat.core.Lifecycle;
import io.github.wj9806.minicat.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MiniCat implements Lifecycle {
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

    public int getPort() {
        return server.getPort();
    }

    @Override
    public void init(){
        try {
            server.init();
        } catch (Exception e) {
            logger.error("Server init failed", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        try {
            server.start();
        } catch (Exception e) {
            logger.error("Server startup failed", e);
        }
    }

    @Override
    public void stop() throws Exception {
        server.stop();
    }

    @Override
    public void destroy() throws Exception {
        server.destroy();
    }
}
