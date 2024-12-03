package com.minicat.example;

import com.minicat.server.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

public class HelloServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HelloServlet.class);

    @Override
    public void init() throws ServletException {
        logger.info("HelloServlet init...");
    }

    @Override
    public void destroy() {
        logger.info("HelloServlet destroy...");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        PrintWriter writer = resp.getWriter();
        writer.write("<h1>Hello from MiniCat!</h1>");
        writer.write("<p>This is a response from HelloServlet</p>");
        writer.write("<p>Request URI: " + req.getRequestURI() + "</p>");
        writer.write("<p>Query String: " + req.getQueryString() + "</p>");
        writer.flush();
    }
}
