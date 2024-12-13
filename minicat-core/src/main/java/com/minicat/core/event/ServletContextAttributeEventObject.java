package com.minicat.core.event;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;

/**
 * ServletContext属性变更事件对象
 */
public class ServletContextAttributeEventObject extends ServletContextAttributeEvent {
    private final EventType eventType;

    public ServletContextAttributeEventObject(ServletContext source, String name, Object value, EventType eventType) {
        super(source, name, value);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
