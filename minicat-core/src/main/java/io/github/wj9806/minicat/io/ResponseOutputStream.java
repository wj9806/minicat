package io.github.wj9806.minicat.io;

import io.github.wj9806.minicat.http.ApplicationResponse;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * 自定义ServletOutputStream实现
 */
public class ResponseOutputStream extends ServletOutputStream {
    private ByteBuffer byteBuffer;
    private final OutputStream outputStream;
    private WriteListener writeListener;  // 用于异步 I/O
    private final Charset charset;
    private final ApplicationResponse response;

    // 构造方法，传入 ByteBuffer、OutputStream 和 Charset
    public ResponseOutputStream(ApplicationResponse response) {
        this.byteBuffer = response.getBodyBuffer();
        this.outputStream = response.getSocketStream();
        this.charset = response.getCharset();  // 默认为 UTF-8
        this.response = response;
    }

    @Override
    public void write(int b) throws IOException {
        // 确保 ByteBuffer 有足够的空间
        if (byteBuffer.remaining() < 1) {
            byteBuffer = NioUtil.expandBuffer(byteBuffer, 1);
            // 更新ApplicationResponse中的bodyBuffer引用
            response.setBodyBuffer(byteBuffer);
        }
        // 写入 ByteBuffer
        byteBuffer.put((byte) b);

        // 如果有 WriteListener，通知它
        if (writeListener != null && byteBuffer.hasRemaining()) {
            writeListener.onWritePossible();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        // 确保 ByteBuffer 有足够的空间
        if (byteBuffer.remaining() < len) {
            byteBuffer = NioUtil.expandBuffer(byteBuffer, len - byteBuffer.remaining());
            // 更新ApplicationResponse中的bodyBuffer引用
            response.setBodyBuffer(byteBuffer);
        }

        // 批量将数据写入 ByteBuffer
        byteBuffer.put(b, off, len);

        // 如果有 WriteListener，通知它
        if (writeListener != null && byteBuffer.hasRemaining()) {
            writeListener.onWritePossible();
        }
    }

    @Override
    public void flush() throws IOException {
        // 只有在响应未提交时才发送头，避免重复设置Content-Length
        if (!response.isCommitted()) {
            response.sendHeader();
        }

        // 将 ByteBuffer 中的数据批量刷新到 OutputStream
        byteBuffer.flip(); // 切换为读取模式
        outputStream.write(byteBuffer.array(), byteBuffer.position(), byteBuffer.remaining());
        byteBuffer.clear(); // 清空 ByteBuffer 数据
        outputStream.flush(); // 刷新 OutputStream
    }

    @Override
    public void close() throws IOException {
        outputStream.close();
    }

    // 判断是否准备好写入
    @Override
    public boolean isReady() {
        // 这里返回 ByteBuffer 是否有剩余空间，如果有空间则可以继续写入
        return byteBuffer.remaining() > 0;
    }

    // 设置 WriteListener，当输出流准备好时会调用监听器
    @Override
    public void setWriteListener(WriteListener writeListener) {
        this.writeListener = writeListener;
    }
}
