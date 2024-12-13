package com.minicat.http;

import com.minicat.core.ApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.*;

public class DefaultSessionManager implements SessionManager {
    private static final Logger log = LoggerFactory.getLogger(DefaultSessionManager.class);
    private final Map<String, ApplicationSession> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("SessionManager");
        thread.setDaemon(true);
        return thread;
    });

    @Override
    public String changeSessionId(String oldId) {
        ApplicationSession session = sessions.remove(oldId);
        if (session == null) {
            throw new IllegalStateException("Session [" + oldId + "] not found");
        }
        String newId = ApplicationSession.createId();
        session.setId(newId);
        sessions.put(newId, session);
        return newId;
    }

    @Override
    public HttpSession getSession(String sessionId, ApplicationContext servletContext, boolean create, HttpServletResponse servletResponse) {
        if (sessionId != null) {
            ApplicationSession session = sessions.get(sessionId);
            if (session != null) {
                if (!isExpired(session)) {
                    session.access();
                    return session;
                } else {
                    sessions.remove(sessionId);
                }
            }
        }

        if (!create) {
            return null;
        }

        ApplicationSession session = new ApplicationSession(servletContext);
        sessions.put(session.getId(), session);

        Cookie cookie = new Cookie("JSESSIONID", session.getId());
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        servletResponse.addCookie(cookie);
        return session;
    }

    private boolean isExpired(ApplicationSession session) {
        if (session.getMaxInactiveInterval() < 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        long lastAccessed = session.getLastAccessedTime();
        return (now - lastAccessed) / 1000 > session.getMaxInactiveInterval();
    }

    private void cleanExpiredSessions() {
        log.trace("SessionManager schedule cleanExpiredSessions");
        sessions.entrySet().removeIf(entry -> isExpired(entry.getValue()));
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void start() throws Exception {
        // 每分钟检查一次过期的session
        scheduler.scheduleAtFixedRate(this::cleanExpiredSessions, 0, 1, TimeUnit.MINUTES);
    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void destroy() throws Exception {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
