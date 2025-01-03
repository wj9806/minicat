package com.minicat.ws.processor;

import com.minicat.io.SocketChannelOutputStream;
import com.minicat.net.Sock;
import com.minicat.ws.WsHttpUpgradeHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/*class WsNioProcessor extends WsProcessor<SelectionKey> {

    WsNioProcessor(Sock<SelectionKey> sock) throws IOException {
        super(sock);
    }

    @Override
    protected OutputStream initOutputStream() {
        return new SocketChannelOutputStream((SocketChannel)sock.source().channel());
    }

    @Override
    public void close() throws Exception {
        WsHttpUpgradeHandler upgradeHandler = sc.getUpgradeHandler(sock);
    }
}*/
