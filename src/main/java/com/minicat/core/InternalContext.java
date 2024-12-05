package com.minicat.core;

import com.minicat.core.event.EventType;
import com.minicat.core.event.ServletContextAttributeEventObject;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestAttributeListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

public class InternalContext {
    private final List<EventListener> listeners = new ArrayList<>();

    public <T extends EventListener> void addListener(T t) {
        if (t instanceof ServletRequestAttributeListener || 
            t instanceof ServletContextAttributeListener) {
            listeners.add(t);
        }
    }

    public void publishEvent(EventObject event) {
        if (listeners.isEmpty() || !(event instanceof ServletContextAttributeEventObject)) {
            return;
        }

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
    }

    public List<ServletRequestAttributeListener> getRequestAttributeListeners() {
        List<ServletRequestAttributeListener> requestListeners = new ArrayList<>();
        for (EventListener listener : listeners) {
            if (listener instanceof ServletRequestAttributeListener) {
                requestListeners.add((ServletRequestAttributeListener) listener);
            }
        }
        return requestListeners;
    }
}
