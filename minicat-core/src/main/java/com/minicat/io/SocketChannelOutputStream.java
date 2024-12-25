package com.minicat.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelOutputStream extends ByteArrayOutputStream {

    private boolean closed = false;

    private final SocketChannel socketChannel;

    public SocketChannelOutputStream(SocketChannel socketChannel) {
        super();
        this.socketChannel = socketChannel;
    }

    public void flush() throws IOException {
        checkClosed();
        if (socketChannel.isOpen()) {
            ByteBuffer buf = ByteBuffer.wrap(super.toByteArray());
            socketChannel.write(buf);
        }
    }

    public void close() throws IOException {
        if (!closed) {
            closed = true;
        }
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
    }

}