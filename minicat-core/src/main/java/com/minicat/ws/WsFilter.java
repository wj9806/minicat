package com.minicat.ws;

import com.minicat.http.HttpHeaders;
import com.minicat.net.Sock;
import com.minicat.server.Constants;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class WsFilter implements Filter {

    private WsServerContainer sc;

    private static final String WEBSOCKET_MAGIC_STRING = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ServletContext servletContext = filterConfig.getServletContext();
        sc = (WsServerContainer) servletContext.getAttribute(WsConstants.WS_SERVER_CONTAINER_ATTRIBUTE);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!isWsRequest(request)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String requestURI = req.getRequestURI();
        EndpointHandler handler = sc.findHandler(requestURI);
        if (handler == null) {
            chain.doFilter(request, response);
            return;
        }

        upgrade(req, resp, handler);
    }

    private void upgrade(HttpServletRequest req, HttpServletResponse resp, EndpointHandler handler) throws IOException, ServletException {
        //校验sec-websocket-version=13
        if (!HttpHeaders.containsHeader(req, HttpHeaders.WEBSOCKET_VERSION_NAME, "13")) {
            resp.setStatus(426);
            resp.setHeader(HttpHeaders.WEBSOCKET_VERSION_NAME, "13");
            return;
        }
        String origin = req.getHeader(HttpHeaders.ORIGIN);
        //校验origin
        if (!handler.getConfigurator().checkOrigin(origin)) {
            resp.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        if (!HttpHeaders.containsHeader(req, HttpHeaders.WEBSOCKET_KEY_NAME)) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        //校验sec-websocket-key
        String key = req.getHeader(HttpHeaders.WEBSOCKET_KEY_NAME);
        if (key.length() != 24) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        char[] keyChars = key.toCharArray();
        if (keyChars[22] != '=' || keyChars[23] != '=') {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        resp.setHeader(HttpHeaders.WEBSOCKET_ACCEPT, generateAcceptKey(key));
        resp.setHeader(HttpHeaders.CONNECTION, HttpHeaders.UPGRADE);
        resp.setHeader(HttpHeaders.UPGRADE, "websocket");

        resp.setStatus(HttpServletResponse.SC_SWITCHING_PROTOCOLS);

        WsHandshakeRequest wsReq = new WsHandshakeRequest(req, handler.getUriTemplate().extractParams(req.getRequestURI()));
        WsHandshakeResponse wsResp = new WsHandshakeResponse();

        WsRequestServerEndpointConfig sec = new WsRequestServerEndpointConfig(handler.getSec());
        handler.getConfigurator().modifyHandshake(sec, wsReq, wsResp);

        //增加自定义请求头
        for (Map.Entry<String, List<String>> entry : wsResp.getHeaders().entrySet()) {
            for (String headerValue : entry.getValue()) {
                resp.addHeader(entry.getKey(), headerValue);
            }
        }

        resp.flushBuffer();

        initWsConnection(req, handler, sec, wsReq);
    }

    private void initWsConnection(HttpServletRequest req, EndpointHandler handler, WsRequestServerEndpointConfig sec, WsHandshakeRequest wsReq) throws IOException, ServletException {
        WsHttpUpgradeHandler hand = req.upgrade(WsHttpUpgradeHandler.class);
        hand.setHandler(handler);
        hand.setServerEndpointConfig(sec);
        hand.setHandshakeRequest(wsReq);
        hand.setServerContainer(sc);

        Sock<?> sock = (Sock<?>) req.getAttribute(Constants.REQUEST_SOCK);
        hand.init(sock.processor());
    }

    @Override
    public void destroy() {

    }

    private String generateAcceptKey(String key) {
        try {
            String concat = key + WEBSOCKET_MAGIC_STRING;
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] hash = sha1.digest(concat.getBytes());
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isWsRequest(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) return false;
        HttpServletRequest req = (HttpServletRequest) request;
        return HttpHeaders.containsHeader(req, HttpHeaders.UPGRADE, "websocket")
                && req.getMethod().equals("GET")
                && HttpHeaders.containsHeader(req, HttpHeaders.CONNECTION, HttpHeaders.UPGRADE);
    }
}
