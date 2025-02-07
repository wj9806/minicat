package io.github.wj9806.minicat.ws.message;

import io.github.wj9806.minicat.ws.WsSession;

public class TextMessageHandlerWhole extends BaseMessageHandlerWhole<String> {

    public TextMessageHandlerWhole(WsSession session) {
        super(session);
    }

    public boolean canHandle(Object msg) {
        if (msg instanceof String) {
            return true;
        }
        return false;
    }

    @Override
    public String convert(Object msg) {
        return (String) msg;
    }
}
