package com.minicat.spring.boot;

import com.minicat.MiniCat;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;

public class MiniCatWebServer implements WebServer {

    private final MiniCat miniCat;

    public MiniCatWebServer(MiniCat miniCat) {
        this.miniCat = miniCat;
        miniCat.init();
    }

    @Override
    public void start() throws WebServerException {
        miniCat.start();
    }

    @Override
    public void stop() throws WebServerException {
        try {
            miniCat.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getPort() {
        return miniCat.getPort();
    }
}
