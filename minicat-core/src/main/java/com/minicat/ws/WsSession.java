package com.minicat.ws;

import com.minicat.ws.processor.WsProcessor;

import javax.websocket.*;
import javax.websocket.server.ServerEndpointConfig;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;

public class WsSession implements Session {

    private static final AtomicLong adder = new AtomicLong();

    private final WebSocketContainer container;
    private final Set<MessageHandler> messageHandlers = new CopyOnWriteArraySet<>();
    private final Map<String, Object> userProperties = new HashMap<>();
    private final String id;
    private final URI requestURI;
    private final Map<String, List<String>> requestParameterMap;
    private final String queryString;
    private Map<String, String> pathParameters;
    private final Principal userPrincipal;
    private Set<Session> openSessions;
    private volatile long maxIdleTimeout;
    private int maxBinaryMessageBufferSize;
    private int maxTextMessageBufferSize;
    private volatile boolean open = true;
    private final String negotiatedSubprotocol;
    private List<Extension> negotiatedExtensions;
    private String protocolVersion = "13"; // Default WebSocket protocol version

    private final EndpointHandler handler;
    private final Endpoint endpoint;
    private final RemoteEndpoint.Basic basicRemote;
    private final WsProcessor<?> processor;

    public WsSession(WebSocketContainer container, EndpointHandler handler,
                     WsHandshakeRequest wsReq, ServerEndpointConfig sec, WsProcessor<?> processor) {
        this.container = container;
        this.handler = handler;
        this.userProperties.putAll(sec.getUserProperties());
        this.id = String.valueOf(adder.incrementAndGet());
        this.requestURI = wsReq.getRequestURI();
        this.userPrincipal = wsReq.getUserPrincipal();
        this.queryString = wsReq.getQueryString();
        this.requestParameterMap = wsReq.getParameterMap();
        this.pathParameters = wsReq.getPathParams();
        this.negotiatedSubprotocol = null;
        this.maxIdleTimeout = container.getDefaultMaxSessionIdleTimeout();

        Object endpointInstance;
        ServerEndpointConfig.Configurator configurator = sec.getConfigurator();
        try {
            endpointInstance = configurator.getEndpointInstance(handler.getEndpointClass());
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        }

        if (endpointInstance instanceof Endpoint) {
            this.endpoint = (Endpoint) endpointInstance;
        } else {
            this.endpoint = new EndpointAdapter(handler, endpointInstance);
        }
        this.processor = processor;
        this.basicRemote = new BasicRemoteEndpoint(processor, sec.getEncoders());
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    @Override
    public WebSocketContainer getContainer() {
        return container;
    }

    @Override
    public void addMessageHandler(MessageHandler handler) throws IllegalStateException {
        if (!open) {
            throw new IllegalStateException("Session is closed");
        }
        messageHandlers.add(handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Whole<T> handler) {
        addMessageHandler(handler);
    }

    @Override
    public <T> void addMessageHandler(Class<T> clazz, MessageHandler.Partial<T> handler) {
        addMessageHandler(handler);
    }

    @Override
    public Set<MessageHandler> getMessageHandlers() {
        return Collections.unmodifiableSet(messageHandlers);
    }

    @Override
    public void removeMessageHandler(MessageHandler handler) {
        messageHandlers.remove(handler);
    }

    @Override
    public String getProtocolVersion() {
        return protocolVersion;
    }

    @Override
    public String getNegotiatedSubprotocol() {
        return negotiatedSubprotocol;
    }

    @Override
    public List<Extension> getNegotiatedExtensions() {
        return negotiatedExtensions;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public long getMaxIdleTimeout() {
        return maxIdleTimeout;
    }

    @Override
    public void setMaxIdleTimeout(long milliseconds) {
        this.maxIdleTimeout = milliseconds;
    }

    @Override
    public void setMaxBinaryMessageBufferSize(int length) {
        this.maxBinaryMessageBufferSize = length;
    }

    @Override
    public int getMaxBinaryMessageBufferSize() {
        return maxBinaryMessageBufferSize;
    }

    @Override
    public void setMaxTextMessageBufferSize(int length) {
        this.maxTextMessageBufferSize = length;
    }

    @Override
    public int getMaxTextMessageBufferSize() {
        return maxTextMessageBufferSize;
    }

    @Override
    public RemoteEndpoint.Async getAsyncRemote() {
        if (!isOpen())
            throw new IllegalStateException("ws connection is not open");
        return null;
    }

    @Override
    public RemoteEndpoint.Basic getBasicRemote() {
        if (!isOpen())
            throw new IllegalStateException("ws connection is not open");
        return basicRemote;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void close() throws IOException {
        close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, ""));
    }

    @Override
    public void close(CloseReason closeReason) throws IOException {
        this.open = false;
    }

    @Override
    public URI getRequestURI() {
        return requestURI;
    }

    @Override
    public Map<String, List<String>> getRequestParameterMap() {
        return requestParameterMap;
    }

    @Override
    public String getQueryString() {
        return queryString;
    }

    @Override
    public Map<String, String> getPathParameters() {
        return pathParameters;
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }

    @Override
    public Principal getUserPrincipal() {
        return userPrincipal;
    }

    @Override
    public Set<Session> getOpenSessions() {
        return Collections.unmodifiableSet(openSessions);
    }

    public EndpointHandler getHandler() {
        return handler;
    }

    public void sendCloseFrame(CloseReason closeReason) throws IOException {
        String reason = closeReason.getReasonPhrase();
        int closeCode = closeReason.getCloseCode().getCode();

        ByteArrayOutputStream frame = new ByteArrayOutputStream();

        frame.write(0x88);

        byte[] reasonBytes = reason != null ? reason.getBytes() : new byte[0];
        int payloadLength = 2 + reasonBytes.length;

        if (payloadLength < 126) {
            frame.write(payloadLength);
        } else if (payloadLength < 65536) {
            frame.write(126);
            frame.write(payloadLength >> 8);
            frame.write(payloadLength & 0xFF);
        }

        frame.write(closeCode >> 8);
        frame.write(closeCode & 0xFF);

        frame.write(reasonBytes);

        processor.send(ByteBuffer.wrap(frame.toByteArray()));
        processor.flush();

        this.open = false;
    }
}
