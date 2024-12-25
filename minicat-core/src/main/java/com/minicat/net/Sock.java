package com.minicat.net;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.channels.SelectionKey;

/**
 * Sock抽象
 */
public interface Sock<S> {

    InetSocketAddress getRemoteAddress();

    InetSocketAddress getLocalAddress();

    long getLastProcess();

    void freshLastProcess();

    S source();

    void close() throws Exception;

    static Sock<Socket> from(Socket s) {
        return new BSock(s);
    }

    static Sock<SelectionKey> from(SelectionKey key) {
        return new NSock(key);
    }
}
