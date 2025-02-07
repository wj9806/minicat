package io.github.wj9806.minicat.core.event;

import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;

public class ServletRequestAttributeEventObject extends ServletRequestAttributeEvent {

    private final EventType eventType;

    public ServletRequestAttributeEventObject(ServletContext sc, ServletRequest request,
                                              String name, Object value, EventType eventType) {
        super(sc, request, name, value);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
