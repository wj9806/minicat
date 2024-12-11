package com.minicat.http;

import com.minicat.core.ApplicationContext;
import com.minicat.server.Lifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.Map;
import java.util.concurrent.*;

public class SessionManager implements Lifecycle {
    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);
    private final Map<String, ApplicationSession> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setName("SessionManager");
        thread.setDaemon(true);
        return thread;
    });

    public SessionManager() {

    }

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

        String cookieValue = "JSESSIONID=" + session.getId() + "; Path=/; SameSite=Strict; HttpOnly";
        servletResponse.addHeader("Set-Cookie", cookieValue);

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
