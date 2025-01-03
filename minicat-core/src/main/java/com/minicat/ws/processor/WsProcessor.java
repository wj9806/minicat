package com.minicat.ws.processor;

import com.minicat.net.Sock;
import com.minicat.server.IProcessor;
import com.minicat.ws.WsConstants;
import com.minicat.ws.WsHttpUpgradeHandler;
import com.minicat.ws.WsServerContainer;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.WebConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class WsProcessor<S> implements WebConnection, IProcessor<S> {

    protected Sock<S> sock;

    private InputStream is;
    protected OutputStream os;

    protected WsServerContainer sc;

    public WsProcessor(Sock<S> sock) throws IOException {
        this.sock = sock;
        this.os = initOutputStream();
        this.is = initInputStream();
        sock.setWsProcessor(this);
    }

    public void setServerContainer(WsServerContainer sc) {
        this.sc = sc;
    }

    @Override
    public int process() throws Exception {
        int firstByte = read();
        if (firstByte == -1) return firstByte;

        int opcode = firstByte & 0x0F;
        int secondByte = read();
        boolean masked = (secondByte & 0x80) != 0;
        int payloadLength = secondByte & 0x7F;

        // 处理扩展长度
        if (payloadLength == 126) {
            payloadLength = ((read() << 8) | read());
        } else if (payloadLength == 127) {
            // 暂不处理超大帧
            return 0;
        }

        // 读取掩码键
        byte[] maskKey = new byte[4];
        if (masked) {
            read(maskKey);
        }

        // 读取负载数据
        byte[] payload = new byte[payloadLength];
        read(payload);

        // 解码数据
        if (masked) {
            for (int i = 0; i < payload.length; i++) {
                payload[i] = (byte) (payload[i] ^ maskKey[i % 4]);
            }
        }

        // 处理不同类型的帧
        switch (opcode) {
            case WsConstants.OPCODE_TEXT: // 文本帧
                System.out.println("OPCODE_TEXT");
                onMessage(payload);
                break;
            case WsConstants.OPCODE_CLOSE: // 关闭帧
                System.out.println("OPCODE_CLOSE");
                return -1;
            case WsConstants.OPCODE_PING: // Ping
                System.out.println("OPCODE_PING");
                break;
        }
        return 0;
    }

    private void onMessage(byte[] payload) {
        WsHttpUpgradeHandler upgradeHandler = sc.getUpgradeHandler(sock);
        upgradeHandler.onMessage(payload);
    }

    protected abstract OutputStream initOutputStream() throws IOException;

    protected abstract InputStream initInputStream() throws IOException;

    public Sock<S> sock() {
        return sock;
    }

    public void send(ByteBuffer buf) throws IOException {
        os.write(buf.array());
    }

    public void flush() throws IOException {
        os.flush();
    }

    public int read() throws IOException {
        return is.read();
    }

    public int read(byte[] buf) throws IOException {
        return is.read(buf);
    }

    public ServletInputStream getInputStream() throws IOException {
        return null;
    }

    public ServletOutputStream getOutputStream() throws IOException {
        return null;
    }

    public static WsProcessor<?> createWebConnection(Sock<?> s) throws IOException {
        Object source = s.source();
        if (source instanceof Socket) {
            return new WsBioProcessor((Sock<Socket>) s);
        } else if (source instanceof SelectionKey) {
            //return new WsNioProcessor((Sock<SelectionKey>) s);
        }
        return null;
    }


}
