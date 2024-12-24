package com.minicat.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class BioTest {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(8080);
        while (true) {
            Socket socket = serverSocket.accept();

            new Thread(() -> {
                try {
                    byte[] bytes = new byte[1];
                    InputStream inputStream = socket.getInputStream();

                    while (inputStream.read(bytes) != -1) {
                        System.out.println(new String(bytes));
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(bytes);
                        outputStream.flush();
                    }
                    System.out.println("shutdown");
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            },"hello").start();

            //outputStream.close();
            //inputStream.close();
        }
    }

}
