package com.minicat.ws;

import javax.websocket.HandshakeResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WsHandshakeResponse implements HandshakeResponse {

    private final Map<String, List<String>> headers = new HashMap<>();

    @Override
    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
