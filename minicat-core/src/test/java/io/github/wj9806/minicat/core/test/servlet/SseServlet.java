package io.github.wj9806.minicat.core.test.servlet;

import io.github.wj9806.minicat.server.HttpServlet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;

// 自定义 Servlet 类
@WebServlet(name = "SseServlet", urlPatterns = "/sse")
public class SseServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // 设置响应头，告知客户端这是 SSE 响应
        resp.setContentType("text/event-stream");
        resp.setHeader("Transfer-Encoding", "chunked");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache");
        resp.setHeader("Connection", "keep-alive");

        try {
            OutputStream outputStream = resp.getOutputStream();
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

                Thread.sleep(1000);
            }

            // 发送结束块（大小为 0）
            outputStream.write("0\r\n\r\n".getBytes("UTF-8"));
            outputStream.flush();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}