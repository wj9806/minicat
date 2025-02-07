package io.github.wj9806.minicat.http;

import io.github.wj9806.minicat.core.ApplicationContext;
import io.github.wj9806.minicat.core.Lifecycle;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public interface SessionManager extends Lifecycle {

    HttpSession getSession(String sessionId, ApplicationContext servletContext,
                           boolean create, HttpServletResponse servletResponse);

    String changeSessionId(String oldId);
}
