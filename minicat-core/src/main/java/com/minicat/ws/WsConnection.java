package com.minicat.ws;

import com.minicat.net.Sock;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.WebConnection;
import java.io.IOException;
import java.net.Socket;
import java.nio.channels.SelectionKey;

public class WsConnection implements WebConnection {

    private Socket socket;

    private SelectionKey key;

    public WsConnection(Socket socket) {
        this.socket = socket;
    }

    public WsConnection(SelectionKey key) {
        this.key = key;
    }

    public static WebConnection createConnection(Sock<?> s) {
        Object source = s.source();
        if (source instanceof Socket) {
            return new WsConnection((Socket) source);
        } else {
            return new WsConnection((SelectionKey) source);
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    @Override
    public void close() throws Exception {

    }
}
