package com.minicat.core;

import javax.servlet.*;
import java.io.IOException;

public class ServletWrapper implements Servlet {

    private final Servlet servlet;
    private final ServletRegistrationImpl registration;

    public ServletWrapper(Servlet servlet, ServletRegistrationImpl registration) {
        this.servlet = servlet;
        this.registration = registration;
    }

    public ServletRegistrationImpl getRegistration() {
        return registration;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        servlet.init(config);
    }

    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
        servlet.service(req, res);
    }

    @Override
    public String getServletInfo() {
        return servlet.getServletInfo();
    }

    @Override
    public void destroy() {
        servlet.destroy();
    }
}
