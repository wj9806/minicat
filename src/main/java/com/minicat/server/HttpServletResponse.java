package com.minicat.server;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import java.io.*;
import java.util.*;

public class HttpServletResponse implements javax.servlet.http.HttpServletResponse {
    private PrintWriter writer;
    private OutputStream outputStream;
    private String characterEncoding = "UTF-8";
    private String contentType = "text/html";
    private int status = SC_OK;
    private Map<String, List<String>> headers = new HashMap<>();
    private boolean committed = false;
    private ByteArrayOutputStream bodyBuffer;
    private ServletContext servletContext;

    public HttpServletResponse(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.bodyBuffer = new ByteArrayOutputStream();
    }

    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    private void checkCommitted() {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
    }

    private void writeResponse() throws IOException {
        if (committed) return;

        // Write status line
        String statusLine = "HTTP/1.1 " + status + " " + getStatusMessage(status) + "\r\n";
        outputStream.write(statusLine.getBytes());
        
        // Set Content-Length if not already set
        if (!headers.containsKey("Content-Length")) {
            setContentLength(bodyBuffer.size());
        }

        // Write headers
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            for (String value : entry.getValue()) {
                String headerLine = entry.getKey() + ": " + value + "\r\n";
                outputStream.write(headerLine.getBytes());
            }
        }
        
        // Write content type
        if (contentType != null) {
            String contentTypeLine = "Content-Type: " + contentType;
            if (characterEncoding != null) {
                contentTypeLine += "; charset=" + characterEncoding;
            }
            outputStream.write((contentTypeLine + "\r\n").getBytes());
        }
        
        // End headers
        outputStream.write("\r\n".getBytes());
        
        // Write body
        if (bodyBuffer.size() > 0) {
            bodyBuffer.writeTo(outputStream);
        }
        
        outputStream.flush();
        committed = true;
    }

    private String getStatusMessage(int status) {
        switch (status) {
            case SC_OK: return "OK";
            case SC_NOT_FOUND: return "Not Found";
            case SC_INTERNAL_SERVER_ERROR: return "Internal Server Error";
            case SC_FOUND: return "Found";
            default: return "Unknown";
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(bodyBuffer, characterEncoding), true);
        }
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            @Override
            public void write(int b) throws IOException {
                bodyBuffer.write(b);
            }

            @Override
            public void flush() throws IOException {
                // Do nothing - we'll flush in writeResponse
            }

            @Override
            public void close() throws IOException {
                // Do nothing - we'll close in writeResponse
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(javax.servlet.WriteListener writeListener) {
                throw new UnsupportedOperationException("Write listeners are not supported");
            }
        };
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        writeResponse();
    }

    @Override
    public void setStatus(int sc) {
        checkCommitted();
        this.status = sc;
    }

    @Override
    public void setHeader(String name, String value) {
        checkCommitted();
        headers.put(name, Collections.singletonList(value));
    }

    @Override
    public void addHeader(String name, String value) {
        checkCommitted();
        headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
    }

    @Override
    public void setContentType(String type) {
        checkCommitted();
        this.contentType = type;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        checkCommitted();
        this.characterEncoding = charset;
    }

    @Override
    public void sendError(int sc, String msg) throws IOException {
        setStatus(sc);
        if (msg != null) {
            getWriter().write(msg);
        }
        flushBuffer();
    }

    @Override
    public void sendError(int sc) throws IOException {
        sendError(sc, null);
    }

    @Override
    public void sendRedirect(String location) throws IOException {
        setStatus(SC_FOUND);
        setHeader("Location", location);
        flushBuffer();
    }

    @Override
    public void setContentLength(int len) {
        setHeader("Content-Length", String.valueOf(len));
    }

    @Override
    public void setContentLengthLong(long length) {
        setHeader("Content-Length", String.valueOf(length));
    }

    @Override
    public boolean containsHeader(String name) {
        return headers.containsKey(name);
    }

    @Override
    public String getHeader(String name) {
        List<String> values = headers.get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    @Override
    public Collection<String> getHeaders(String name) {
        return headers.getOrDefault(name, Collections.emptyList());
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }

    @Override
    public void resetBuffer() {
        checkCommitted();
        bodyBuffer.reset();
        if (writer != null) {
            writer = null;
        }
    }

    @Override
    public void reset() {
        checkCommitted();
        headers.clear();
        status = SC_OK;
        bodyBuffer.reset();
        if (writer != null) {
            writer = null;
        }
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getBufferSize() {
        return bodyBuffer.size();
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    // 以下方法暂不需要实现
    @Override
    public void setBufferSize(int size) { }

    @Override
    public void setLocale(Locale loc) { }

    @Override
    public Locale getLocale() {
        return Locale.getDefault();
    }

    @Override
    public void addCookie(Cookie cookie) { }

    @Override
    public String encodeURL(String url) { return url; }

    @Override
    public String encodeRedirectURL(String url) { return url; }

    @Override
    public String encodeUrl(String url) { return url; }

    @Override
    public String encodeRedirectUrl(String url) { return url; }

    @Override
    public void setDateHeader(String name, long date) {
        setHeader(name, new Date(date).toString());
    }

    @Override
    public void addDateHeader(String name, long date) {
        addHeader(name, new Date(date).toString());
    }

    @Override
    public void setIntHeader(String name, int value) {
        setHeader(name, String.valueOf(value));
    }

    @Override
    public void addIntHeader(String name, int value) {
        addHeader(name, String.valueOf(value));
    }

    @Override
    public void setStatus(int sc, String sm) {
        setStatus(sc);
    }

    @Override
    public int getStatus() {
        return status;
    }
}
