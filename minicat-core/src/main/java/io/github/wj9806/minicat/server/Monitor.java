package io.github.wj9806.minicat.server;

import io.github.wj9806.minicat.net.Sock;
import io.github.wj9806.minicat.server.config.Config;
import io.github.wj9806.minicat.server.connector.ServerConnector;
import io.github.wj9806.minicat.ws.processor.WsProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Monitor implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Monitor.class);

    private final List<ServerConnector<?>> connectors;

    public Monitor(List<ServerConnector<?>> connectors) {
        this.connectors = connectors;
    }

    @Override
    public void run() {
        for (ServerConnector<?> connector : connectors) {
            Collection<? extends Sock<?>> socks = connector.getSocks();
            List<? extends Sock<?>> removed = socks.stream()
                    .filter(this::timeout)
                    .collect(Collectors.toList());
            socks.removeAll(removed);
            for (Sock<?> sock : removed) {
                try {
                    sock.close();
                } catch (Exception e) {
                    logger.error("remove sock failed", e);
                }
            }
        }
    }

    /**
     * processor是否超时
     */
    private boolean timeout(Sock<?> sock) {
        if (sock.wsProcessor() != null) {
            WsProcessor<?> wsProcessor = sock.wsProcessor();
            return wsProcessor.idleTimeout();
        }

        if (Config.getInstance().getHttp().getKeepAliveTime() == -1)
            return false;
        return System.currentTimeMillis() - sock.getLastProcess()
                > 1000L * Config.getInstance().getHttp().getKeepAliveTime();
    }

}
