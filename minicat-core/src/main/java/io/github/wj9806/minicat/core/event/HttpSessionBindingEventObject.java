package io.github.wj9806.minicat.core.event;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;

public class HttpSessionBindingEventObject extends HttpSessionBindingEvent {

    private final EventType eventType;

    public HttpSessionBindingEventObject(HttpSession session, String name, EventType eventType) {
        super(session, name);
        this.eventType = eventType;
    }

    public HttpSessionBindingEventObject(HttpSession session, String name, Object value, EventType eventType) {
        super(session, name, value);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
