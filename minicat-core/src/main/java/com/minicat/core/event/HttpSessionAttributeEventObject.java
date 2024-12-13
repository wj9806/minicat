package com.minicat.core.event;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

public class HttpSessionAttributeEventObject extends HttpSessionBindingEvent {
    private final Object value;
    private final EventType type;

    public HttpSessionAttributeEventObject(HttpSession session, String name, Object value, EventType type) {
        super(session, name);
        this.value = value;
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public EventType getType() {
        return type;
    }
}
