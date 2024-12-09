package com.minicat.http;

import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class PartImpl implements Part {

    private final String name;
    private final String fileName;
    private final Map<String, String> headers;
    private final byte[] content;

    public PartImpl(String name, String fileName, Map<String, String> headers, byte[] content) {
        this.name = name;
        this.fileName = fileName;
        this.headers = headers != null ? new HashMap<>(headers) : null;
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
        // TODO
    }

    @Override
    public void delete() throws IOException {
        // TODO
    }

    @Override
    public String getHeader(String name) {
        return headers.get(name);
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
