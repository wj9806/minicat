package com.minicat.core.event;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;

public class HttpSessionIdEventObject extends HttpSessionEvent {
    private final String oldSessionId;
    private final String newSessionId;
    private final EventType eventType;

    public HttpSessionIdEventObject(HttpSession session, String oldSessionId, String newSessionId, EventType eventType) {
        super(session);
        this.oldSessionId = oldSessionId;
        this.newSessionId = newSessionId;
        this.eventType = eventType;
    }

    public String getOldSessionId() {
        return oldSessionId;
    }

    public String getNewSessionId() {
        return newSessionId;
    }

    public EventType getEventType() {
        return eventType;
    }
}
