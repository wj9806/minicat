package com.minicat.test;

import com.minicat.MiniCat;
import com.minicat.server.HttpServer;
import com.minicat.test.filter.LoggingFilter;
import com.minicat.test.filter.TestFilter;
import com.minicat.test.servlet.*;

public class TestCoreMain {

    public static void main(String[] args) {
        MiniCat miniCat = new MiniCat();
        HttpServer server = miniCat.getServer();

        server.addServlet(new FormServlet(), "/form");
        server.addServlet(new MultipartServlet(), "/file/upload");
        server.addServlet(new HelloServlet(), "/hello.html");
        server.addServlet(new JsonServlet(), "/json", "/json2", "/json/*");
        server.addServlet(new SseServlet());

        server.addFilter(new LoggingFilter(), "/*");
        server.addFilter(new TestFilter(), "/");
        miniCat.init();
        miniCat.start();
    }
}
