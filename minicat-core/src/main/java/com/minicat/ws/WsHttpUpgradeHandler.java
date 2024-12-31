package com.minicat.ws;

import com.minicat.server.processor.Processor;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerEndpointConfig;

public class WsHttpUpgradeHandler implements HttpUpgradeHandler {

    private WsServerContainer sc;

    private WebConnection wc;

    private EndpointHandler handler;

    private ServerEndpointConfig sec;

    private WsHandshakeRequest wsReq;

    private WsSession session;

    private Endpoint endpoint;

    @Override
    public void init(WebConnection wc) {
        this.wc = wc;
        this.session = new WsSession(sc, handler, wsReq, sec, (Processor<?>) wc);
        this.endpoint = session.getEndpoint();
        endpoint.onOpen(this.session, sec);
    }

    public void setHandler(EndpointHandler handler) {
        this.handler = handler;
    }

    public void setServerEndpointConfig(ServerEndpointConfig sec) {
        this.sec = sec;
    }

    public void setHandshakeRequest(WsHandshakeRequest wsReq) {
        this.wsReq = wsReq;
    }

    public void setServerContainer(WsServerContainer sc) {
        this.sc = sc;
    }

    @Override
    public void destroy() {
        try {
            wc.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
