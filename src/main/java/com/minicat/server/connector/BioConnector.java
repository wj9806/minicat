package com.minicat.server.connector;

import com.minicat.config.ServerConfig;
import com.minicat.server.ServletContext;
import com.minicat.server.processor.BioProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * BIO连接器实现
 */
public class BioConnector implements ServerConnector {
    private static final Logger logger = LoggerFactory.getLogger(BioConnector.class);
    private final String name = "BioConnector";
    private final ServerConfig config;
    private final ServletContext servletContext;
    private final ThreadPoolExecutor threadPool;
    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public BioConnector(ThreadPoolExecutor threadPool, ServletContext servletContext, ServerConfig config) {
        this.threadPool = threadPool;
        this.servletContext = servletContext;
        this.config = config;
    }

    @Override
    public void init() throws Exception {
        // BIO模式下初始化比较简单，主要是准备ServerSocket
        try {
            serverSocket = new ServerSocket(config.getPort());
        } catch (IOException e) {
            logger.error("Failed to initialize " + getName(), e);
            throw e;
        }
    }

    @Override
    public void start() throws Exception {
        running = true;

        try {
            while (running) {
                Socket socket = serverSocket.accept();
                handleSocket(socket);
            }
        } catch (IOException e) {
            if (running) {
                logger.error("Error accepting connection", e);
                throw e;
            }
        }
    }

    @Override
    public void stop() throws Exception {
        running = false;
        logger.info("{} stopping...", getName());

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
                throw e;
            }
        }
    }

    @Override
    public void destroy() throws Exception {

    }

    @Override
    public String getName() {
        return name;
    }

    private void handleSocket(Socket socket) {
        Runnable task = () -> {
            try {
                BioProcessor processor = new BioProcessor(servletContext, socket);
                processor.process();
            } catch (Exception e) {
                logger.error("Error processing request", e);
            } finally {
                try {
                    socket.close();
                } catch (Exception e) {
                    logger.error("Error closing socket", e);
                }
            }
        };

        if (config.isThreadPoolEnabled() && threadPool != null) {
            threadPool.execute(task);
        } else {
            task.run();
        }
    }
}
