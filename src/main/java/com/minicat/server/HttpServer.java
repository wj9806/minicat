package com.minicat.server;

import com.minicat.core.ApplicationContext;
import com.minicat.core.Lifecycle;
import com.minicat.server.config.ServerConfig;
import com.minicat.server.connector.BioConnector;
import com.minicat.server.connector.ServerConnector;
import com.minicat.server.thread.WorkerQueue;
import com.minicat.server.thread.Worker;
import com.minicat.util.BannerPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import java.util.EnumSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer implements Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    
    private int port;
    private String contextPath;
    private String staticPath;
    private Worker worker;
    private final ServerConfig config;
    private volatile boolean running = false;
    private final ApplicationContext applicationContext;
    private final ServerConnector connector;
    private final long startTime;

    public HttpServer() {
        this(ServerConfig.getInstance().getPort());
    }

    public HttpServer(int port) {
        this.startTime = System.currentTimeMillis();
        this.config = ServerConfig.getInstance();
        this.config.setPort(port);
        if (config.isShowBanner()) {
            BannerPrinter.printBanner();
        }
        this.port = port;
        this.contextPath = config.getContextPath();
        this.staticPath = config.getStaticPath();
        this.applicationContext = new ApplicationContext(this.config);
        //创建工作线程
        this.createWorker();
        this.connector = new BioConnector(worker, applicationContext, config);
    }

    @Override
    public void init() throws Exception {
        // 打印启动信息
        printStartupInfo();
        // 注册关闭钩子
        registerShutdownHook();
        //初始化工作线程
        initWorker();

        this.applicationContext.init();
        this.connector.init();
    }

    @Override
    public void start() throws Exception {
        this.applicationContext.start();
        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("MiniCat server started in {} ms ", totalTime);

        this.running = true;
        this.connector.start();
    }

    @Override
    public void stop() throws Exception {
        this.applicationContext.stop();
        logger.info("Stopping MiniCat server...");
        this.running = false;
        this.connector.stop();

        if (worker != null) {
            worker.stop();
        }
        logger.info("MiniCat Server stopped");
    }

    @Override
    public void destroy() throws Exception {
        logger.info("Destroying MiniCat server...");
        
        // 销毁所有 Servlet
        this.applicationContext.destroy();
        this.connector.destroy();
        
        logger.info("MiniCat Server destroyed");
    }

    private void createWorker() {
        if (!config.isWorkerEnabled()) return;

        WorkerQueue taskQueue = new WorkerQueue(config.getWorkerQueueSize());
        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName("minicat-worker-" + threadNumber.getAndIncrement());
            return thread;
        };

        worker = new Worker(
            config.getWorkerCoreSize(),
            config.getWorkerMaxSize(),
            config.getWorkerKeepAliveTime(),
            TimeUnit.SECONDS,
            taskQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        taskQueue.setExecutor(worker);
    }

    private void initWorker() {
        if (!config.isWorkerEnabled()) return;

        // 允许核心线程超时
        worker.allowCoreThreadTimeOut(true);

        // 预启动所有核心线程
        worker.prestartAllCoreThreads();
    }

    private void printStartupInfo() {
        logger.info("MiniCat context path: {}", contextPath.isEmpty() ? "/" : contextPath);

        if (config.isWorkerEnabled()) {
            logger.info("MiniCat worker pool: enabled, core={}, max={}, queueSize={}",
                    config.getWorkerCoreSize(),
                    config.getWorkerMaxSize(),
                    config.getWorkerQueueSize());
        } else {
            logger.info("MiniCat worker pool: disabled");
        }
        logger.info("MiniCat start on port: {}", port);
    }
    
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                stop();
                destroy();
            } catch (Exception e) {
                logger.error("Error shutting down server", e);
            }
        }, "MiniCat-Destroy-Hook"));
    }

    public void addServlet(HttpServlet servlet, String... urls) {
        String className = servlet.getClass().getSimpleName();
        String servletName = "servlet_" + className;
        
        // 尝试添加 servlet
        ServletRegistration.Dynamic registration = applicationContext.addServlet(servletName, servlet);
        if (registration == null) {
            // 如果返回 null，说明该名称已存在
            logger.warn("Failed to add servlet: name='{}', class='{}'", servletName, className);
            return;
        }

        logger.debug("Adding servlet: name='{}', class='{}', urls='{}'", servletName, className, String.join(",", urls));

        // 添加映射
        registration.addMapping(urls);
    }

    public void addFilter(Filter filter, String... urls) {
        String className = filter.getClass().getSimpleName();
        String filterName = "filter_" + className;

        FilterRegistration.Dynamic registration = applicationContext.addFilter(filterName, filter);
        if (registration == null) {
            // 如果返回 null，说明该名称已存在
            logger.warn("Failed to add filter: name='{}', class='{}'", filterName, className);
            return;
        }

        logger.debug("Adding filter: name='{}', class='{}', urls='{}'", filterName, className, String.join(",", urls));

        // 添加映射
        registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, urls);
    }

    public boolean isRunning() {
        return running;
    }
}
