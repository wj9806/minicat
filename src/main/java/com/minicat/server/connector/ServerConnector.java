package com.minicat.server.connector;

import com.minicat.server.Lifecycle;

/**
 * 服务器连接器接口，定义了连接器的基本行为
 */
public interface ServerConnector extends Lifecycle {
    /**
     * 获取连接器的名称
     * @return 连接器名称
     */
    String getName();
}
