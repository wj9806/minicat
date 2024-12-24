package com.minicat.server.connector;

import com.minicat.server.config.ServerConfig;
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
import java.util.stream.Collectors;

/**
 * BIO连接器实现
 */
public class BioConnector implements ServerConnector {
    private static final Logger logger = LoggerFactory.getLogger(BioConnector.class);
    private final ServerConfig config;
    private final ApplicationContext applicationContext;
    private final Worker worker;
    private volatile boolean running = false;
    private ServerSocket serverSocket;
    private final BioAcceptor acceptor;
    private final List<BioProcessor> processors;
    private final ScheduledExecutorService executorService;

    public BioConnector(Worker worker, ApplicationContext applicationContext, ServerConfig config) {
        this.worker = worker;
        this.applicationContext = applicationContext;
        this.config = config;
        this.acceptor = new BioAcceptor();
        this.processors = new CopyOnWriteArrayList<>();
        this.executorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setName("Monitor");
            return t;
        });
    }

    @Override
    public void init() throws Exception {
        // BIO模式下初始化比较简单，主要是准备ServerSocket
        try {
            serverSocket = new ServerSocket(config.getPort());
        } catch (IOException e) {
            logger.error("Failed to initialize {}", getName(), e);
            throw e;
        }
    }

    @Override
    public void start() throws Exception {
        running = true;
        acceptor.start();
        executorService.scheduleWithFixedDelay(new Monitor(), 50, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() throws Exception {
        running = false;
        acceptor.interrupt();
        logger.info("{} stopping...", getName());
        executorService.shutdown();

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

    private void handleSocket(Socket socket) {
        Runnable task = () -> {
            BioProcessor processor = null;
            try {
                processor = new BioProcessor(applicationContext, socket);
                processors.add(processor);
                while (true) {
                    if (processor.process() == -1)
                        break;
                }
                processors.remove(processor);
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

        if (config.isWorkerEnabled() && worker != null) {
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

    class Monitor implements Runnable {
        @Override
        public void run() {
            List<BioProcessor> removed = processors.stream()
                    .filter(p -> System.currentTimeMillis() - p.getLastProcess() > 35000)
                    .collect(Collectors.toList());
            processors.removeAll(removed);
            for (BioProcessor processor : removed) {
                try {
                    processor.destroy();
                } catch (Exception e) {
                    logger.error("remove processor failed", e);
                }
            }
        }
    }
}
