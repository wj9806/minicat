package com.minicat.io;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * 自定义ServletOutputStream实现
 * 将数据写入ByteArrayOutputStream而不是直接写入底层socket
 */
public class ResponseOutputStream extends ServletOutputStream {
    private final ByteArrayOutputStream buffer;
    private boolean closed = false;

    public ResponseOutputStream(ByteArrayOutputStream buffer) {
        this.buffer = buffer;
    }

    @Override
    public void write(int b) throws IOException {
        checkClosed();
        buffer.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        checkClosed();
        buffer.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        checkClosed();
        buffer.write(b, off, len);
    }

    @Override
    public void flush() throws IOException {
        checkClosed();
        buffer.flush();
    }

    @Override
    public void close() throws IOException {
        if (!closed) {
            flush();
            closed = true;
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void setWriteListener(WriteListener writeListener) {
        throw new UnsupportedOperationException("Write listeners are not supported");
    }

    private void checkClosed() throws IOException {
        if (closed) {
            throw new IOException("Stream has been closed");
        }
    }
}
