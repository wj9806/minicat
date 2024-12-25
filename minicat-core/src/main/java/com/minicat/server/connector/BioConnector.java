package com.minicat.server.connector;

import com.minicat.net.Sock;
import com.minicat.server.config.Config;
import com.minicat.core.ApplicationContext;
import com.minicat.server.processor.BioProcessor;
import com.minicat.server.thread.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.*;

/**
 * BIO连接器实现
 */
public class BioConnector implements ServerConnector<Socket> {
    private static final Logger logger = LoggerFactory.getLogger(BioConnector.class);
    private final Config config;
    private final ApplicationContext applicationContext;
    private final Worker worker;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final BioAcceptor acceptor;
    private final List<Sock<Socket>> socks;

    public BioConnector(Worker worker, ApplicationContext applicationContext, Config config) {
        this.worker = worker;
        this.applicationContext = applicationContext;
        this.config = config;
        this.acceptor = new BioAcceptor();
        this.socks = new CopyOnWriteArrayList<>();
    }

    @Override
    public void init() throws Exception {
        // BIO模式下初始化比较简单，主要是准备ServerSocket
        try {
            serverSocket = new ServerSocket(config.getServer().getPort());
        } catch (IOException e) {
            logger.error("Failed to initialize {}", getName(), e);
            throw e;
        }
    }

    @Override
    public void start() throws Exception {
        running = true;
        acceptor.start();
    }

    @Override
    public void stop() throws Exception {
        running = false;
        acceptor.interrupt();
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
        return "BioConnector";
    }

    @Override
    public List<Sock<Socket>> getSocks() {
        return socks;
    }

    public void addSock(Sock<Socket> sock) {
        socks.add(sock);
    }

    public void removeSock(Sock<Socket> sock) {
        socks.remove(sock);
    }

    private void handleSocket(Socket socket) {
        Runnable task = () -> {
            BioProcessor processor = null;
            try {
                processor = new BioProcessor(applicationContext, socket);
                Sock<Socket> sock = processor.sock();
                addSock(sock);
                while (true) {
                    if (processor.process() == -1)
                        break;
                }
                removeSock(sock);
            } catch (Exception e) {
                logger.error("Error processing request", e);
            } finally {
                try {
                    if (processor != null)
                        processor.destroy();
                } catch (Exception e) {
                    logger.error("Error closing socket", e);
                }
            }
        };

        if (config.getServer().getWorker().isEnabled() && worker != null) {
            worker.execute(task);
        } else {
            task.run();
        }
    }

    class BioAcceptor extends Thread {

        public BioAcceptor() {
            super("Acceptor");
        }

        @Override
        public void run() {
            try {
                while (running) {
                    Socket socket = serverSocket.accept();
                    handleSocket(socket);
                }
            } catch (IOException e) {
                if (running) {
                    logger.error("Error accepting connection", e);
                    throw new RuntimeException(e);
                }
            }
        }
    }

}
