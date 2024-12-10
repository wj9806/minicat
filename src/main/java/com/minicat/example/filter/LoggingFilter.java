package com.minicat.example.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

public class LoggingFilter implements Filter {
    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        logger.info("LoggingFilter initialized");
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // 记录请求开始时间
        long startTime = System.currentTimeMillis();
        
        try {
            // 继续处理请求
            chain.doFilter(request, response);
        } finally {
            // 记录请求处理时间
            long endTime = System.currentTimeMillis();
            logger.info("Request completed in {} ms", endTime - startTime);
        }
    }

    @Override
    public void destroy() {
        logger.info("LoggingFilter destroyed");
    }
}
