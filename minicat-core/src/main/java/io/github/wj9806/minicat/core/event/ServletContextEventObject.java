package io.github.wj9806.minicat.core.event;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

public class ServletContextEventObject extends ServletContextEvent {

    private final EventType eventType;

    /**
     * Construct a ServletContextEvent from the given context.
     *
     * @param source - the ServletContext that is sending the event.
     */
    public ServletContextEventObject(ServletContext source, EventType eventType) {
        super(source);
        this.eventType = eventType;
    }

    public EventType getEventType() {
        return eventType;
    }
}
