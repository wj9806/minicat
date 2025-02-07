package io.github.wj9806.minicat.server;

import io.github.wj9806.minicat.net.Sock;

public interface IProcessor<S> extends AutoCloseable {

    /**
     * 处理请求
     * @throws Exception 处理过程中可能出现的异常
     * @return 等于0 说明处理正常 等于-1说明处理完成
     */
    int process() throws Exception;

    Sock<S> sock();
}
