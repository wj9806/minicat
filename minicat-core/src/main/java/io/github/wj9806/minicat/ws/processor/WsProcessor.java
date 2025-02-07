package io.github.wj9806.minicat.ws.processor;

import io.github.wj9806.minicat.net.Sock;
import io.github.wj9806.minicat.server.IProcessor;
import io.github.wj9806.minicat.ws.WsConstants;
import io.github.wj9806.minicat.ws.WsHttpUpgradeHandler;
import io.github.wj9806.minicat.ws.WsServerContainer;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.WebConnection;
import javax.websocket.CloseReason;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;

public abstract class WsProcessor<S> implements WebConnection, IProcessor<S> {

    protected Sock<S> sock;

    protected InputStream is;
    protected OutputStream os;

    protected WsServerContainer sc;

    private volatile boolean closed = false;

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
        if (firstByte == -1)
            return firstByte;

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
                sock.freshLastProcess();
                onMessage(payload);
                break;
            case WsConstants.OPCODE_CLOSE: // 关闭帧
                CloseReason closeReason = getCloseCode(payloadLength, payload);
                onClose(closeReason);
                return -1;
            case WsConstants.OPCODE_PING: // Ping
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + opcode);
        }
        return 0;
    }

    private static CloseReason getCloseCode(int payloadLength, byte[] payload) {
        String reason = "";
        CloseReason.CloseCode code = CloseReason.CloseCodes.NORMAL_CLOSURE;
        if (payloadLength >= 2) {
            int closeCode = (payload[0] << 8) | (payload[1] & 0xFF);
            code = CloseReason.CloseCodes.getCloseCode(closeCode);

            if (payloadLength > 2) {
                reason = new String(payload, 2, payloadLength - 2);
            }
        }
        return new CloseReason(code, reason);
    }

    private void onMessage(byte[] payload) {
        WsHttpUpgradeHandler upgradeHandler = sc.getUpgradeHandler(sock);
        upgradeHandler.onMessage(payload);
    }

    private void onClose(CloseReason closeReason) throws IOException {
        if (!closed) {
            this.closed = true;
            WsHttpUpgradeHandler upgradeHandler = sc.getUpgradeHandler(sock);
            upgradeHandler.onClose(closeReason);
        }
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

    @SuppressWarnings("unchecked")
    public static WsProcessor<?> createWebConnection(Sock<?> s) throws IOException {
        Object source = s.source();
        if (source instanceof Socket) {
            return new WsBioProcessor((Sock<Socket>) s);
        } else if (source instanceof SelectionKey) {
            return new WsNioProcessor((Sock<SelectionKey>) s);
        }
        return null;
    }

    @Override
    public void close() throws Exception {
        onClose(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
    }

    public boolean idleTimeout() {
        WsHttpUpgradeHandler upgradeHandler = sc.getUpgradeHandler(sock);
        return upgradeHandler.idleTimeout();
    }
}
