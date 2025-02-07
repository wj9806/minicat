package io.github.wj9806.minicat.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.*;

public class SseServer {

    public static void main(String[] args) {
        int port = 8080; // 设置监听端口

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("SSE Server started on port " + port);

            while (true) {
                // 等待客户端连接
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                // 启动一个线程处理客户端
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void handleClient(Socket clientSocket) {
        BufferedReader reader;
        OutputStream outputStream;
        while (true) {
            try {
                reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                outputStream = clientSocket.getOutputStream(); // 使用 OutputStream 替代 Writer

                // 读取请求行（请求路径）
                String requestLine = reader.readLine();
                if (requestLine == null) {
                    return; // 如果没有请求行，直接返回
                }

                // 解析请求行，检查路径是否为 /sse 或 /json
                String[] requestParts = requestLine.split(" ");
                if (requestParts.length < 2) {
                    return; // 如果请求格式不正确，直接返回
                }

                String requestPath = requestParts[1];

                if ("/sse".equals(requestPath)) {
                    // 如果请求路径是 /sse，返回 SSE 数据流
                    handleSse(outputStream, clientSocket);
                } else if ("/json".equals(requestPath)) {
                    // 如果请求路径是 /json，返回 JSON 数据
                    handleJson(outputStream, clientSocket);
                } else {
                    // 对其他路径返回 404 Not Found
                    String response = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "\r\n" +
                            "Not Found\r\n";
                    outputStream.write(response.getBytes("UTF-8"));
                    outputStream.flush();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 处理 /sse 路径
    private static void handleSse(OutputStream outputStream, Socket clientSocket) {
        try {
            // 设置响应头，启用 chunked 传输编码
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Transfer-Encoding: chunked\r\n" +
                    "Content-Type: text/event-stream\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: keep-alive\r\n" +
                    "\r\n";
            outputStream.write(headers.getBytes("UTF-8"));
            outputStream.flush();

            // 模拟持续发送事件
            for (int i = 0; i < 5; i++) {
                String data = "data: 当前时间 " + System.currentTimeMillis() + "\n\n";
                byte[] dataBytes = data.getBytes("UTF-8");

                // 写入块大小（十六进制）和数据
                String chunkSize = Integer.toHexString(dataBytes.length) + "\r\n";
                outputStream.write(chunkSize.getBytes("UTF-8"));
                outputStream.write(dataBytes);
                outputStream.write("\r\n".getBytes("UTF-8"));
                outputStream.flush();

                // 等待 2 秒后发送下一条消息
                Thread.sleep(1000);
            }

            // 发送结束块（大小为 0）
            outputStream.write("0\r\n\r\n".getBytes("UTF-8"));
            outputStream.flush();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 处理 /json 路径
    private static void handleJson(OutputStream outputStream, Socket clientSocket) {
        try {
            // 准备返回的 JSON 数据
            String jsonResponse = "{\n" +
                    "  \"message\": \"Hello, this is a JSON response\",\n" +
                    "  \"status\": \"success\",\n" +
                    "  \"timestamp\": " + System.currentTimeMillis() + "\n" +
                    "}";

            // 设置响应头
            String headers = "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: application/json\r\n" +
                    "Cache-Control: no-cache\r\n" +
                    "Connection: close\r\n" +
                    "\r\n";
            outputStream.write(headers.getBytes("UTF-8"));
            outputStream.flush();

            // 发送 JSON 数据
            outputStream.write(jsonResponse.getBytes("UTF-8"));
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}


