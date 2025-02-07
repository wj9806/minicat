package io.github.wj9806.minicat.core;

import javax.servlet.*;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

public class FilterChainImpl implements FilterChain {
    private final Iterator<Filter> filterIterator;
    private final Servlet servlet;

    public FilterChainImpl(List<Filter> filters, Servlet servlet) {
        this.filterIterator = filters.iterator();
        this.servlet = servlet;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response) 
            throws IOException, ServletException {
        if (filterIterator.hasNext()) {
            Filter filter = filterIterator.next();
            filter.doFilter(request, response, this);
        } else if (servlet != null) {
            servlet.service(request, response);
        }
    }
}
