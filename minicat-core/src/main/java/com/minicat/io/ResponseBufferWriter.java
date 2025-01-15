package com.minicat.io;

import com.minicat.http.ApplicationResponse;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ResponseBufferWriter extends Writer {

    private ByteBuffer byteBuffer;
    private final OutputStream outputStream;
    private final Charset charset;
    private final Writer byteWriter;
    private final ApplicationResponse response;

    // 构造方法，传入 ByteBuffer, OutputStream 和 Charset
    public ResponseBufferWriter(ApplicationResponse response) {
        this.byteBuffer = response.getBodyBuffer();
        this.outputStream = response.getSocketStream();
        this.charset = response.getCharset();  // 默认为 UTF-8
        this.byteWriter = new OutputStreamWriter(outputStream, this.charset); // 使用指定字符集的 OutputStreamWriter
        this.response = response;
    }

    @Override
    public void write(int c) throws IOException {
        // 将字符转换为字节并写入 ByteBuffer
        if (byteBuffer.remaining() < 1) {
            byteBuffer = NioUtil.expandBuffer(byteBuffer, 1);
        }
        byteBuffer.put((byte) c);
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        // 检查 ByteBuffer 是否有足够空间
        if (byteBuffer.remaining() < len) {
            byteBuffer = NioUtil.expandBuffer(byteBuffer, len - byteBuffer.remaining());
        }

        // 使用指定字符集将字符数组转换为字节并写入 ByteBuffer
        byte[] encoded = new String(cbuf, off, len).getBytes(charset);
        if (byteBuffer.remaining() < encoded.length) {
            throw new IOException("Not enough space in ByteBuffer");
        }
        byteBuffer.put(encoded);
    }

    @Override
    public void flush() throws IOException {
        response.sendHeader();

        // 将 ByteBuffer 中的数据刷新到 OutputStream
        byteBuffer.flip();  // 切换为读取模式
        while (byteBuffer.hasRemaining()) {
            outputStream.write(byteBuffer.get());
        }
        byteBuffer.clear();  // 清空 ByteBuffer 数据
        outputStream.flush();  // 刷新 OutputStream
    }

    @Override
    public void close() throws IOException {
        // 关闭 OutputStream
        byteWriter.close();
    }
}

