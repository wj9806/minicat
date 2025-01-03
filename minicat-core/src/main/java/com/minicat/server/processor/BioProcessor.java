package com.minicat.server.processor;

import com.minicat.core.ApplicationContext;
import com.minicat.http.ApplicationResponse;
import com.minicat.net.Sock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.Socket;

/**
 * BIO模式的请求处理器
 */
public class BioProcessor extends Processor<Socket> {
    private static final Logger logger = LoggerFactory.getLogger(BioProcessor.class);
    private final InputStream his;
    private boolean closed = false;

    public BioProcessor(ApplicationContext applicationContext, Sock<Socket> s) {
        super(applicationContext, s);
        Socket socket = s.source();
        try {
            this.hos = socket.getOutputStream();
            this.his = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Logger logger() {
        return logger;
    }

    @Override
    protected Object allocTmpBuf() {
        return new byte[1024];
    }

    @Override
    protected HttpServletResponse buildResponse() {
        return new ApplicationResponse(hos);
    }

    @Override
    protected int read(Object buf) throws IOException {
        return his.read((byte[])buf);
    }

    @Override
    protected void write(OutputStream os, Object buffer, int off, int len) throws IOException {
        byte[] buf = (byte[]) buffer;
        // 检查边界条件
        if (os == null) {
            throw new NullPointerException("ByteArrayOutputStream cannot be null");
        }
        if (buf == null) {
            throw new NullPointerException("byte array cannot be null");
        }
        if (off < 0 || len < 0 || (off + len) > buf.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length");
        }

        // 直接将指定范围的数据写入ByteArrayOutputStream
        os.write(buf, off, len);
    }

    @Override
    protected void sendNotFoundResponse() throws Exception {
        String notFoundResponse = notFoundResponse();
        hos.write(notFoundResponse.getBytes());
    }

    @Override
    protected void sendErrorResponse(String message) throws Exception {
        String errorResponse = errorResponse(message);
        hos.write(errorResponse.getBytes());
    }

    @Override
    public void destroy() throws Exception {
        if (closed) return;

        closed = true;
        his.close();
        hos.close();
        sock.close();
    }

    @Override
    public void close() throws Exception {

    }
}
