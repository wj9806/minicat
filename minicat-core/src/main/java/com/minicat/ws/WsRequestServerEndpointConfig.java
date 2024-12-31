package com.minicat.ws;

import javax.websocket.Decoder;
import javax.websocket.Encoder;
import javax.websocket.Extension;
import javax.websocket.server.ServerEndpointConfig;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WsRequestServerEndpointConfig implements ServerEndpointConfig {

    private final ServerEndpointConfig sec;
    private final Map<String, Object> userProperties;

    public WsRequestServerEndpointConfig(ServerEndpointConfig sec) {
        this.sec = sec;
        this.userProperties = new ConcurrentHashMap<>();
        this.userProperties.putAll(sec.getUserProperties());
    }

    @Override
    public Class<?> getEndpointClass() {
        return sec.getEndpointClass();
    }

    @Override
    public String getPath() {
        return sec.getPath();
    }

    @Override
    public List<String> getSubprotocols() {
        return sec.getSubprotocols();
    }

    @Override
    public List<Extension> getExtensions() {
        return sec.getExtensions();
    }

    @Override
    public Configurator getConfigurator() {
        return sec.getConfigurator();
    }

    @Override
    public List<Class<? extends Encoder>> getEncoders() {
        return sec.getEncoders();
    }

    @Override
    public List<Class<? extends Decoder>> getDecoders() {
        return sec.getDecoders();
    }

    @Override
    public Map<String, Object> getUserProperties() {
        return userProperties;
    }
}
