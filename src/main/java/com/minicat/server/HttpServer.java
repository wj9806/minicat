package com.minicat.server;

import com.minicat.config.ServerConfig;
import com.minicat.server.thread.MinicatTaskQueue;
import com.minicat.server.thread.MinicatThreadPool;
import com.minicat.util.BannerPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    
    private int port;
    private String contextPath;
    private String staticPath;
    private ThreadPoolExecutor threadPool;
    private ServerConfig config;
    private volatile boolean running = false;
    private ServletContext servletContext;

    public HttpServer() {
        config = ServerConfig.getInstance();
        if (config.isShowBanner()) {
            BannerPrinter.printBanner();
        }
        this.port = config.getPort();
        this.contextPath = config.getContextPath();
        this.staticPath = config.getStaticPath();
        if (config.isThreadPoolEnabled()) {
            initThreadPool();
        }
        this.servletContext = new ServletContext(contextPath, staticPath);
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
    
    public HttpServer(int port) {
        this();
        this.port = port;  // 允许通过构造函数覆盖配置文件中的端口
    }
    
    public void start() throws Exception {
        logger.info("Server is starting...");
        running = true;

        // 打印启动信息
        printStartupInfo();

        // 注册关闭钩子
        registerShutdownHook();

        // 启动服务器并处理请求
        try (ServerSocket serverSocket = new ServerSocket(port)) {
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
        }
    }
    
    private void printStartupInfo() {
        logger.info("=====>>>minicat start on port: {}", port);
        logger.info("=====>>>context path: {}", contextPath);

        if (config.isThreadPoolEnabled()) {
            logger.info("=====>>>thread pool: enabled, core={}, max={}, queueSize={}", 
                    config.getThreadPoolCoreSize(),
                    config.getThreadPoolMaxSize(),
                    config.getThreadPoolQueueSize());
        } else {
            logger.info("=====>>>thread pool: disabled");
        }
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                shutdown();
            } catch (Exception e) {
                logger.error("Error shutting down server", e);
            }
        }));
    }
    
    private void handleRequest(Socket socket) {
        Runnable task = () -> {
            try {
                processRequest(socket);
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
    
    public void shutdown() {
        logger.info("Shutting down server...");
        running = false;

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
        logger.info("Server shutdown complete");
    }
    
    private void processRequest(Socket socket) throws Exception {
        InputStream inputStream = socket.getInputStream();
        OutputStream outputStream = socket.getOutputStream();

        // 解析HTTP请求
        StringBuilder requestBuilder = new StringBuilder();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            requestBuilder.append(new String(buffer, 0, len));
            if (requestBuilder.toString().contains("\r\n\r\n")) {
                break;
            }
        }

        String request = requestBuilder.toString();
        String[] lines = request.split("\r\n");
        if (lines.length > 0) {
            String[] requestLine = lines[0].split(" ");
            if (requestLine.length >= 3) {
                String method = requestLine[0];
                String uri = requestLine[1];
                String protocol = requestLine[2];

                logger.debug("Received request for URI: {}", uri);

                // 处理context-path
                if (!contextPath.isEmpty() && !uri.startsWith(contextPath)) {
                    String notFoundResponse = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: 23\r\n\r\n" +
                            "<h1>404 Not Found</h1>";
                    outputStream.write(notFoundResponse.getBytes());
                    return;
                }

                // 创建Request和Response对象
                HttpServletRequest servletRequest = new HttpServletRequest(method, uri, protocol);
                servletRequest.setServletContext(servletContext);
                HttpServletResponse servletResponse = new HttpServletResponse(outputStream);
                servletResponse.setServletContext(servletContext);

                // 查找匹配的Servlet
                HttpServlet servlet = servletContext.findMatchingServlet(uri);
                if (servlet != null) {
                    try {
                        servlet.service(servletRequest, servletResponse);
                        if (!servletResponse.isCommitted()) {
                            servletResponse.flushBuffer();
                        }
                    } catch (Exception e) {
                        logger.error("Error processing request", e);
                        String errorResponse = "HTTP/1.1 500 Internal Server Error\r\n" +
                                "Content-Type: text/html\r\n" +
                                "Content-Length: " + e.getMessage().length() + "\r\n\r\n" +
                                e.getMessage();
                        outputStream.write(errorResponse.getBytes());
                    }
                }
            }
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
