package io.github.wj9806.minicat.core;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import java.util.*;

public class FilterConfigImpl implements FilterConfig {
    private final String filterName;
    private Map<String, String> initParameters;
    private final ServletContext servletContext;

    public FilterConfigImpl(String filterName, ServletContext servletContext) {
        this.filterName = filterName;
        this.servletContext = servletContext;
    }

    @Override
    public String getFilterName() {
        return filterName;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public String getInitParameter(String name) {
        return initParameters.get(name);
    }

    public void setInitParameters(Map<String, String> initParameters) {
        this.initParameters = initParameters;
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }

    public void addInitParameter(String name, String value) {
        initParameters.put(name, value);
    }
}
