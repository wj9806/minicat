package com.minicat.ws;

import com.minicat.ws.processor.WsProcessor;

import javax.websocket.EncodeException;
import javax.websocket.RemoteEndpoint;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class BasicRemoteEndpoint implements RemoteEndpoint.Basic {

    private final WsProcessor<?> processor;

    public BasicRemoteEndpoint(WsProcessor<?> processor) {
        this.processor = processor;
    }

    @Override
    public void sendText(String text) throws IOException {
        ByteBuffer buffer = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
        sendBinary(buffer);
    }

    @Override
    public void sendBinary(ByteBuffer data) throws IOException {
        int capacity = data.capacity();

        // 创建帧头
        byte firstByte = (byte) (0x80 | 0x01); // FIN=1, RSV1-3=0, Opcode=1 (text)

        int headerLength;

        // 设置负载长度
        if (capacity < 126) {
            headerLength = 2;
        } else if (capacity <= 65535) {
            headerLength = 4;
        } else {
            headerLength = 10;
        }

        // 分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(headerLength + capacity);

        // 写入帧头
        buffer.put(firstByte);
        if (capacity < 126) {
            buffer.put((byte) capacity); // Masked bit = 0
        } else if (capacity <= 65535) {
            buffer.put((byte) 126); // Extended payload length
            buffer.putShort((short) capacity);
        } else {
            buffer.put((byte) 127); // Extended payload length
            for (int i = 7; i >= 0; i--) {
                buffer.put((byte) ((capacity >> (8 * i)) & 0xFF));
            }
        }

        // 写入负载数据
        buffer.put(data);

        buffer.flip();
        // 发送数据
        processor.send(buffer);
        processor.flush();
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
