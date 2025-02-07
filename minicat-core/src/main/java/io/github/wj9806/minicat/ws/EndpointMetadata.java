package io.github.wj9806.minicat.ws;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

public class EndpointMetadata {

    private Method open;
    private EndpointParam[] openParams;
    private Method close;
    private EndpointParam[] closeParams;
    private Method error;
    private EndpointParam[] errorParams;
    private MessageHandlerMetadata message;

    public EndpointMetadata(Class<?> endpointClass) throws DeploymentException {
        Method[] methods = endpointClass.getDeclaredMethods();
        for (Method method : methods) {
            //跳过合成方法、非公共方法
            if (method.isSynthetic()
                    || !Modifier.isPublic(method.getModifiers())) {
                continue;
            }

            if (method.isAnnotationPresent(OnOpen.class)) {
                if (this.open != null)
                    throw new DeploymentException(endpointClass.getName() + " has more than one OnOpen method");
                this.open = method;
                this.openParams = EndpointParam.from(method);
            } else if (method.isAnnotationPresent(OnClose.class)) {
                if (this.close != null)
                    throw new DeploymentException(endpointClass.getName() + " has more than one OnClose method");
                this.close = method;
                this.closeParams = EndpointParam.from(method);
            } else if (method.isAnnotationPresent(OnError.class)) {
                if (this.error != null)
                    throw new DeploymentException(endpointClass.getName() + " has more than one OnError method");
                this.error = method;
                this.errorParams = EndpointParam.from(method);
            } else if (method.isAnnotationPresent(OnMessage.class)) {
                if (this.message != null)
                    throw new DeploymentException(endpointClass.getName() + " has more than one OnMessage method");
                this.message = MessageHandlerMetadata.from(method);
            }
        }
    }

    public void onOpen(Object endpoint, Session session, EndpointConfig sec) {
        Object[] args = new Object[openParams.length];

        for (int i = 0; i < openParams.length; i++) {
            EndpointParam param = openParams[i];
            if (Session.class.equals(param.getType())) {
                args[i] = session;
            } else if (EndpointConfig.class.equals(param.getType())) {
                args[i] = sec;
            } else if (param.getName() != null) {
                args[i] = session.getPathParameters().get(param.getName());
            } else {
                args[i] = null;
            }
        }

        try {
            open.invoke(endpoint, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public void onClose(Object endpoint, Session session, CloseReason closeReason) {
        Object[] args = new Object[closeParams.length];

        for (int i = 0; i < closeParams.length; i++) {
            EndpointParam param = closeParams[i];
            if (Session.class.equals(param.getType())) {
                args[i] = session;
            } else if (CloseReason.class.equals(param.getType())) {
                args[i] = closeReason;
            } else if (param.getName() != null) {
                args[i] = session.getPathParameters().get(param.getName());
            } else {
                args[i] = null;
            }
        }

        try {
            close.invoke(endpoint, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public MessageHandlerMetadata getMessageMetadata() {
        return message;
    }

    public static class MessageHandlerMetadata {
        private Method message;
        private EndpointParam[] params;

        public static MessageHandlerMetadata from(Method method) {
            MessageHandlerMetadata metadata = new MessageHandlerMetadata();
            metadata.message = method;
            metadata.params = EndpointParam.from(method);
            return metadata;
        }

        public Method getMessage() {
            return message;
        }

        public EndpointParam[] getParams() {
            return params;
        }
    }

    public static class EndpointParam {
        private final Class<?> type;
        private final String name;

        public Class<?> getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public EndpointParam(Class<?> type, String name) {
            this.type = type;
            this.name = name;
        }

        public static EndpointParam[] from(Method method) {
            Parameter[] parameters = method.getParameters();
            if (parameters == null || parameters.length == 0) return new EndpointParam[0];

            EndpointParam[] params = new EndpointParam[parameters.length];

            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                PathParam pathParam = parameter.getAnnotation(PathParam.class);
                if (pathParam == null) {
                    params[i] = new EndpointParam(parameter.getType(), null);
                } else {
                    params[i] = new EndpointParam(parameter.getType(), pathParam.value());
                }
            }
            return params;
        }
    }
}
