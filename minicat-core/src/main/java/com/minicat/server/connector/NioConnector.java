package com.minicat.server.connector;

import com.minicat.core.ApplicationContext;
import com.minicat.net.Sock;
import com.minicat.server.config.ServerConfig;
import com.minicat.server.processor.NioProcessor;
import com.minicat.server.thread.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class NioConnector implements ServerConnector<SelectionKey> {

    private static final Logger logger = LoggerFactory.getLogger(NioConnector.class);
    private final ServerConfig config;
    private final ApplicationContext applicationContext;
    private final Worker worker;
    private Selector selector;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = false;
    private final NioAcceptor acceptor;
    private final Set<Sock<SelectionKey>> socks;

    public NioConnector(Worker worker, ApplicationContext applicationContext, ServerConfig config) {
        this.worker = worker;
        this.applicationContext = applicationContext;
        this.config = config;
        this.acceptor = new NioAcceptor();
        this.socks = new CopyOnWriteArraySet<>();
    }

    @Override
    public String getName() {
        return "NioConnector";
    }

    @Override
    public Set<Sock<SelectionKey>> getSocks() {
        return socks;
    }

    @Override
    public void init() throws Exception {
        try {
            selector = Selector.open();
            // 创建ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
        } catch (IOException e) {
            logger.error("Failed to initialize {}", getName(), e);
            throw e;
        }
    }

    @Override
    public void start() throws Exception {
        running = true;
        serverChannel.socket().bind(new InetSocketAddress(config.getPort()));
        // 注册到Selector
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        acceptor.start();
    }

    @Override
    public void stop() throws Exception {
        running = false;
        // 唤醒selector以响应停止信号
        selector.wakeup();
    }

    @Override
    public void destroy() throws Exception {
        acceptor.interrupt();
        // 关闭所有客户端连接
        if (selector != null && selector.isOpen()) {
            for (SelectionKey key : selector.keys()) {
                try {
                    key.channel().close();
                } catch (IOException e) {
                    logger.error("Error closing channel: {}", e.getMessage());
                }
            }
            selector.close();
        }

        // 关闭服务器Socket
        if (serverChannel != null && serverChannel.isOpen()) {
            serverChannel.close();
        }
    }

    class NioAcceptor extends Thread {
        public NioAcceptor() {
            super("Acceptor");
        }

        @Override
        public void run() {
            while (running) {
                try {
                    if (selector.select(100) == 0) {
                        continue;
                    }

                    Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                    while (keyIterator.hasNext() && running) {
                        SelectionKey key = keyIterator.next();
                        keyIterator.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        if (key.isAcceptable()) {
                            handleAccept(key);
                        } else if (key.isReadable()) {
                            handleRead(key);
                        }
                    }

                } catch (IOException e) {
                    if (running) {
                        logger.error("Error accepting connection", e);
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private void handleAccept(SelectionKey key) throws IOException {
            ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
            SocketChannel clientChannel = serverChannel.accept();
            clientChannel.configureBlocking(false);

            // 注册读事件
            clientChannel.register(selector, SelectionKey.OP_READ);
        }

        private Sock<SelectionKey> fromKey(SelectionKey key) {
            if (socks.isEmpty()) return Sock.from(key);
            for (Sock<SelectionKey> sock : socks) {
                SelectionKey k = sock.source();
                if (k.equals(key)) return sock;
            }
            return Sock.from(key);
        }

        private void handleRead(SelectionKey key) {
            // 检查key是否已经有处理中的任务
            if (key.attachment() != null) {
                return;
            }

            // 标记该key正在处理中
            key.attach(Boolean.TRUE);

            Runnable task = () -> {
                try (NioProcessor processor = new NioProcessor(applicationContext, fromKey(key))) {
                    int process = processor.process();
                    if (process == -1) {
                        processor.destroy();
                    } else {
                        socks.add(processor.sock());
                    }
                } catch (Exception e) {
                    logger.error("Error processing request", e);
                } finally {
                    // 清除处理中标记，允许下次读取
                    key.attach(null);

                    // 如果channel还是打开的，重新注册读事件
                    if (key.channel().isOpen() && key.isValid()) {
                        try {
                            key.interestOps(SelectionKey.OP_READ);
                        } catch (CancelledKeyException e) {
                            // 忽略已取消的key
                        }
                    }
                }
            };

            if (config.isWorkerEnabled() && worker != null) {
                worker.execute(task);
            } else {
                task.run();
            }
        }
    }
}
