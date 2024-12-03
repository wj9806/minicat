package com.minicat.server.thread;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Minicat自定义任务队列
 * 执行策略：
 * 1. 如果线程数小于最大线程数，优先创建新线程
 * 2. 如果线程数达到最大线程数，才将任务放入队列
 */
public class MinicatTaskQueue extends LinkedBlockingQueue<Runnable> {
    private static final long serialVersionUID = -1L;
    private transient ThreadPoolExecutor executor;

    public MinicatTaskQueue(int capacity) {
        super(capacity);
    }

    public void setExecutor(ThreadPoolExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean offer(Runnable runnable) {
        if (executor == null) {
            throw new RejectedExecutionException("The task queue is not initialized with an executor");
        }
        
        int currentPoolSize = executor.getPoolSize();
        
        // 如果有空闲线程，直接加入队列让空闲线程执行
        if (executor.getActiveCount() < currentPoolSize) {
            return super.offer(runnable);
        }
        
        // 如果当前线程数小于最大线程数，返回false让executor创建新线程
        if (currentPoolSize < executor.getMaximumPoolSize()) {
            return false;
        }
        
        // 如果当前线程数达到最大线程数，则加入队列
        return super.offer(runnable);
    }
}
