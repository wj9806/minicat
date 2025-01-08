package com.minicat.ws.processor;

import com.minicat.net.Sock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

class WsBioProcessor extends WsProcessor<Socket> {

    WsBioProcessor(Sock<Socket> sock) throws IOException {
        super(sock);
    }

    @Override
    protected OutputStream initOutputStream() throws IOException {
        return sock.source().getOutputStream();
    }

    @Override
    protected InputStream initInputStream() throws IOException {
        return sock.source().getInputStream();
    }

}
