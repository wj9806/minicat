package io.github.wj9806.minicat.core.test.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import java.io.IOException;

@WebFilter(initParams = @WebInitParam(name = "key", value = "value"))
public class TestFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(TestFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        String value = filterConfig.getInitParameter("key");
        logger.debug("TestFilter InitParameter key: {}", value);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        logger.info("TestFilter-doFilter");

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
