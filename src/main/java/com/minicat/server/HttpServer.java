package com.minicat.server;

import com.minicat.config.ServerConfig;
import com.minicat.server.thread.MinicatTaskQueue;
import com.minicat.server.thread.MinicatThreadPool;
import com.minicat.util.BannerPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer implements Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    
    private int port;
    private String contextPath;
    private String staticPath;
    private ThreadPoolExecutor threadPool;
    private ServerConfig config;
    private volatile boolean running = false;
    private ServletContext servletContext;
    private RequestProcessor requestProcessor;
    private ServerSocket serverSocket;

    public HttpServer() throws Exception {
        config = ServerConfig.getInstance();
        if (config.isShowBanner()) {
            BannerPrinter.printBanner();
        }
        this.port = config.getPort();
        this.contextPath = config.getContextPath();
        this.staticPath = config.getStaticPath();
        this.servletContext = new ServletContext(contextPath, staticPath);
        this.requestProcessor = new RequestProcessor(servletContext);
    }
    
    public HttpServer(int port) throws Exception {
        this();
        this.port = port;
    }

    @Override
    public void init() throws Exception {
        if (config.isThreadPoolEnabled()) {
            initThreadPool();
        }
        this.servletContext.init();
        logger.info("Minicat Server initialized");
    }

    @Override
    public void start() throws Exception {
        logger.info("Starting Minicat server...");
        running = true;

        // 打印启动信息
        printStartupInfo();

        // 注册关闭钩子
        registerShutdownHook();

        //初始化
        init();

        // 启动服务器并处理请求
        openSocket();
    }

    @Override
    public void stop() throws Exception {
        logger.info("Stopping Minicat server...");
        running = false;

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                logger.error("Error closing server socket", e);
            }
        }

        if (config.isThreadPoolEnabled() && threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(60, TimeUnit.SECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        logger.info("Minicat Server stopped");
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Destroying Minicat server...");
        
        // 销毁所有 Servlet
        servletContext.destroy();
        
        // 清理资源
        if (threadPool != null) {
            threadPool.shutdownNow();
        }
        
        logger.info("Minicat Server destroyed");
    }

    private void initThreadPool() {
        MinicatTaskQueue taskQueue = new MinicatTaskQueue(config.getThreadPoolQueueSize());
        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("minicat-worker-" + threadNumber.getAndIncrement());
            return thread;
        };

        threadPool = new MinicatThreadPool(
            config.getThreadPoolCoreSize(),
            config.getThreadPoolMaxSize(),
            config.getThreadPoolKeepAliveTime(),
            TimeUnit.SECONDS,
            taskQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy() // 队列满时，在调用者线程中执行任务
        );
        
        // 设置executor引用，使队列可以访问到线程池的信息
        taskQueue.setExecutor(threadPool);

        // 允许核心线程超时
        threadPool.allowCoreThreadTimeOut(true);

        // 预启动所有核心线程
        threadPool.prestartAllCoreThreads();
    }

    private void openSocket() throws IOException {
        serverSocket = new ServerSocket(port);
        try {
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    handleRequest(socket);
                } catch (Exception e) {
                    if (running) {
                        logger.error("Error accepting connection", e);
                    }
                }
            }
        } finally {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        }
    }

    private void printStartupInfo() {
        logger.info("=====>>>minicat start on port: {}", port);
        logger.info("=====>>>context path: {}", contextPath);

        if (config.isThreadPoolEnabled()) {
            logger.info("=====>>>worker pool: enabled, core={}, max={}, queueSize={}",
                    config.getThreadPoolCoreSize(),
                    config.getThreadPoolMaxSize(),
                    config.getThreadPoolQueueSize());
        } else {
            logger.info("=====>>>worker pool: disabled");
        }
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
                destroy();
            } catch (Exception e) {
                logger.error("Error shutting down server", e);
            }
        }, "Minicat-Destroy-Hook"));
    }
    
    private void handleRequest(Socket socket) {
        Runnable task = () -> {
            try {
                requestProcessor.process(socket);
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

        if (config.isThreadPoolEnabled()) {
            threadPool.execute(task);
        } else {
            task.run();
        }
    }

    public void addServlet(String url, HttpServlet servlet) {
        String className = servlet.getClass().getSimpleName();
        String servletName = "servlet_" + className;
        
        // 尝试添加 servlet
        ServletRegistration.Dynamic registration = servletContext.addServlet(servletName, servlet);
        if (registration == null) {
            // 如果返回 null，说明该名称已存在
            logger.warn("Failed to add servlet: name='{}', class='{}', url='{}'", servletName, className, url);
            return;
        }

        logger.info("Adding servlet: name='{}', class='{}', url='{}'", servletName, className, url);

        // 添加映射
        registration.addMapping(url);
    }
}
