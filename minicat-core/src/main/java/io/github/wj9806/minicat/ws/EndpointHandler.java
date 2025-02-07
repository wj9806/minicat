package io.github.wj9806.minicat.ws;

import io.github.wj9806.minicat.util.UriTemplate;

import javax.servlet.ServletContext;
import javax.websocket.*;
import javax.websocket.server.ServerEndpointConfig;
import java.util.ArrayList;
import java.util.List;

public class EndpointHandler {

    private final Class<?> endpointClass;
    private final String path;
    private final List<Class<? extends Encoder>> encoders = new ArrayList<>();
    private final List<Class<? extends Decoder>> decoders = new ArrayList<>();;
    private final List<String> subprotocols = new ArrayList<>();;
    private final ServerEndpointConfig.Configurator serverEndpointConfigurator;
    private final List<Extension> extensions = new ArrayList<>();
    private final UriTemplate uriTemplate;
    private final ServletContext sc;
    private final ServerEndpointConfig sec;
    private final EndpointMetadata endpointMethod;

    public EndpointHandler(ServerEndpointConfig sec, ServletContext sc) throws DeploymentException {
        this.endpointClass = sec.getEndpointClass();
        this.path = sec.getPath();
        this.encoders.addAll(sec.getEncoders());
        this.decoders.addAll(sec.getDecoders());
        this.extensions.addAll(sec.getExtensions());
        this.subprotocols.addAll(sec.getSubprotocols());
        this.serverEndpointConfigurator = sec.getConfigurator();
        this.uriTemplate = new UriTemplate(path, sc.getContextPath());
        this.sc = sc;
        this.sec = sec;
        this.endpointMethod = new EndpointMetadata(endpointClass);
    }

    public boolean canHandle(String requestURI) {
        return uriTemplate.canHandle(requestURI);
    }

    public Class<?> getEndpointClass() {
        return endpointClass;
    }

    public String getPath() {
        return path;
    }

    public List<Class<? extends Encoder>> getEncoders() {
        return encoders;
    }

    public List<Class<? extends Decoder>> getDecoders() {
        return decoders;
    }

    public List<String> getSubprotocols() {
        return subprotocols;
    }

    public ServerEndpointConfig.Configurator getConfigurator() {
        return serverEndpointConfigurator;
    }

    public List<Extension> getExtensions() {
        return extensions;
    }

    public UriTemplate getUriTemplate() {
        return uriTemplate;
    }

    public ServerEndpointConfig getSec() {
        return sec;
    }

    public EndpointMetadata getEndpointMethod() {
        return endpointMethod;
    }
}
