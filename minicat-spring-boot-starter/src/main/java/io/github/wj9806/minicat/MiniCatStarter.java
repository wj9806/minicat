package io.github.wj9806.minicat;

import org.springframework.boot.web.servlet.ServletContextInitializer;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Set;

public class MiniCatStarter implements ServletContainerInitializer {

    private final ServletContextInitializer[] initializers;

    MiniCatStarter(ServletContextInitializer[] initializers) {
        this.initializers = initializers;
    }

    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) throws ServletException {
        for (ServletContextInitializer initializer : this.initializers) {
            initializer.onStartup(ctx);
        }
    }
}
