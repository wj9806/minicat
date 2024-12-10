package com.minicat.core;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.annotation.WebFilter;
import java.util.*;
import java.util.regex.Pattern;

public class FilterRegistrationImpl extends RegistrationBase implements FilterRegistration.Dynamic {
    private final Filter filter;
    private final List<Pattern> urlPatterns;
    private final FilterConfigImpl filterConfig;

    public FilterRegistrationImpl(String filterName, Filter filter, FilterConfigImpl filterConfig) {
        super(filterName, filter.getClass().getName());
        this.filter = filter;
        this.filterConfig = filterConfig;
        this.urlPatterns = new ArrayList<>();

        WebFilter webFilter = filter.getClass().getAnnotation(WebFilter.class);
        if (webFilter != null)
            handleWebInitParams(webFilter.initParams());
        filterConfig.setInitParameters(getInitParameters());
    }

    public void addUrlPattern(String urlPattern) {
        // 将URL模式转换为正则表达式
        String regex = urlPattern
            .replace(".", "\\.")
            .replace("/*", "/.*")
            .replace("*.", ".*\\.");
        urlPatterns.add(Pattern.compile(regex));
    }

    public boolean matches(String uri) {
        for (Pattern pattern : urlPatterns) {
            if (pattern.matcher(uri).matches()) {
                return true;
            }
        }
        return false;
    }

    public Filter getFilter() {
        return filter;
    }

    public FilterConfigImpl getFilterConfig() {
        return filterConfig;
    }

    @Override
    public void addMappingForServletNames(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... servletNames) {

    }

    @Override
    public Collection<String> getServletNameMappings() {
        return null;
    }

    @Override
    public void addMappingForUrlPatterns(EnumSet<DispatcherType> dispatcherTypes, boolean isMatchAfter, String... urlPatterns) {
        for (String urlPattern : urlPatterns) {
            addUrlPattern(urlPattern);
        }
    }

    @Override
    public Collection<String> getUrlPatternMappings() {
        return null;
    }
}
