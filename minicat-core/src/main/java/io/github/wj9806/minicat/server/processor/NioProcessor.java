package io.github.wj9806.minicat.server.processor;

import io.github.wj9806.minicat.core.ApplicationContext;
import io.github.wj9806.minicat.http.ApplicationResponse;
import io.github.wj9806.minicat.io.SocketChannelOutputStream;
import io.github.wj9806.minicat.net.Sock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

public class NioProcessor extends Processor<SelectionKey> implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(NioProcessor.class);

    public final SelectionKey key;

    public final SocketChannel socketChannel;

    public NioProcessor(ApplicationContext applicationContext, Sock<SelectionKey> s) {
        super(applicationContext, s);
        this.key = s.source();
        this.socketChannel = (SocketChannel) key.channel();
        this.hos = new SocketChannelOutputStream(socketChannel);
    }

    @Override
    protected HttpServletResponse buildResponse(ApplicationContext applicationContext) {
        return new ApplicationResponse(applicationContext, hos);
    }

    @Override
    protected int read(Object buf) throws IOException {
        ByteBuffer buffer = (ByteBuffer)buf;
        buffer.clear();
        return socketChannel.read(buffer);
    }

    @Override
    protected void sendNotFoundResponse() throws Exception {
        String notFoundResponse = notFoundResponse();
        hos.write(notFoundResponse.getBytes());
        hos.flush();
    }

    @Override
    protected void sendErrorResponse(String message) throws Exception {
        String errorResponse = errorResponse(message);
        hos.write(errorResponse.getBytes());
        hos.flush();
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    protected Object allocTmpBuf() {
        return ByteBuffer.allocate(BUFFER_SIZE);
    }

    @Override
    protected void write(OutputStream os, Object buffer, int off, int len) throws IOException {
        ByteBuffer buf = (ByteBuffer) buffer;
        buf.flip();
        // 检查边界条件
        if (off < 0 || len < 0 || (off + len) > buf.limit()) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }

        byte[] tempBuffer = new byte[Math.min(BUFFER_SIZE, len)]; // 使用一个较小的临时缓冲区
        buf.position(off);
        int bytesCopied = 0;

        while (bytesCopied < len) {
            int bytesToRead = Math.min(tempBuffer.length, len - bytesCopied);
            buf.get(tempBuffer, 0, bytesToRead);
            os.write(tempBuffer, 0, bytesToRead);
            bytesCopied += bytesToRead;
        }
    }

    public void close() throws IOException {
        hos.close();
    }

    @Override
    public void destroy() throws Exception {
        close();
        sock.close();
    }
}
