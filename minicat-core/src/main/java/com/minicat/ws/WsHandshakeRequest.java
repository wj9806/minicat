package com.minicat.ws;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.websocket.server.HandshakeRequest;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.*;

public class WsHandshakeRequest implements HandshakeRequest {

    private final Map<String, List<String>> parameterMap;
    private final URI reqUri;
    private final String queryString;
    private final HttpSession session;
    private final Principal userPrincipal;
    private final Map<String, List<String>> headers;
    private final Map<String, String> pathParams;

    public WsHandshakeRequest(HttpServletRequest req, Map<String, String> pathParams) {
        this.reqUri = buildRequestUri(req);
        this.pathParams = pathParams;
        this.queryString = req.getQueryString();
        this.userPrincipal = req.getUserPrincipal();
        this.session = req.getSession(false);

        Map<String, List<String>> pm = new HashMap<>();

        Enumeration<String> parameterNames = req.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            pm.put(name,Collections.unmodifiableList(Arrays.asList(req.getParameterValues(name))));
        }
        if (pathParams != null) {
            for (String key : pathParams.keySet()) {
                pm.put(key, Collections.singletonList(pathParams.get(key)));
            }
        }
        parameterMap = Collections.unmodifiableMap(pm);

        Map<String, List<String>> newHeaders = new HashMap<>();
        Enumeration<String> headerNames = req.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();

            newHeaders.put(headerName, Collections.unmodifiableList(Collections.list(req.getHeaders(headerName))));
        }
        headers = Collections.unmodifiableMap(newHeaders);
    }

    private static URI buildRequestUri(HttpServletRequest req) {

        StringBuilder uri = new StringBuilder();
        String scheme = req.getScheme();
        int port = req.getServerPort();
        if (port < 0) {
            // Work around java.net.URL bug
            port = 80;
        }

        if ("http".equals(scheme)) {
            uri.append("ws");
        } else if ("https".equals(scheme)) {
            uri.append("wss");
        } else if ("wss".equals(scheme) || "ws".equals(scheme)) {
            uri.append(scheme);
        } else {
            throw new IllegalArgumentException("unknown scheme: " + scheme);
        }

        uri.append("://");
        uri.append(req.getServerName());

        if ((scheme.equals("http") && (port != 80)) || (scheme.equals("ws") && (port != 80)) ||
                (scheme.equals("wss") && (port != 443)) || (scheme.equals("https") && (port != 443))) {
            uri.append(':');
            uri.append(port);
        }

        uri.append(req.getRequestURI());

        if (req.getQueryString() != null) {
            uri.append('?');
            uri.append(req.getQueryString());
        }

        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            // Should never happen
            throw new IllegalArgumentException("invalidUri", e);
        }
    }

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public URI getRequestURI() {
        return reqUri;
    }

    @Override
    public boolean isUserInRole(String role) {
        return true;
    }

    @Override
    public Object getHttpSession() {
        return session;
    }

    @Override
    public Map<String, List<String>> getParameterMap() {
        return parameterMap;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    public Map<String, String> getPathParams() {
        return pathParams;
    }
}
