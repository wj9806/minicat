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
        logger.info("Minicat Thread pool has been terminated");
    }

    /**
     * 创建Minicat线程池构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Minicat线程池构建器
     */
    public static class Builder {
        private int corePoolSize = 10;
        private int maximumPoolSize = 50;
        private long keepAliveTime = 60;
        private TimeUnit unit = TimeUnit.SECONDS;
        private BlockingQueue<Runnable> workQueue;
        private ThreadFactory threadFactory;
        private RejectedExecutionHandler handler;

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maximumPoolSize(int maximumPoolSize) {
            this.maximumPoolSize = maximumPoolSize;
            return this;
        }

        public Builder keepAliveTime(long keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public Builder unit(TimeUnit unit) {
            this.unit = unit;
            return this;
        }

        public Builder workQueue(int queueSize) {
            // 使用有界队列，防止OOM
            this.workQueue = new LinkedBlockingQueue<>(queueSize);
            return this;
        }

        public Builder threadFactory(String poolName) {
            this.threadFactory = new DefaultThreadFactory(poolName);
            return this;
        }

        public Builder rejectedExecutionHandler(RejectedExecutionHandler handler) {
            this.handler = handler;
            return this;
        }

        public MinicatThreadPool build() {
            if (threadFactory == null) {
                threadFactory = new DefaultThreadFactory("minicat-pool");
            }
            if (handler == null) {
                handler = new ThreadPoolExecutor.CallerRunsPolicy();
            }
            if (workQueue == null) {
                workQueue = new LinkedBlockingQueue<>(100);
            }
            return new MinicatThreadPool(corePoolSize, maximumPoolSize, keepAliveTime, unit, 
                    workQueue, threadFactory, handler);
        }
    }

    /**
     * 默认线程工厂
     */
    private static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String poolName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = poolName + "-" + poolNumber.getAndIncrement() + "-thread-";
        }

        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}
