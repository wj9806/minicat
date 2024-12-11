package com.minicat.core.event;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;

public class ServletRequestEventObject extends ServletRequestEvent {

    private final EventType eventType;

    /**
     * Construct a ServletContextEvent from the given context.
     *
     * @param source - the ServletContext that is sending the event.
     */
    public ServletRequestEventObject(ServletContext source, ServletRequest request, EventType eventType) {
        super(source, request);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
