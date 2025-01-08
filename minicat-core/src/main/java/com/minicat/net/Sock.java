package com.minicat.net;

import com.minicat.ws.processor.WsProcessor;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;

/**
 * Sock抽象
 */
public interface Sock<S> {

    Object sockLock();

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    /**
     * 获取最后一次处理请求时间
     */
    long getLastProcess();

    /**
     * 刷新最后一次处理请求时间
     */
    void freshLastProcess();

    S source();

    void setWsProcessor(WsProcessor<S> p);

    WsProcessor<S> wsProcessor();

    void close() throws Exception;

    static Sock<Socket> from(Socket s) {
        return new BSock(s);
    }

    static Sock<SelectionKey> from(SelectionKey key) {
        return new NSock(key);
    }
}
