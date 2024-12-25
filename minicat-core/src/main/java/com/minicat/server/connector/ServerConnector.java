package com.minicat.server.connector;

import com.minicat.core.Lifecycle;
import com.minicat.net.Sock;

import java.util.Collection;

/**
 * 服务器连接器接口，定义了连接器的基本行为
 */
public interface ServerConnector<S> extends Lifecycle {

    /**
     * 获取连接器的名称
     * @return 连接器名称
     */
    String getName();

    /**
     * 获取当前正在运行的处理器
     */
    Collection<Sock<S>> getSocks();
}
