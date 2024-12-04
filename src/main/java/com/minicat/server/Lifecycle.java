package com.minicat.server;

/**
 * 服务器组件生命周期接口
 */
public interface Lifecycle {
    /**
     * 初始化组件
     */
    void init() throws Exception;

    /**
     * 启动组件
     */
    void start() throws Exception;

    /**
     * 停止组件
     */
    void stop() throws Exception;

    /**
     * 销毁组件，释放资源
     */
    void destroy() throws Exception;
}
