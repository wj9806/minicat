package com.minicat.server.thread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Minicat自定义线程池
 * 执行策略：
 * 1. 优先使用核心线程处理任务
 * 2. 如果核心线程都忙，则创建新线程直到最大线程数
 * 3. 如果达到最大线程数，则将任务放入队列等待
 * 4. 如果队列已满，则执行拒绝策略
 */
public class MinicatThreadPool extends ThreadPoolExecutor {
    private static final Logger logger = LoggerFactory.getLogger(MinicatThreadPool.class);
    
    public MinicatThreadPool(int corePoolSize,
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
        logger.info("Minicat Worker pool has been terminated");
    }
}
