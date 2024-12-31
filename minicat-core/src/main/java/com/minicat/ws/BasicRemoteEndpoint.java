package com.minicat.ws;

import com.minicat.server.processor.Processor;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class BasicRemoteEndpoint implements RemoteEndpoint.Basic {

    private final Processor<?> processor;

    public BasicRemoteEndpoint(Processor<?> processor) {
        this.processor = processor;
    }

    @Override
    public void sendText(String text) throws IOException {
        byte[] payload = text.getBytes(StandardCharsets.UTF_8);

        // 创建帧头
        byte firstByte = (byte) (0x80 | 0x01); // FIN=1, RSV1-3=0, Opcode=1 (text)

        int headerLength;

        // 设置负载长度
        if (payload.length < 126) {
            headerLength = 2;
        } else if (payload.length <= 65535) {
            headerLength = 4;
        } else {
            headerLength = 10;
        }

        // 分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(headerLength + payload.length);

        // 写入帧头
        buffer.put(firstByte);
        if (payload.length < 126) {
            buffer.put((byte) payload.length); // Masked bit = 0
        } else if (payload.length <= 65535) {
            buffer.put((byte) 126); // Extended payload length
            buffer.putShort((short) payload.length);
        } else {
            buffer.put((byte) 127); // Extended payload length
            for (int i = 7; i >= 0; i--) {
                buffer.put((byte) ((payload.length >> (8 * i)) & 0xFF));
            }
        }

        // 写入负载数据
        buffer.put(payload);

        buffer.flip();
        // 发送数据
        processor.send(buffer); // 假设 processor 支持 ByteBuffer 类型
        processor.flush(); // 如果需要，刷新缓冲区
        System.out.println("Frame content: " + Arrays.toString(buffer.array()));
    }

    @Override
    public void sendBinary(ByteBuffer data) throws IOException {

    }

    @Override
    public void sendText(String partialMessage, boolean isLast) throws IOException {

    }

    @Override
    public void sendBinary(ByteBuffer partialByte, boolean isLast) throws IOException {

    }

    @Override
    public OutputStream getSendStream() throws IOException {
        return null;
    }

    @Override
    public Writer getSendWriter() throws IOException {
        return null;
    }

    @Override
    public void sendObject(Object data) throws IOException, EncodeException {

    }

    @Override
    public void setBatchingAllowed(boolean allowed) throws IOException {

    }

    @Override
    public boolean getBatchingAllowed() {
        return false;
    }

    @Override
    public void flushBatch() throws IOException {

    }

    @Override
    public void sendPing(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }

    @Override
    public void sendPong(ByteBuffer applicationData) throws IOException, IllegalArgumentException {

    }
}
