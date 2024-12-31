package com.minicat.core;

import com.minicat.core.event.*;

import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionIdListener;
import javax.servlet.http.HttpSessionListener;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.EventObject;
import java.util.List;

public class InternalEventMulticast {

    private List<ServletContextListener> servletContextListeners;
    private List<ServletContextAttributeListener> servletContextAttributeListeners;
    private List<ServletRequestListener> servletRequestListeners;
    private List<ServletRequestAttributeListener> servletRequestAttributeListeners;
    private List<HttpSessionListener> httpSessionListeners;
    private List<HttpSessionAttributeListener> httpSessionAttributeListeners;
    private List<HttpSessionIdListener> httpSessionIdListeners;


    public <T extends EventListener> void addListener(T t) {
        if (t instanceof ServletContextListener) {
            if (servletContextListeners == null) servletContextListeners = new ArrayList<>();
            servletContextListeners.add((ServletContextListener)t);
        }

        if (t instanceof ServletRequestAttributeListener) {
            if (servletRequestAttributeListeners == null) servletRequestAttributeListeners = new ArrayList<>();
            servletRequestAttributeListeners.add((ServletRequestAttributeListener)t);
        }

        if (t instanceof ServletContextAttributeListener) {
            if (servletContextAttributeListeners == null) servletContextAttributeListeners = new ArrayList<>();
            servletContextAttributeListeners.add((ServletContextAttributeListener)t);
        }

        if (t instanceof ServletRequestListener) {
            if (servletRequestListeners == null) servletRequestListeners = new ArrayList<>();
            servletRequestListeners.add((ServletRequestListener)t);
        }

        if (t instanceof HttpSessionListener) {
            if (httpSessionListeners == null) httpSessionListeners = new ArrayList<>();
            httpSessionListeners.add((HttpSessionListener)t);
        }

        if (t instanceof HttpSessionAttributeListener) {
            if (httpSessionAttributeListeners == null) httpSessionAttributeListeners = new ArrayList<>();
            httpSessionAttributeListeners.add((HttpSessionAttributeListener)t);
        }

        if (t instanceof HttpSessionIdListener) {
            if (httpSessionIdListeners == null) httpSessionIdListeners = new ArrayList<>();
            httpSessionIdListeners.add((HttpSessionIdListener)t);
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
        } else if (event instanceof HttpSessionIdEventObject) {
            HttpSessionIdEventObject e = (HttpSessionIdEventObject) event;
            fireHttpSessionIdEvent(e);
        } else if (event instanceof ServletContextEventObject) {
            ServletContextEventObject e = (ServletContextEventObject)event;
            fireServletContextEvent(e);
        }
    }

    private void fireServletContextEvent(ServletContextEventObject event) {
        if (servletContextListeners == null) return;
        for (ServletContextListener listener : servletContextListeners) {
            switch (event.getEventType()) {
                case SERVLET_CONTEXT_DESTROYED:
                    listener.contextDestroyed(event);
                    break;
                case SERVLET_CONTEXT_INITIALIZED:
                    listener.contextInitialized(event);
                    break;
            }
        }
    }

    private void fireHttpSessionIdEvent(HttpSessionIdEventObject e) {
        if (httpSessionIdListeners == null) return;
        for (HttpSessionIdListener listener : httpSessionIdListeners) {
            listener.sessionIdChanged(e, e.getOldSessionId());
        }
    }

    private void fireServletRequestAttributeEvent(ServletRequestAttributeEventObject event) {
        if (servletRequestAttributeListeners == null) return;
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
        if (servletContextAttributeListeners == null) return;
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
        if (servletRequestListeners == null) return;
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
        if (httpSessionListeners == null) return;
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
        if (httpSessionAttributeListeners == null) return;
        for (HttpSessionAttributeListener listener : httpSessionAttributeListeners) {
            switch (event.getEventType()) {
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
