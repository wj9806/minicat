package com.minicat.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.IOException;
import java.io.InputStream;

public class Config {

    private static final Logger logger = LoggerFactory.getLogger(Config.class);

    private static volatile Config conf;

    public static Config getInstance() {
        if (Config.conf == null) {
            synchronized (Config.class) {
                if (conf == null) {
                    Representer representer = new Representer();
                    representer.getPropertyUtils().setSkipMissingProperties(true);

                    Yaml yaml = new Yaml(new Constructor(Config.class), representer);
                    try (InputStream inputStream =
                                 Config.class.getClassLoader().getResourceAsStream("minicat.yaml")) {
                        if (inputStream != null) {
                            Config.conf = yaml.load(inputStream);
                        }
                    } catch (IOException e) {
                        logger.error("read minicat.yaml error: {}, use default config", e.getMessage());
                        Config.conf = new Config();
                    }
                }
            }
        }
        return Config.conf;
    }

    private ServerConfig server = new ServerConfig();

    private HttpConfig http = new HttpConfig();

    public HttpConfig getHttp() {
        return http;
    }

    public void setHttp(HttpConfig http) {
        this.http = http;
    }

    public ServerConfig getServer() {
        return server;
    }

    public void setServer(ServerConfig server) {
        this.server = server;
    }
}
