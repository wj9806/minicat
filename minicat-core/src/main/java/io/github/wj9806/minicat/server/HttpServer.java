package io.github.wj9806.minicat.server;

import io.github.wj9806.minicat.core.ApplicationContext;
import io.github.wj9806.minicat.core.Lifecycle;
import io.github.wj9806.minicat.server.config.Config;
import io.github.wj9806.minicat.server.config.ServerConfig;
import io.github.wj9806.minicat.server.connector.BioConnector;
import io.github.wj9806.minicat.server.connector.NioConnector;
import io.github.wj9806.minicat.server.connector.ServerConnector;
import io.github.wj9806.minicat.server.thread.WorkerQueue;
import io.github.wj9806.minicat.server.thread.Worker;
import io.github.wj9806.minicat.util.BannerPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpServer implements Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    
    private int port;
    private String contextPath;
    private List<String> staticPath;
    private Worker worker;
    private final Config config;
    private volatile boolean running = false;
    private volatile boolean stopped = false;
    private volatile boolean destroyed = false;
    private final ApplicationContext applicationContext;
    private final ServerConnector<?> connector;
    private final ScheduledExecutorService monitorService;
    private final long startTime;

    public HttpServer() {
        this(Config.getInstance().getServer().getPort());
    }

    public HttpServer(int port) {
        this.startTime = System.currentTimeMillis();
        this.config = Config.getInstance();

        ServerConfig server = this.config.getServer();
        server.setPort(port);
        if (server.isShowBanner()) {
            BannerPrinter.printBanner();
        }
        this.port = port;
        this.contextPath = server.getContextPath();
        this.staticPath = server.getStaticPath();
        this.applicationContext = new ApplicationContext(this.config);
        //创建工作线程
        this.createWorker();
        this.connector = server.nioEnabled()
                ? new NioConnector(worker, applicationContext, config)
                : new BioConnector(worker, applicationContext, config);
        this.monitorService = new ScheduledThreadPoolExecutor(1, r -> {
            Thread t = new Thread(r);
            t.setName("Monitor");
            return t;
        });
    }

    @Override
    public void init() throws Exception {
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
        this.running = true;
        this.connector.start();
        this.monitorService.scheduleWithFixedDelay(new Monitor(Collections.singletonList(connector)),
                50, 50, TimeUnit.MILLISECONDS);
        // 打印启动信息
        printStartupInfo();
    }

    @Override
    public synchronized void stop() throws Exception {
        if (stopped) return;
        this.applicationContext.stop();
        logger.info("Stopping MiniCat server...");
        this.running = false;
        this.connector.stop();

        if (worker != null) {
            worker.stop();
        }
        logger.info("MiniCat Server stopped");
        stopped = true;
    }

    @Override
    public synchronized void destroy() throws Exception {
        if (destroyed) return;
        logger.info("Destroying MiniCat server...");
        
        // 销毁所有 Servlet
        this.applicationContext.destroy();
        this.connector.destroy();
        this.monitorService.shutdown();
        
        logger.info("MiniCat Server destroyed");
        destroyed = true;
    }

    private void createWorker() {
        ServerConfig.WorkerConfig workerConfig = config.getServer().getWorker();
        if (!workerConfig.isEnabled()) return;

        WorkerQueue taskQueue = new WorkerQueue(workerConfig.getQueueSize());
        AtomicInteger threadNumber = new AtomicInteger(1);
        ThreadFactory threadFactory = r -> {
            Thread thread = new Thread(r);
            thread.setName(config.getServer().getMode() + "-worker-" + threadNumber.getAndIncrement());
            return thread;
        };

        worker = new Worker(
            workerConfig.getCoreSize(),
            workerConfig.getMaxSize(),
            workerConfig.getKeepAliveTime(),
            TimeUnit.SECONDS,
            taskQueue,
            threadFactory,
            new ThreadPoolExecutor.CallerRunsPolicy()
        );

        taskQueue.setExecutor(worker);
    }

    private void initWorker() {
        if (!config.getServer().getWorker().isEnabled()) return;

        // 允许核心线程超时
        //worker.allowCoreThreadTimeOut(true);
        worker.prestartAllCoreThreads();
    }

    private void printStartupInfo() {
        logger.info("MiniCat context path: {}", contextPath.isEmpty() ? "/" : contextPath);

        ServerConfig.WorkerConfig workerConfig = config.getServer().getWorker();
        if (workerConfig.isEnabled()) {
            logger.info("MiniCat worker pool: enabled, core={}, max={}, queueSize={}",
                    workerConfig.getCoreSize(),
                    workerConfig.getMaxSize(),
                    workerConfig.getQueueSize());
        } else {
            logger.info("MiniCat worker pool: disabled");
        }
        logger.info("MiniCat start on port: {}", port);

        long totalTime = System.currentTimeMillis() - startTime;
        logger.info("MiniCat server started in {} ms ", totalTime);
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
        if (urls.length > 0) {
            registration.addMapping(urls);
        }
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

    public int getPort() {
        return port;
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }
}
