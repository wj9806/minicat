package com.minicat.core.event;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public class HttpSessionEventObject extends HttpSessionEvent {
    private final EventType eventType;

    public HttpSessionEventObject(HttpSession session, EventType eventType) {
        super(session);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }

}
