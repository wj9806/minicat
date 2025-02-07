package io.github.wj9806.minicat.core.test;

import io.github.wj9806.minicat.core.test.filter.LoggingFilter;
import io.github.wj9806.minicat.core.test.filter.TestFilter;
import io.github.wj9806.minicat.MiniCat;
import io.github.wj9806.minicat.core.test.servlet.*;
import io.github.wj9806.minicat.server.HttpServer;

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
