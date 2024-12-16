package com.minicat.http;

import javax.servlet.http.Part;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PartImpl implements Part {

    private final String name;
    private final String fileName;
    private final Map<String, String> headers;
    private final byte[] content;
    private Path savedFilePath;

    public PartImpl(String name, String fileName, Map<String, String> headers, byte[] content) {
        this.name = name;
        this.fileName = fileName;
        this.headers = headers != null ? new HashMap<>(headers) : new HashMap<>();
        this.content = content;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSubmittedFileName() {
        return fileName;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public void write(String fileName) throws IOException {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be null or empty");
        }

        // 创建目标文件的Path对象
        Path targetPath = Paths.get(fileName);

        // 确保目标目录存在
        Files.createDirectories(targetPath.getParent());

        try (OutputStream outputStream = Files.newOutputStream(targetPath)) {
            outputStream.write(content);
        }

        // 保存文件路径以便后续删除
        this.savedFilePath = targetPath;
    }

    @Override
    public void delete() throws IOException {
        if (savedFilePath != null && Files.exists(savedFilePath)) {
            Files.delete(savedFilePath);
            savedFilePath = null;
        }
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name.toLowerCase());
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return Collections.singleton(headers.get(name));
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public String getContentType() {
        return getHeader("content-type");
    }
}
