package com.minicat.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NioTest {
    private static final int PORT = 8080;
    private static final int BUFFER_SIZE = 1024;
    private static final int THREAD_POOL_SIZE = 10;

    private Selector selector;
    private ExecutorService executorService;
    private ServerSocketChannel serverChannel;
    private volatile boolean running = true;

    public static void main(String[] args) {
        NioTest server = new NioTest();
        server.start();
    }

    public void start() {
        try {
            // 创建Selector
            selector = Selector.open();

            // 创建ServerSocketChannel
            serverChannel = ServerSocketChannel.open();
            serverChannel.configureBlocking(false);
            serverChannel.socket().bind(new InetSocketAddress(PORT));
            
            // 注册到Selector
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            // 创建线程池
            executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

            System.out.println("Server started on port " + PORT);

            while (running) {
                // 等待事件，设置超时时间为100ms，以便及时响应停止信号
                if (selector.select(100) == 0) {
                    continue;
                }

                // 处理所有就绪的Channel
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
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            destroy();
        }
    }

    public void stop() {
        System.out.println("Stopping server...");
        running = false;
        selector.wakeup(); // 唤醒selector以响应停止信号
    }

    public void destroy() {
        System.out.println("Destroying server resources...");
        try {
            // 关闭所有客户端连接
            if (selector != null && selector.isOpen()) {
                for (SelectionKey key : selector.keys()) {
                    try {
                        key.channel().close();
                    } catch (IOException e) {
                        System.err.println("Error closing channel: " + e.getMessage());
                    }
                }
                selector.close();
            }

            // 关闭服务器Socket
            if (serverChannel != null && serverChannel.isOpen()) {
                serverChannel.close();
            }

            // 关闭线程池
            if (executorService != null) {
                executorService.shutdown();
                try {
                    // 等待所有任务完成
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        executorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    executorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        } catch (IOException e) {
            System.err.println("Error during server shutdown: " + e.getMessage());
        }
        System.out.println("Server shutdown completed");
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        clientChannel.configureBlocking(false);
        
        // 注册读事件
        clientChannel.register(selector, SelectionKey.OP_READ);
        System.out.println("New connection accepted from: " + clientChannel.getRemoteAddress());

        System.out.println("---" + System.identityHashCode(clientChannel));
    }

    private void handleRead(SelectionKey key) {
        // 检查key是否已经有处理中的任务
        if (key.attachment() != null) {
            return;
        }
        
        // 标记该key正在处理中
        key.attach(Boolean.TRUE);
        
        // 提交读取任务到线程池
        executorService.submit(new ReadHandler(key));
    }

    private class ReadHandler implements Runnable {
        private final SelectionKey key;

        public ReadHandler(SelectionKey key) {
            this.key = key;
        }

        @Override
        public void run() {
            System.out.println("run");
            SocketChannel clientChannel = (SocketChannel) key.channel();
            System.out.println("---" + System.identityHashCode(clientChannel));

            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);

            try {
                // 检查channel和key的有效性
                if (!clientChannel.isOpen() || !key.isValid()) {
                    closeConnection(clientChannel);
                    return;
                }

                // 循环读取所有可用数据
                while (clientChannel.isOpen() && key.isValid()) {
                    buffer.clear();
                    int bytesRead = clientChannel.read(buffer);

                    if (bytesRead == -1) {
                        // 客户端关闭连接
                        closeConnection(clientChannel);
                        return;
                    }

                    if (bytesRead > 0) {
                        // 处理读取到的数据
                        buffer.flip();
                        byte[] data = new byte[buffer.remaining()];
                        buffer.get(data);
                        String message = new String(data);
                        
                        System.out.println("Received from " + clientChannel.getRemoteAddress() + ": " + message.trim());

                        // 检查channel是否仍然打开
                        if (clientChannel.isOpen()) {
                            // 发送响应
                            String response = "Server received: " + message;
                            ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes());
                            clientChannel.write(responseBuffer);
                        }
                    } else {
                        // 没有更多数据可读，退出循环
                        break;
                    }
                }
            } catch (IOException e) {
                if (!(e instanceof ClosedChannelException)) {
                    System.err.println("IO Error: " + e.getMessage());
                    e.printStackTrace();
                }
                closeConnection(clientChannel);
            } finally {
                // 清除处理中标记，允许下次读取
                key.attach(null);
                
                // 如果channel还是打开的，重新注册读事件
                if (clientChannel.isOpen() && key.isValid()) {
                    try {
                        key.interestOps(SelectionKey.OP_READ);
                    } catch (CancelledKeyException e) {
                        // 忽略已取消的key
                    }
                }
            }
        }

        private void closeConnection(SocketChannel channel) {
            try {
                if (channel.isOpen()) {
                    System.out.println("Connection closed: " + channel.getRemoteAddress());
                    key.cancel();
                    channel.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}
