package com.minicat.net;

import com.minicat.ws.processor.WsProcessor;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

class BSock implements Sock<Socket> {
    InetSocketAddress r;
    InetSocketAddress l;
    private long lastProcess;
    private final Socket s;
    private WsProcessor<Socket> p;

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
    public void setWsProcessor(WsProcessor<Socket> p) {
        this.p = p;
    }

    @Override
    public WsProcessor<Socket> wsProcessor() {
        return p;
    }

    @Override
    public void close() throws Exception {
        if (p != null) {
            p.close();
        }
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
