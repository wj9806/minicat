package com.minicat.net;

import java.net.InetSocketAddress;
import java.net.Socket;

class BSock implements Sock<Socket> {
    InetSocketAddress r;
    InetSocketAddress l;
    private long lastProcess;
    private final Socket s;

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
    public void close() throws Exception {
        s.close();
    }
}
