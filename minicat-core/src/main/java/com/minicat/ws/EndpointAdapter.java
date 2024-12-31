package com.minicat.ws;

import javax.websocket.*;

public class EndpointAdapter extends Endpoint {

    private final EndpointHandler handler;

    private final Object endpoint;

    public EndpointAdapter(EndpointHandler handler, Object endpoint) {
        this.handler = handler;
        this.endpoint = endpoint;
    }

    @Override
    public void onOpen(Session session, EndpointConfig config) {
        EndpointMetadata endpointMethod = handler.getEndpointMethod();
        endpointMethod.onOpen(endpoint, session, config);
    }

    public void onClose(Session session, CloseReason closeReason) {
    }

    public void onError(Session session, Throwable thr) {
    }
}
