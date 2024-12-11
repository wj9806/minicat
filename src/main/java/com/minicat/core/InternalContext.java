package com.minicat.core;

import com.minicat.core.event.*;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

public class InternalContext {

    private final List<ServletRequestAttributeListener> servletRequestAttributeListeners = new ArrayList<>();
    private final List<ServletContextAttributeListener> servletContextAttributeListeners = new ArrayList<>();
    private final List<ServletRequestListener> servletRequestListeners = new ArrayList<>();
    private final List<HttpSessionListener> httpSessionListeners = new ArrayList<>();
    private final List<HttpSessionAttributeListener> httpSessionAttributeListeners = new ArrayList<>();


    public <T extends EventListener> void addListener(T t) {
        if (t instanceof ServletRequestAttributeListener) {
            servletRequestAttributeListeners.add((ServletRequestAttributeListener)t);
        }

        if (t instanceof ServletContextAttributeListener) {
            servletContextAttributeListeners.add((ServletContextAttributeListener)t);
        }

        if (t instanceof ServletRequestListener) {
            servletRequestListeners.add((ServletRequestListener)t);
        }

        if (t instanceof HttpSessionListener) {
            httpSessionListeners.add((HttpSessionListener)t);
        }

        if (t instanceof HttpSessionAttributeListener) {
            httpSessionAttributeListeners.add((HttpSessionAttributeListener)t);
        }
    }

    public void publishEvent(EventObject event) {

        if (event instanceof ServletContextAttributeEventObject) {
            ServletContextAttributeEventObject e = (ServletContextAttributeEventObject) event;
            fireServletContextAttributeEvent(e);
        } else if (event instanceof ServletRequestEventObject) {
            ServletRequestEventObject e = (ServletRequestEventObject) event;
            fireServletRequestEvent(e);
        } else if (event instanceof HttpSessionEventObject) {
            HttpSessionEventObject e = (HttpSessionEventObject) event;
            fireHttpSessionEvent(e);
        } else if (event instanceof HttpSessionAttributeEventObject) {
            HttpSessionAttributeEventObject e = (HttpSessionAttributeEventObject) event;
            fireHttpSessionAttributeEvent(e);
        } else if (event instanceof ServletRequestAttributeEventObject) {
            ServletRequestAttributeEventObject e = (ServletRequestAttributeEventObject) event;
            fireServletRequestAttributeEvent(e);
        }
    }

    private void fireServletRequestAttributeEvent(ServletRequestAttributeEventObject event) {
        for (ServletRequestAttributeListener listener : servletRequestAttributeListeners) {
            switch (event.getEventType()) {
                case SERVLET_REQUEST_ATTRIBUTE_ADDED:
                    listener.attributeAdded(event);
                    break;
                case SERVLET_REQUEST_ATTRIBUTE_REMOVED:
                    listener.attributeRemoved(event);
                    break;
                case SERVLET_REQUEST_ATTRIBUTE_REPLACED:
                    listener.attributeReplaced(event);
                    break;
            }
        }
    }

    private void fireServletContextAttributeEvent(ServletContextAttributeEventObject event) {
        for (ServletContextAttributeListener listener : servletContextAttributeListeners) {
            switch (event.getEventType()) {
                case SERVLET_CONTEXT_ATTRIBUTE_ADDED:
                    listener.attributeAdded(event);
                    break;
                case SERVLET_CONTEXT_ATTRIBUTE_REMOVED:
                    listener.attributeRemoved(event);
                    break;
                case SERVLET_CONTEXT_ATTRIBUTE_REPLACED:
                    listener.attributeReplaced(event);
                    break;
            }
        }
    }

    private void fireServletRequestEvent(ServletRequestEventObject event) {
        for (ServletRequestListener listener : servletRequestListeners) {
            switch (event.getEventType()) {
                case SERVLET_REQUEST_DESTROYED:
                    listener.requestDestroyed(event);
                    break;
                case SERVLET_REQUEST_INITIALIZED:
                    listener.requestInitialized(event);
                    break;
            }
        }
    }

    private void fireHttpSessionEvent(HttpSessionEventObject event) {
        for (HttpSessionListener listener : httpSessionListeners) {
            switch (event.getEventType()) {
                case SESSION_CREATED:
                    listener.sessionCreated(event);
                    break;
                case SESSION_DESTROYED:
                    listener.sessionDestroyed(event);
                    break;
            }
        }
    }

    private void fireHttpSessionAttributeEvent(HttpSessionAttributeEventObject event) {
        for (HttpSessionAttributeListener listener : httpSessionAttributeListeners) {
            switch (event.getType()) {
                case SESSION_ATTRIBUTE_ADDED:
                    listener.attributeAdded(event);
                    break;
                case SESSION_ATTRIBUTE_REMOVED:
                    listener.attributeRemoved(event);
                    break;
                case SESSION_ATTRIBUTE_REPLACED:
                    listener.attributeReplaced(event);
                    break;
            }
        }
    }
}
