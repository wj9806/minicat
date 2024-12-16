package com.minicat.http;

import com.minicat.core.ApplicationContext;
import com.minicat.core.event.EventType;
import com.minicat.core.event.HttpSessionAttributeEventObject;
import com.minicat.core.event.HttpSessionBindingEventObject;
import com.minicat.core.event.HttpSessionEventObject;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingListener;
import javax.servlet.http.HttpSessionContext;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ApplicationSession implements HttpSession {

    private String id;
    private final long creationTime;
    private long lastAccessedTime;
    private int maxInactiveInterval;
    private boolean isNew;
    private boolean isValid;
    private final ApplicationContext servletContext;
    private final Map<String, Object> attributes;


    public static String createId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public ApplicationSession(ApplicationContext servletContext) {
        this.id = createId();
        this.creationTime = System.currentTimeMillis();
        this.lastAccessedTime = this.creationTime;
        this.maxInactiveInterval = 1800;  // 默认30分钟
        this.isNew = true;
        this.isValid = true;
        this.servletContext = servletContext;

        this.attributes = new ConcurrentHashMap<>();

        // 触发session创建事件
        servletContext.publishEvent(new HttpSessionEventObject(this, EventType.SESSION_CREATED));
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public long getCreationTime() {
        checkValid();
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        checkValid();
        return lastAccessedTime;
    }

    @Override
    public ServletContext getServletContext() {
        return servletContext;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        this.maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        return null;
    }

    @Override
    public Object getAttribute(String name) {
        checkValid();
        return attributes.get(name);
    }

    @Override
    @Deprecated
    public Object getValue(String name) {
        return getAttribute(name);
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        checkValid();
        return new Enumeration<String>() {
            private final Iterator<String> iterator = attributes.keySet().iterator();

            @Override
            public boolean hasMoreElements() {
                return iterator.hasNext();
            }

            @Override
            public String nextElement() {
                return iterator.next();
            }
        };
    }

    @Override
    @Deprecated
    public String[] getValueNames() {
        checkValid();
        return attributes.keySet().toArray(new String[0]);
    }

    @Override
    public void setAttribute(String name, Object value) {
        checkValid();
        if (name == null) {
            throw new IllegalArgumentException("Session attribute name cannot be null");
        }
        
        if (value == null) {
            removeAttribute(name);
            return;
        }

        Object oldValue = attributes.put(name, value);

        // 触发属性变更事件
        if (oldValue == null) {
            servletContext.publishEvent(new HttpSessionAttributeEventObject(this, name, value, EventType.SESSION_ATTRIBUTE_ADDED));
        } else {
            servletContext.publishEvent(new HttpSessionAttributeEventObject(this, name, oldValue, EventType.SESSION_ATTRIBUTE_REPLACED));

            if (oldValue instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) oldValue;
                listener.valueUnbound(new HttpSessionBindingEventObject(this, name, oldValue, EventType.SESSION_VALUE_UNBOUND));
            }
        }

        if (value instanceof HttpSessionBindingListener) {
            HttpSessionBindingListener listener = (HttpSessionBindingListener) value;
            listener.valueBound(new HttpSessionBindingEventObject(this, name, value, EventType.SESSION_VALUE_BOUND));
        }
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) {
        setAttribute(name, value);
    }

    @Override
    public void removeAttribute(String name) {
        checkValid();
        Object oldValue = attributes.remove(name);
        if (oldValue != null) {
            // 触发属性移除事件
            servletContext.publishEvent(new HttpSessionAttributeEventObject(this, name, oldValue, EventType.SESSION_ATTRIBUTE_REMOVED));
            if (oldValue instanceof HttpSessionBindingListener) {
                HttpSessionBindingListener listener = (HttpSessionBindingListener) oldValue;
                listener.valueUnbound(new HttpSessionBindingEventObject(this, name, oldValue, EventType.SESSION_VALUE_UNBOUND));
            }
        }
    }

    @Override
    @Deprecated
    public void removeValue(String name) {
        removeAttribute(name);
    }

    @Override
    public void invalidate() {
        checkValid();
        // 触发session销毁事件
        servletContext.publishEvent(new HttpSessionEventObject(this, EventType.SESSION_DESTROYED));
        attributes.clear();
        isValid = false;
    }

    @Override
    public boolean isNew() {
        checkValid();
        return isNew;
    }

    public void setNew(boolean isNew) {
        this.isNew = isNew;
    }

    public void access() {
        this.lastAccessedTime = System.currentTimeMillis();
        this.isNew = false;
    }

    private void checkValid() {
        if (!isValid) {
            throw new IllegalStateException("Session has been invalidated");
        }
    }
}
