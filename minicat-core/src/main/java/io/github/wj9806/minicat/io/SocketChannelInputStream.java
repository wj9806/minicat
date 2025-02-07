package io.github.wj9806.minicat.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class SocketChannelInputStream extends InputStream {

    private final SocketChannel socketChannel;

    private final ByteBuffer buffer;

    public SocketChannelInputStream(SocketChannel socketChannel, int bufferSize) {
        if (socketChannel == null || bufferSize <= 0) {
            throw new IllegalArgumentException("SocketChannel cannot be null and bufferSize must be positive");
        }
        this.socketChannel = socketChannel;
        this.buffer = ByteBuffer.allocate(bufferSize);
        this.buffer.flip(); // Start with an empty buffer
    }

    @Override
    public int read() throws IOException {
        if (!buffer.hasRemaining()) {
            if (!fillBuffer()) {
                return -1; // End of stream
            }
        }
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (b == null) {
            throw new NullPointerException("Buffer cannot be null");
        }
        if (off < 0 || len < 0 || off + len > b.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }
        if (len == 0) {
            return 0;
        }

        int bytesRead = 0;
        while (len > 0) {
            if (!buffer.hasRemaining()) {
                if (!fillBuffer()) {
                    return bytesRead == 0 ? -1 : bytesRead; // Return -1 if no data read
                }
            }
            int toRead = Math.min(len, buffer.remaining());
            buffer.get(b, off, toRead);
            off += toRead;
            len -= toRead;
            bytesRead += toRead;
        }
        return bytesRead;
    }

    private boolean fillBuffer() throws IOException {
        buffer.clear();
        int bytesRead = socketChannel.read(buffer);
        if (bytesRead == -1) {
            return false; // End of stream
        }
        buffer.flip();
        return true;
    }

    @Override
    public void close() throws IOException {
        socketChannel.close();
    }
}
