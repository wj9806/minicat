package com.minicat.core;

import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletContextAttributeEventObject;
import com.minicat.core.event.ServletContextEventObject;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

public class InternalContext {
    private final List<EventListener> listeners = new ArrayList<>();

    public <T extends EventListener> void addListener(T t) {
        if (t instanceof ServletRequestAttributeListener || 
            t instanceof ServletContextAttributeListener ||
            t instanceof ServletRequestListener) {
            listeners.add(t);
        }
    }

    public void publishEvent(EventObject event) {
        if (listeners.isEmpty()) {
            return;
        }

        if (event instanceof ServletContextAttributeEventObject) {
            ServletContextAttributeEventObject attributeEvent = (ServletContextAttributeEventObject) event;
            EventType eventType = attributeEvent.getEventType();

            for (EventListener listener : listeners) {
                if (listener instanceof ServletContextAttributeListener) {
                    ServletContextAttributeListener contextListener = (ServletContextAttributeListener) listener;
                    switch (eventType) {
                        case ATTRIBUTE_ADDED:
                            contextListener.attributeAdded(attributeEvent);
                            break;
                        case ATTRIBUTE_REMOVED:
                            contextListener.attributeRemoved(attributeEvent);
                            break;
                        case ATTRIBUTE_REPLACED:
                            contextListener.attributeReplaced(attributeEvent);
                            break;
                    }
                }
            }
        } else if (event instanceof ServletContextEventObject) {
            ServletContextEventObject requestEvent = (ServletContextEventObject) event;
            EventType eventType = requestEvent.getEventType();

            for (EventListener listener : listeners) {
                if (listener instanceof ServletRequestListener) {
                    ServletRequestListener requestListener = (ServletRequestListener) listener;
                    switch (eventType) {
                        case REQUEST_DESTROYED:
                            requestListener.requestDestroyed(requestEvent);
                            break;
                        case REQUEST_INITIALIZED:
                            requestListener.requestInitialized(requestEvent);
                            break;
                    }
                }
            }
        }
    }
}
