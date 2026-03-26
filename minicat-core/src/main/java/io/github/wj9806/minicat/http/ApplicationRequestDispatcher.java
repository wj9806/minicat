package io.github.wj9806.minicat.http;

import io.github.wj9806.minicat.core.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ApplicationRequestDispatcher implements RequestDispatcher {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationRequestDispatcher.class);

    private final String path;
    private final ApplicationContext servletContext;

    public ApplicationRequestDispatcher(ApplicationContext servletContext, String path) {
        this.servletContext = servletContext;
        this.path = path;
    }

    @Override
    public void forward(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        if (response.isCommitted()) {
            throw new IllegalStateException("Response already committed");
        }

        // 重置响应
        if (response instanceof ApplicationResponse) {
            ((ApplicationResponse) response).resetBuffer();
        }

        // 处理路径
        String forwardedPath = path;
        if (forwardedPath.startsWith("/")) {
            // 绝对路径，直接使用
        } else {
            // 相对路径，需要根据当前请求的路径进行处理
            String currentUri = ((ApplicationRequest) request).getRequestURI();
            if (currentUri.endsWith("/")) {
                forwardedPath = currentUri + forwardedPath;
            } else {
                forwardedPath = currentUri.substring(0, currentUri.lastIndexOf("/") + 1) + forwardedPath;
            }
        }

        // 设置转发属性
        request.setAttribute(ApplicationRequest.FORWARD_REQUEST_URI, ((ApplicationRequest) request).getRequestURI());
        request.setAttribute(ApplicationRequest.FORWARD_SERVLET_PATH, ((ApplicationRequest) request).getServletPath());
        request.setAttribute(ApplicationRequest.FORWARD_PATH_INFO, ((ApplicationRequest) request).getPathInfo());
        request.setAttribute(ApplicationRequest.FORWARD_QUERY_STRING, ((ApplicationRequest) request).getQueryString());

        // 更新请求URI
        ((ApplicationRequest) request).setRequestURI(forwardedPath);

        // 查找匹配的Servlet
        Servlet servlet = servletContext.findMatchingServlet((HttpServletRequest) request);
        if (servlet == null) {
            logger.error("No servlet found for path: {}", forwardedPath);
            throw new ServletException("No servlet found for path: " + forwardedPath);
        }

        // 执行Servlet
        FilterChain filterChain = servletContext.buildFilterChain((HttpServletRequest) request, servlet);
        filterChain.doFilter(request, response);
    }

    @Override
    public void include(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        // 处理路径
        String includedPath = path;
        if (includedPath.startsWith("/")) {
            // 绝对路径，直接使用
        } else {
            // 相对路径，需要根据当前请求的路径进行处理
            String currentUri = ((ApplicationRequest) request).getRequestURI();
            if (currentUri.endsWith("/")) {
                includedPath = currentUri + includedPath;
            } else {
                includedPath = currentUri.substring(0, currentUri.lastIndexOf("/") + 1) + includedPath;
            }
        }

        // 设置包含属性
        request.setAttribute(ApplicationRequest.INCLUDE_REQUEST_URI, ((ApplicationRequest) request).getRequestURI());
        request.setAttribute(ApplicationRequest.INCLUDE_SERVLET_PATH, ((ApplicationRequest) request).getServletPath());
        request.setAttribute(ApplicationRequest.INCLUDE_PATH_INFO, ((ApplicationRequest) request).getPathInfo());
        request.setAttribute(ApplicationRequest.INCLUDE_QUERY_STRING, ((ApplicationRequest) request).getQueryString());

        // 保存原始URI
        String originalUri = ((ApplicationRequest) request).getRequestURI();
        String originalServletPath = ((ApplicationRequest) request).getServletPath();
        String originalPathInfo = ((ApplicationRequest) request).getPathInfo();

        try {
            // 更新请求URI
            ((ApplicationRequest) request).setRequestURI(includedPath);

            // 查找匹配的Servlet
            Servlet servlet = servletContext.findMatchingServlet((HttpServletRequest) request);
            if (servlet == null) {
                logger.error("No servlet found for path: {}", includedPath);
                throw new ServletException("No servlet found for path: " + includedPath);
            }

            // 执行Servlet
            FilterChain filterChain = servletContext.buildFilterChain((HttpServletRequest) request, servlet);
            filterChain.doFilter(request, response);
        } finally {
            // 恢复原始URI
            ((ApplicationRequest) request).setRequestURI(originalUri);
            ((ApplicationRequest) request).setServletPath(originalServletPath);
            ((ApplicationRequest) request).setPathInfo(originalPathInfo);
        }
    }
}
