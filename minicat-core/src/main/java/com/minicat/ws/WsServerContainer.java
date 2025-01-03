package com.minicat.ws;

import com.minicat.http.ApplicationRequest;
import com.minicat.net.Sock;
import com.minicat.server.Constants;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpUpgradeHandler;
import javax.websocket.*;
import javax.websocket.server.ServerContainer;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class WsServerContainer implements ServerContainer {

    private long defaultAsyncTimeout = -1;
    private int maxBinaryMessageBufferSize = 1024 * 1024;
    private int maxTextMessageBufferSize = 1024 * 1024;

    private final ServletContext ctx;
    private final List<EndpointHandler> handlers;
    private final Map<Sock<?>, WsHttpUpgradeHandler> upgradeHandlerMap;

    public WsServerContainer(ServletContext ctx) {
        this.ctx = ctx;
        this.handlers = new CopyOnWriteArrayList<>();
        this.upgradeHandlerMap = new ConcurrentHashMap<>();
    }

    @Override
    public long getDefaultAsyncSendTimeout() {
        return defaultAsyncTimeout;
    }

    @Override
    public void setAsyncSendTimeout(long timeoutmillis) {
        this.defaultAsyncTimeout = timeoutmillis;
    }

    @Override
    public Session connectToServer(Object annotatedEndpointInstance, URI path) throws DeploymentException, IOException {
        // 实现连接到服务器的逻辑
        // 这里需要根据 annotatedEndpointInstance 创建相应的 Session
        // 具体实现可以根据需要进行调整
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Session connectToServer(Class<?> annotatedEndpointClass, URI path) throws DeploymentException, IOException {
        // 实现连接到服务器的逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Session connectToServer(Endpoint endpointInstance, ClientEndpointConfig cec, URI path) throws DeploymentException, IOException {
        // 实现连接到服务器的逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public Session connectToServer(Class<? extends Endpoint> endpointClass, ClientEndpointConfig cec, URI path) throws DeploymentException, IOException {
        // 实现连接到服务器的逻辑
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public long getDefaultMaxSessionIdleTimeout() {
        return defaultAsyncTimeout; // 返回默认的最大会话空闲超时时间
    }

    @Override
    public void setDefaultMaxSessionIdleTimeout(long timeout) {
        this.defaultAsyncTimeout = timeout; // 设置最大会话空闲超时时间
    }

    @Override
    public int getDefaultMaxBinaryMessageBufferSize() {
        return this.maxBinaryMessageBufferSize; // 默认的二进制消息缓冲区大小，例如 1MB
    }

    @Override
    public void setDefaultMaxBinaryMessageBufferSize(int maxBinaryMessageBufferSize) {
        // 设置最大二进制消息缓冲区大小
        this.maxBinaryMessageBufferSize = maxBinaryMessageBufferSize;
    }

    @Override
    public int getDefaultMaxTextMessageBufferSize() {
        return this.maxTextMessageBufferSize; // 默认的文本消息缓冲区大小，例如 1MB
    }

    @Override
    public void setDefaultMaxTextMessageBufferSize(int maxTextMessageBufferSize) {
        // 设置最大文本消息缓冲区大小
        this.maxTextMessageBufferSize = maxTextMessageBufferSize;
    }

    @Override
    public Set<Extension> getInstalledExtensions() {
        return Collections.emptySet();
    }

    @Override
    public void addEndpoint(Class<?> endpointClass) throws DeploymentException {
        ServerEndpoint serverEndpoint = endpointClass.getAnnotation(ServerEndpoint.class);
        if (serverEndpoint == null)
            throw new DeploymentException("cannot find ServerEndpoint Annotation on " + endpointClass.getName());
        String path = serverEndpoint.value();

        ServerEndpointConfig sec = ServerEndpointConfig.Builder.create(endpointClass, path)
                .decoders(Arrays.asList(serverEndpoint.decoders()))
                .encoders(Arrays.asList(serverEndpoint.encoders()))
                .subprotocols(Arrays.asList(serverEndpoint.subprotocols()))
                .build();

        addEndpoint(sec);
    }

    //注册ServerEndpointConfig
    @Override
    public void addEndpoint(ServerEndpointConfig serverConfig) throws DeploymentException {
        EndpointHandler handler = new EndpointHandler(serverConfig, ctx);
        handlers.add(handler);
    }

    public EndpointHandler findHandler(String requestURI) {
        for (EndpointHandler handler : handlers) {
            if (handler.canHandle(requestURI))
                return handler;
        }
        return null;
    }

    public <T extends HttpUpgradeHandler> void upgrade(ApplicationRequest req, T handler) {
        Sock<?> sock = (Sock<?>) req.getAttribute(Constants.REQUEST_SOCK);
        if (handler instanceof WsHttpUpgradeHandler) {
            upgradeHandlerMap.put(sock, (WsHttpUpgradeHandler) handler);
        }
    }

    public WsHttpUpgradeHandler getUpgradeHandler(Sock<?> sock) {
        return upgradeHandlerMap.get(sock);
    }
}
