package com.minicat.test.servlet;


import com.minicat.test.util.JSON;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FormServlet extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws IOException {
        // 获取表单参数
        req.setCharacterEncoding(StandardCharsets.UTF_8.name());
        String username = req.getParameter("username");
        String email = req.getParameter("email");

        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Form submitted successfully");
        response.put("data", new HashMap<String, String>() {{
            put("username", username);
            put("email", email);
        }});

        // 设置响应类型
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        String json = JSON.toJSONString(response);
        PrintWriter writer = resp.getWriter();
        writer.write(json);
    }
}
