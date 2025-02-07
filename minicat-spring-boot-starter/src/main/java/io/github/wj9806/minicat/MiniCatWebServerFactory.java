package io.github.wj9806.minicat;

import io.github.wj9806.minicat.core.ApplicationContext;
import io.github.wj9806.minicat.server.HttpServer;
import io.github.wj9806.minicat.server.config.Config;
import io.github.wj9806.minicat.server.config.ServerConfig;
import org.springframework.boot.web.server.*;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.*;

public class MiniCatWebServerFactory extends AbstractServletWebServerFactory {

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        ServerConfig conf = Config.getInstance().getServer();
        conf.setShowBanner(false);

        MiniCat miniCat = new MiniCat(getPort());
        HttpServer server = miniCat.getServer();
        ApplicationContext applicationContext = server.getApplicationContext();
        configureContext(initializers, applicationContext);
        return new MiniCatWebServer(miniCat);
    }

    private void configureContext(ServletContextInitializer[] initializers, ApplicationContext applicationContext) {
        ServletContextInitializer[] initializersToUse = mergeInitializers(initializers);
        MiniCatStarter miniCatStarter = new MiniCatStarter(initializersToUse);
        applicationContext.addServletContainerInitializer(miniCatStarter, null);
        for (String webListenerClassName : getWebListenerClassNames()) {
            applicationContext.addListener(webListenerClassName);
        }
    }

}
