package com.minicat.core.event;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

public class HttpSessionAttributeEventObject extends HttpSessionBindingEvent {
    private final Object value;
    private final EventType eventType;

    public HttpSessionAttributeEventObject(HttpSession session, String name, Object value, EventType eventType) {
        super(session, name);
        this.value = value;
        this.eventType = eventType;
    }

    public Object getValue() {
        return value;
    }

    public EventType getEventType() {
        return eventType;
    }
}
