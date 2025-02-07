package io.github.wj9806.minicat.core;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.annotation.WebFilter;
import java.util.*;
import java.util.regex.Pattern;

public class FilterRegistrationImpl extends RegistrationBase implements FilterRegistration.Dynamic {
    private final Filter filter;
    private final List<Pattern> urlPatterns;
    private final List<String> urlPatternMappings;
    private final FilterConfigImpl filterConfig;

    public FilterRegistrationImpl(String filterName, Filter filter, FilterConfigImpl filterConfig) {
        super(filterName, filter.getClass().getName());
        this.filter = filter;
        this.filterConfig = filterConfig;
        this.urlPatterns = new ArrayList<>();
        this.urlPatternMappings = new ArrayList<>();

        WebFilter webFilter = filter.getClass().getAnnotation(WebFilter.class);
        if (webFilter != null) {
            handleWebInitParams(webFilter.initParams());
            handleMapping(webFilter.urlPatterns(), webFilter.value());
        }
        filterConfig.setInitParameters(getInitParameters());
    }

    private void handleMapping(String[] urlPatterns, String[] value) {
        if (value != null && value.length > 0) {
            for (String pa : value) {
                addUrlPattern(pa);
            }
        } else if (urlPatterns != null && urlPatterns.length > 0) {
            for (String pa : urlPatterns) {
                addUrlPattern(pa);
            }
        }
    }

    public void addUrlPattern(String urlPattern) {
        urlPatternMappings.add(urlPattern);
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
        return urlPatternMappings;
    }
}
