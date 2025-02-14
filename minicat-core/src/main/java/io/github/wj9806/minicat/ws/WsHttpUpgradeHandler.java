package io.github.wj9806.minicat.ws;

import io.github.wj9806.minicat.ws.message.BaseMessageHandlerWhole;
import io.github.wj9806.minicat.ws.message.TextMessageHandlerWhole;
import io.github.wj9806.minicat.ws.processor.WsProcessor;

import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.WebConnection;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.MessageHandler;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WsHttpUpgradeHandler implements HttpUpgradeHandler {

    private WsServerContainer sc;

    private WsProcessor<?> processor;

    private EndpointHandler handler;

    private ServerEndpointConfig sec;

    private WsHandshakeRequest wsReq;

    private WsSession session;

    private Endpoint endpoint;

    private List<BaseMessageHandlerWhole<?>> wholeMessageHandlers;

    private volatile long lastIdleTime;

    @Override
    public void init(WebConnection wc) {
        this.processor = (WsProcessor<?>) wc;
        this.processor.setServerContainer(sc);
        this.session = new WsSession(sc, handler, wsReq, sec, processor);
        this.endpoint = session.getEndpoint();
        this.wholeMessageHandlers = new ArrayList<>();
        this.initMessageHandlers();
        this.onOpen();
        this.lastIdleTime = System.currentTimeMillis();
    }

    private void initMessageHandlers() {

        EndpointMetadata.MessageHandlerMetadata metadata = handler.getEndpointMethod().getMessageMetadata();
        EndpointMetadata.EndpointParam[] params = metadata.getParams();
        for (EndpointMetadata.EndpointParam param : params) {
            if (param.getType().equals(String.class)) {
                wholeMessageHandlers.add(new TextMessageHandlerWhole(session));
            }
        }
    }

    public void onOpen() {
        this.endpoint.onOpen(this.session, this.sec);
    }

    public void onClose(CloseReason closeReason) throws IOException {
        session.sendCloseFrame(closeReason);
        processor.sock().setWsProcessor(null);
        this.endpoint.onClose(this.session, closeReason);
    }

    public void onMessage(byte[] payload) {
        String message = new String(payload);
        for (BaseMessageHandlerWhole<?> wholeMessageHandler : wholeMessageHandlers) {
            if (wholeMessageHandler.canHandle(message)) {
                ((MessageHandler.Whole<String>)wholeMessageHandler).onMessage(message);
                break;
            }
        }
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

    public boolean idleTimeout() {
        if (session.getMaxIdleTimeout() == -1) return false;
        return System.currentTimeMillis() - lastIdleTime >= session.getMaxIdleTimeout();
    }

    @Override
    public void destroy() {
        try {
            processor.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
