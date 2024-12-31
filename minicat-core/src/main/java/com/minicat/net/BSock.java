package com.minicat.net;

import com.minicat.server.processor.Processor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Objects;

class BSock implements Sock<Socket> {
    InetSocketAddress r;
    InetSocketAddress l;
    private long lastProcess;
    private final Socket s;
    private Processor<Socket> p;

    BSock(Socket s) {
        this.r = new InetSocketAddress(s.getInetAddress(), s.getPort());
        this.l = new InetSocketAddress(s.getLocalAddress(), s.getLocalPort());
        this.lastProcess = System.currentTimeMillis();
        this.s = s;
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
    public Socket source() {
        return s;
    }

    @Override
    public void setProcessor(Processor<Socket> p) {
        this.p = p;
    }

    @Override
    public Processor<Socket> processor() {
        return p;
    }

    @Override
    public void close() throws Exception {
        s.close();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BSock bSock = (BSock) o;
        return Objects.equals(s, bSock.s);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(s);
    }
}
