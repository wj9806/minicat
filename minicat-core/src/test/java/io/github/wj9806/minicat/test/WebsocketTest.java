package io.github.wj9806.minicat.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class WebsocketTest {

    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    public static void main(String[] args) {
        startWebSocketServer();
    }

    public static void startWebSocketServer() {
        try (ServerSocket server = new ServerSocket(8080)) {
            System.out.println("WebSocket服务器启动，监听端口：8080");

            while (true) {
                Socket client = server.accept();
                new Thread(new WebSocketHandler(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class WebSocketHandler implements Runnable {
        private final Socket client;
        private InputStream in;
        private OutputStream out;

        public WebSocketHandler(Socket client) {
            this.client = client;
            try {
                this.in = client.getInputStream();
                this.out = client.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                // 处理WebSocket握手
                if (handleHandshake()) {
                    // 处理WebSocket数据帧
                    handleWebSocketFrame();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean handleHandshake() throws IOException {
            // 读取HTTP请求头
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            Map<String, String> headers = new HashMap<>();

            // 读取第一行
            String firstLine = reader.readLine();
            if (firstLine == null || !firstLine.contains("GET")) {
                return false;
            }

            // 读取所有请求头
            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                String[] parts = line.split(": ", 2);
                if (parts.length == 2) {
                    headers.put(parts[0], parts[1]);
                }
            }

            // 验证是否为WebSocket升级请求
            if (!"websocket".equalsIgnoreCase(headers.get("Upgrade")) ||
                    !headers.containsKey("Sec-WebSocket-Key")) {
                return false;
            }

            // 生成WebSocket接受键
            String acceptKey = generateAcceptKey(headers.get("Sec-WebSocket-Key"));

            // 发送握手响应
            String response = "HTTP/1.1 101 Switching Protocols\r\n" +
                    "Upgrade: websocket\r\n" +
                    "Connection: Upgrade\r\n" +
                    "Sec-WebSocket-Accept: " + acceptKey + "\r\n\r\n";

            out.write(response.getBytes());
            out.flush();
            return true;
        }

        private String generateAcceptKey(String key) {
            try {
                String concat = key + WEBSOCKET_MAGIC_STRING;
                MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
                byte[] hash = sha1.digest(concat.getBytes());
                return Base64.getEncoder().encodeToString(hash);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        }

        private void handleWebSocketFrame() throws IOException {
            while (true) {
                // 读取帧头的第一个字节
                int firstByte = in.read();
                if (firstByte == -1) return;

                // 解析FIN和Opcode
                boolean fin = (firstByte & 0x80) != 0;
                int opcode = firstByte & 0x0F;

                // 读取帧头的第二个字节
                int secondByte = in.read();
                boolean masked = (secondByte & 0x80) != 0;
                int payloadLength = secondByte & 0x7F;

                // 处理扩展长度
                if (payloadLength == 126) {
                    payloadLength = ((in.read() << 8) | in.read());
                } else if (payloadLength == 127) {
                    // 暂不处理超大帧
                    continue;
                }

                // 读取掩码键
                byte[] maskKey = new byte[4];
                if (masked) {
                    in.read(maskKey);
                }

                // 读取负载数据
                byte[] payload = new byte[payloadLength];
                in.read(payload);

                // 解码数据
                if (masked) {
                    for (int i = 0; i < payload.length; i++) {
                        payload[i] = (byte) (payload[i] ^ maskKey[i % 4]);
                    }
                }

                // 处理不同类型的帧
                switch (opcode) {
                    case 0x1: // 文本帧
                        String message = new String(payload);
                        System.out.println("收到消息: " + message);
                        // 发送回显消息
                        sendMessage("服务器收到: " + message);
                        break;


                    case 0x8: // 关闭帧
                        System.out.println("收到关闭帧");

                        int closeCode = 1000; // 默认关闭状态码
                        String reason = "";

                        if (payloadLength >= 2) {
                            closeCode = (payload[0] << 8) | (payload[1] & 0xFF);
                            if (payloadLength > 2) {
                                reason = new String(payload, 2, payloadLength - 2);
                            }
                        }

                        System.out.println("关闭原因: 状态码=" + closeCode + ", 原因=" + reason);

                        sendCloseFrame(closeCode, reason);
                        return;

                    case 0x9: // Ping
                        sendPong();
                        break;
                }
            }
        }

        private void sendMessage(String message) throws IOException {
            byte[] payload = message.getBytes();

            // 创建帧头
            int firstByte = 0x81; // FIN=1, Opcode=1 (text)
            out.write(firstByte);

            // 设置负载长度
            if (payload.length < 126) {
                out.write(payload.length);
            } else if (payload.length < 65536) {
                out.write(126);
                out.write(payload.length >> 8);
                out.write(payload.length & 0xFF);
            } else {
                out.write(127);
                for (int i = 0; i < 8; i++) {
                    out.write((payload.length >> (8 * (7 - i))) & 0xFF);
                }
            }

            // 发送负载数据
            out.write(payload);
            out.flush();
        }

        private void sendPong() throws IOException {
            out.write(0x8A); // FIN=1, Opcode=0xA (Pong)
            out.write(0x00); // 空负载
            out.flush();
        }

        private void sendCloseFrame(int closeCode, String reason) throws IOException {
            ByteArrayOutputStream frame = new ByteArrayOutputStream();

            frame.write(0x88);

            byte[] reasonBytes = reason != null ? reason.getBytes() : new byte[0];
            int payloadLength = 2 + reasonBytes.length;

            if (payloadLength < 126) {
                frame.write(payloadLength);
            } else if (payloadLength < 65536) {
                frame.write(126);
                frame.write(payloadLength >> 8);
                frame.write(payloadLength & 0xFF);
            }

            frame.write(closeCode >> 8);
            frame.write(closeCode & 0xFF);

            frame.write(reasonBytes);

            out.write(frame.toByteArray());
            out.flush();

            System.out.println("发送关闭帧: 状态码=" + closeCode + ", 原因=" + reason);
        }
    }

}
