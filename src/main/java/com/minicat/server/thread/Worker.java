package com.minicat.server.thread;

import com.minicat.server.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * MiniCat自定义线程池
 * 执行策略：
 * 1. 优先使用核心线程处理任务
 * 2. 如果核心线程都忙，则创建新线程直到最大线程数
 * 3. 如果达到最大线程数，则将任务放入队列等待
 * 4. 如果队列已满，则执行拒绝策略
 */
public class Worker extends ThreadPoolExecutor implements Lifecycle {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);
    
    public Worker(int corePoolSize,
                  int maximumPoolSize,
                  long keepAliveTime,
                  TimeUnit unit,
                  BlockingQueue<Runnable> workQueue,
                  ThreadFactory threadFactory,
                  RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {
        super.beforeExecute(t, r);
        logger.trace("Thread {} is about to execute task", t.getName());
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t != null) {
            logger.error("Task execution failed", t);
        } else {
            logger.trace("Task execution completed successfully");
        }
    }

    @Override
    protected void terminated() {
        super.terminated();
        logger.info("MiniCat Worker pool has been terminated");
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {
        this.shutdown();
        try {
            if (!this.awaitTermination(60, TimeUnit.SECONDS)) {
                this.shutdownNow();
            }
        } catch (InterruptedException e) {
            this.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void destroy() throws Exception {

    }
}
