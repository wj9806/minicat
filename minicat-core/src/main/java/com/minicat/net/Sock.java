package com.minicat.net;

import com.minicat.server.processor.Processor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

/**
 * Sock抽象
 */
public interface Sock<S> {

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

    void setProcessor(Processor<S> p);

    Processor<S> processor();

    void close() throws Exception;

    static Sock<Socket> from(Socket s) {
        return new BSock(s);
    }

    static Sock<SelectionKey> from(SelectionKey key) {
        return new NSock(key);
    }
}
