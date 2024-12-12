package com.minicat.http;

import com.minicat.core.ApplicationContext;
import com.minicat.core.Lifecycle;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public interface SessionManager extends Lifecycle {

    HttpSession getSession(String sessionId, ApplicationContext servletContext,
                           boolean create, HttpServletResponse servletResponse);

    String changeSessionId(String oldId);
}
