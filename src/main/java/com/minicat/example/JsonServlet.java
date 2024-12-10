package com.minicat.example;

import com.minicat.server.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class JsonServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(JsonServlet.class);

    @Override
    public void init() throws ServletException {
        logger.info("JsonServlet init...");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        // 设置响应类型为application/json
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        
        // 创建示例数据
        Map<String, Object> data = new HashMap<>();
        data.put("message", "Hello from JsonServlet,你好世界");
        data.put("status", "success");
        data.put("code", 200);
        
        Map<String, Object> details = new HashMap<>();
        details.put("time", System.currentTimeMillis());
        details.put("version", "1.0.0");
        details.put("pathInfo", req.getPathInfo());
        details.put("servletPath", req.getServletPath());
        data.put("details", details);
        
        // 转换为JSON字符串
        String json = JSON.toJSONString(data);
        
        // 发送响应
        PrintWriter writer = resp.getWriter();
        writer.write(json);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        // 设置响应类型
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        // 读取请求体
        StringBuilder requestBody = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                requestBody.append(line);
            }
        }

        // 创建响应数据
        Map<String, Object> data = new HashMap<>();
        data.put("message", "POST request processed successfully");
        data.put("status", "success");
        data.put("code", 200);

        Map<String, Object> details = new HashMap<>();
        details.put("time", System.currentTimeMillis());
        details.put("contentType", req.getContentType());
        details.put("contentLength", req.getContentLength());
        details.put("requestBody", requestBody.toString());
        data.put("details", details);

        // 转换为JSON字符串并发送响应
        String json = JSON.toJSONString(data);
        PrintWriter writer = resp.getWriter();
        writer.write(json);
    }

    @Override
    public void destroy() {
        logger.info("JsonServlet destroy...");
    }
}
