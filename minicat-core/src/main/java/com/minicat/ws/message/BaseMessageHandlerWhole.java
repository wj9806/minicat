package com.minicat.ws.message;

import com.minicat.ws.EndpointAdapter;
import com.minicat.ws.EndpointHandler;
import com.minicat.ws.EndpointMetadata;
import com.minicat.ws.WsSession;

import javax.websocket.Endpoint;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class BaseMessageHandlerWhole<T> implements MessageHandler.Whole<T> {

    protected final WsSession session;
    private final Object source;
    private final Method method;
    private final EndpointMetadata.EndpointParam[] params;
    private final EndpointHandler endpointHandler;

    public BaseMessageHandlerWhole(WsSession session) {
        this.session = session;
        Endpoint endpoint = session.getEndpoint();
        this.source = ((EndpointAdapter) endpoint).getSource();
        this.endpointHandler = session.getHandler();
        EndpointMetadata.MessageHandlerMetadata messageMetadata = endpointHandler.getEndpointMethod().getMessageMetadata();
        this.method = messageMetadata.getMessage();
        this.params = messageMetadata.getParams();
    }

    @Override
    public void onMessage(T message) {
        T msg = convert(message);

        Object[] parameters = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            EndpointMetadata.EndpointParam param = params[i];
            Class<?> type = param.getType();
            String name = param.getName();
            if (type.equals(String.class)) {
                if (name != null && !name.isEmpty()) {
                    String value = session.getPathParameters().get(name);
                    parameters[i] = value;
                } else {
                    parameters[i] = msg;
                }
            } else if (Session.class.isAssignableFrom(type)) {
                parameters[i] = session;
            }
        }
        Object ret = null;
        try {
            ret = method.invoke(source, parameters);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract boolean canHandle(Object msg);

    public abstract T convert(Object msg);
}
