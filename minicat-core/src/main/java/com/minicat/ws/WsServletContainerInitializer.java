package com.minicat.ws;

import javax.servlet.*;
import javax.servlet.annotation.HandlesTypes;
import javax.websocket.DeploymentException;
import javax.websocket.Endpoint;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.lang.reflect.Modifier;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static com.minicat.ws.WsConstants.WS_SERVER_CONTAINER_ATTRIBUTE;

@HandlesTypes({ ServerEndpoint.class, ServerApplicationConfig.class, Endpoint.class })
public class WsServletContainerInitializer implements ServletContainerInitializer {

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        WsServerContainer container = initContainer(ctx);

        Set<ServerApplicationConfig> serverApplicationConfigs = new HashSet<>();
        Set<Class<? extends Endpoint>> scannedEndpointClasses = new HashSet<>();
        Set<Class<?>> scannedPojoEndpoints = new HashSet<>();
        try {
            for (Class<?> clazz : c) {
                int modifiers = clazz.getModifiers();
                if (Modifier.isAbstract(modifiers) || !Modifier.isPublic(modifiers)
                        || Modifier.isInterface(modifiers)) {
                    continue;
                }

                if (clazz.isAnnotationPresent(ServerEndpoint.class)) {
                    scannedPojoEndpoints.add(clazz);
                }

                if (Endpoint.class.isAssignableFrom(clazz)) {
                    Class<? extends Endpoint> endpoint = (Class<? extends Endpoint>) clazz;
                    scannedEndpointClasses.add(endpoint);
                }
                if (ServerApplicationConfig.class.isAssignableFrom(clazz)) {
                    serverApplicationConfigs.add((ServerApplicationConfig) clazz.getConstructor().newInstance());
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }

        Set<ServerEndpointConfig> filteredEndpointConfigs = new HashSet<>();
        Set<Class<?>> filteredPojoEndpoints = new HashSet<>();

        if (serverApplicationConfigs.isEmpty()) {
            filteredPojoEndpoints.addAll(scannedPojoEndpoints);
        } else {
            for (ServerApplicationConfig config : serverApplicationConfigs) {
                Set<ServerEndpointConfig> configFilteredEndpoints = config.getEndpointConfigs(scannedEndpointClasses);
                if (configFilteredEndpoints != null) {
                    filteredEndpointConfigs.addAll(configFilteredEndpoints);
                }
                Set<Class<?>> configFilteredPojos = config.getAnnotatedEndpointClasses(scannedPojoEndpoints);
                if (configFilteredPojos != null) {
                    filteredPojoEndpoints.addAll(configFilteredPojos);
                }
            }
        }

        try {
            for (ServerEndpointConfig config : filteredEndpointConfigs) {
                container.addEndpoint(config);
            }
            for (Class<?> clazz : filteredPojoEndpoints) {
                container.addEndpoint(clazz);
            }
        } catch (DeploymentException e) {
            throw new ServletException(e);
        }

        FilterRegistration.Dynamic registration = ctx.addFilter("WsFilter", WsFilter.class);
        EnumSet<DispatcherType> types = EnumSet.of(DispatcherType.REQUEST, DispatcherType.FORWARD);
        registration.addMappingForUrlPatterns(types, true, "/*");
    }

    private WsServerContainer initContainer(ServletContext ctx) {
        WsServerContainer container = new WsServerContainer(ctx);
        ctx.setAttribute(WS_SERVER_CONTAINER_ATTRIBUTE, container);
        return container;
    }
}
