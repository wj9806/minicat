package com.minicat.ws;

import javax.websocket.Extension;
import java.util.ArrayList;
import java.util.List;

public class WsExtension implements Extension {

    private final String name;
    private final List<Parameter> parameters = new ArrayList<>();

    WsExtension(String name) {
        this.name = name;
    }

    void addParameter(Parameter parameter) {
        parameters.add(parameter);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }
}
