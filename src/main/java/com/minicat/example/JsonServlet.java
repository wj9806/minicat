package com.minicat.example;

import com.minicat.server.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
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
        data.put("details", details);
        
        // 转换为JSON字符串
        String json = toJson(data);
        
        // 发送响应
        PrintWriter writer = resp.getWriter();
        writer.write(json);
        //writer.flush();
    }
    
    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(value).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(toJson((Map<String, Object>) value));
            } else {
                json.append("\"").append(value).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }

    @Override
    public void destroy() {
        logger.info("JsonServlet destroy...");
    }
}
