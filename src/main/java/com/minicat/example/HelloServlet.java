package com.minicat.example;

import com.minicat.server.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

public class HelloServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HelloServlet.class);

    @Override
    public void init() throws ServletException {
        logger.info("HelloServlet init...");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();
        writer.write("<!DOCTYPE html>");
        writer.write("<html>");
        writer.write("<head>");
        writer.write("<title>Hello from MiniCat</title>");
        writer.write("<style>");
        writer.write(".button { display: inline-block; padding: 10px 20px; background-color: #4CAF50; color: white; ");
        writer.write("text-decoration: none; border-radius: 5px; margin-top: 20px; }");
        writer.write(".button:hover { background-color: #45a049; }");
        writer.write("</style>");
        writer.write("</head>");
        writer.write("<body>");
        writer.write("<h1>Hello from MiniCat!</h1>");
        writer.write("<p>This is a response from HelloServlet</p>");
        writer.write("<p>Request URI: " + req.getRequestURI() + "</p>");
        writer.write("<p>Query String: " + req.getQueryString() + "</p>");
        
        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            writer.write("<p>name: " + name + " ; value: " + req.getParameter(name) +" </p>");
        }

        // 添加跳转到首页的按钮
        writer.write("<a href=\"/\" class=\"button\">返回首页</a>");
        
        writer.write("</body>");
        writer.write("</html>");
    }

    @Override
    public void destroy() {
        logger.info("HelloServlet destroy...");
    }
}
