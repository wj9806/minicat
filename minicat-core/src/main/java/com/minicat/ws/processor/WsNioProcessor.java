package com.minicat.ws.processor;

import com.minicat.io.SocketChannelInputStream;
import com.minicat.io.SocketChannelOutputStream;
import com.minicat.net.Sock;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

class WsNioProcessor extends WsProcessor<SelectionKey> {

    WsNioProcessor(Sock<SelectionKey> sock) throws IOException {
        super(sock);
    }

    @Override
    protected OutputStream initOutputStream() {
        return new SocketChannelOutputStream((SocketChannel)sock.source().channel());
    }

    @Override
    protected InputStream initInputStream() throws IOException {
        return new SocketChannelInputStream((SocketChannel)sock.source().channel(), 4);
    }
}
