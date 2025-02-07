package io.github.wj9806.minicat.ws;

import javax.websocket.HandshakeResponse;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.ServerEndpointConfig;

public class DefaultServerEndpointConfigurator extends ServerEndpointConfig.Configurator {

    public boolean checkOrigin(String originHeaderValue) {
        return true;
    }

    public void modifyHandshake(ServerEndpointConfig sec, HandshakeRequest request, HandshakeResponse response) {
        // nothing.
    }

    public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
        try {
            return endpointClass.getConstructor().newInstance();
        } catch (Exception e) {
            throw new InstantiationException(e.getMessage());
        }
    }

}
