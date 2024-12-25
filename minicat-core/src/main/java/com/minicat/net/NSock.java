package com.minicat.net;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;

class NSock implements Sock<SelectionKey> {

    InetSocketAddress r;
    InetSocketAddress l;
    private long lastProcess;
    private final SocketChannel sc;
    private final SelectionKey key;

    NSock(SelectionKey key) {
        this.sc = (SocketChannel) key.channel();
        this.key = key;
        try {
            this.r = (InetSocketAddress) sc.getRemoteAddress();
            this.l = (InetSocketAddress) sc.getLocalAddress();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        this.lastProcess = System.currentTimeMillis();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return r;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return l;
    }

    @Override
    public long getLastProcess() {
        return lastProcess;
    }

    @Override
    public void freshLastProcess() {
        lastProcess = System.currentTimeMillis();
    }

    @Override
    public SelectionKey source() {
        return key;
    }

    @Override
    public void close() throws Exception {
        if (sc.isOpen()) {
            key.cancel();
            sc.close();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NSock nSock = (NSock) o;
        return Objects.equals(sc, nSock.sc) && Objects.equals(key, nSock.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sc, key);
    }
}
