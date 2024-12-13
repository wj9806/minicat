package com.minicat.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WebappClassLoader extends URLClassLoader {

    public WebappClassLoader(ClassLoader classLoader) {
        super(new URL[0], classLoader);
    }

    @Override
    public URL[] getURLs() {
        List<URL> urls = new ArrayList<>();
        urls.addAll(Arrays.asList(super.getURLs()));
        ClassLoader loader = Thread.currentThread().getContextClassLoader().getParent();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                urls.addAll(Arrays.asList(((URLClassLoader)loader).getURLs()));
            }
            loader = loader.getParent();
        }

        return urls.toArray(new URL[0]);
    }

    public Class<?> loadClass(String name, InputStream stream) throws IOException {
        // 将 InputStream 转换为字节数组
        byte[] classBytes = readBytes(stream);

        // 调用 defineClass 方法将字节码定义为 Class 对象
        return defineClass(name, classBytes, 0, classBytes.length);
    }

    private byte[] readBytes(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            return outputStream.toByteArray();
        }
    }
}
