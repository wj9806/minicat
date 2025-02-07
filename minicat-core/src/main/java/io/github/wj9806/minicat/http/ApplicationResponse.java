package io.github.wj9806.minicat.http;

import io.github.wj9806.minicat.core.ApplicationContext;
import io.github.wj9806.minicat.io.ResponseBufferWriter;
import io.github.wj9806.minicat.io.ResponseOutputStream;
import io.github.wj9806.minicat.server.config.Config;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

public class ApplicationResponse implements HttpServletResponse {

    //socket 输出流
    private final OutputStream socketStream;

    //响应体缓冲
    private ByteBuffer bodyBuffer;
    private int bufferSize = 8192; // 默认8KB缓冲区

    private PrintWriter writer;
    private ServletOutputStream servletOutputStream;
    private boolean initial = false;

    private Charset charset = StandardCharsets.ISO_8859_1;
    private Locale locale = Locale.getDefault();
    private String contentType = "text/html";
    private int status = SC_OK;
    private final HttpHeaders headers = new HttpHeaders();
    private boolean committed = false;
    private final ApplicationContext context;

    public ApplicationResponse(ApplicationContext applicationContext, OutputStream socketStream) {
        this.socketStream = socketStream;
        this.bodyBuffer = ByteBuffer.allocate(bufferSize);
        this.context = applicationContext;
    }

    private void checkCommitted() {
        if (committed) {
            throw new IllegalStateException("Response already committed");
        }
    }

    private void writeResponse() throws IOException {
        if (committed) return;

        sendHeader();

        //do flush
        if (writer != null)
            writer.flush();
        else if (servletOutputStream != null)
            servletOutputStream.flush();
        else
            socketStream.flush();

        committed = true;
    }

    public void sendHeader() throws IOException {
        if (initial) return;

        // Add standard headers
        addStandardHeaders();

        // 使用ByteArrayOutputStream构建完整的响应
        ByteArrayOutputStream fullResponse = buildResponse();

        // Write everything at once
        socketStream.write(fullResponse.toByteArray());

        initial = true;
    }

    private ByteArrayOutputStream buildResponse() throws IOException {
        ByteArrayOutputStream fullResponse = new ByteArrayOutputStream();

        // Write status line
        fullResponse.write(String.format("HTTP/1.1 %d %s\r\n",
            status, getStatusMessage(status)).getBytes());

        // Write headers
        Map<String, List<String>> headerMap = headers.getAll();
        for (Map.Entry<String, List<String>> entry : headerMap.entrySet()) {
            String headerName = formatHeaderName(entry.getKey());
            List<String> values = entry.getValue();

            if (HttpHeaders.isMultiValueHeader(entry.getKey())) {
                // 合并多值响应头
                fullResponse.write(String.format("%s: %s\r\n",
                    headerName, String.join(", ", values)).getBytes());
            } else {
                // 分别写入每个值
                for (String value : values) {
                    fullResponse.write(String.format("%s: %s\r\n",
                        headerName, value).getBytes());
                }
            }
        }

        // End headers
        fullResponse.write("\r\n".getBytes());

        return fullResponse;
    }

    /**
     * 格式化响应头名称，保证首字母大写
     */
    private String formatHeaderName(String name) {
        String[] parts = name.split("-");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) {
                result.append('-');
            }
            String part = parts[i];
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)));
                if (part.length() > 1) {
                    result.append(part.substring(1).toLowerCase());
                }
            }
        }
        return result.toString();
    }

    private String getStatusMessage(int status) {
        switch (status) {
            case SC_OK: return "OK";
            case SC_SWITCHING_PROTOCOLS: return "Switching Protocols";
            case SC_NOT_FOUND: return "Not Found";
            case SC_INTERNAL_SERVER_ERROR: return "Internal Server Error";
            case SC_FOUND: return "Found";
            case SC_NOT_MODIFIED: return "Not Modified";
            default: return "Unknown";
        }
    }

    @Override
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new ResponseBufferWriter(this));
        }
        return writer;
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (servletOutputStream == null)
            this.servletOutputStream = new ResponseOutputStream(this);
        return servletOutputStream;
    }

    @Override
    public void flushBuffer() throws IOException {
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
        headers.set(name, value);
    }

    @Override
    public void addHeader(String name, String value) {
        checkCommitted();
        headers.add(name, value);
    }

    @Override
    public void setContentType(String type) {
        checkCommitted();
        this.contentType = type;
    }

    @Override
    public void setCharacterEncoding(String charset) {
        checkCommitted();
        this.charset = Charset.forName(charset);
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
        return headers.contains(name);
    }

    @Override
    public String getHeader(String name) {
        return headers.getFirst(name);
    }

    @Override
    public Collection<String> getHeaders(String name) {
        List<String> values = headers.get(name);
        return values != null ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    @Override
    public Collection<String> getHeaderNames() {
        return headers.names();
    }

    @Override
    public void resetBuffer() {
        checkCommitted();
        bodyBuffer.clear();
        if (writer != null) {
            writer = null;
        }
    }

    @Override
    public void reset() {
        checkCommitted();
        headers.clear();
        status = SC_OK;
        bodyBuffer.clear();
        if (writer != null) {
            writer = null;
        }
    }

    @Override
    public String getCharacterEncoding() {
        return charset.name();
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public boolean isCommitted() {
        return committed;
    }

    @Override
    public void setBufferSize(int size) {
        checkCommitted();
        if (bodyBuffer.position() > 0) {
            throw new IllegalStateException("Cannot set buffer size after data has been written");
        }
        this.bufferSize = size;
        this.bodyBuffer = ByteBuffer.allocate(this.bufferSize);
        // 重置输出流
        this.writer = null;
        this.servletOutputStream = null;
    }

    @Override
    public void addCookie(Cookie cookie) {
        if (cookie == null) {
            return;
        }
        StringBuilder cookieStr = new StringBuilder();
        cookieStr.append(cookie.getName()).append("=").append(cookie.getValue());
        
        if (cookie.getMaxAge() >= 0) {
            cookieStr.append("; Max-Age=").append(cookie.getMaxAge());
        }
        if (cookie.getPath() != null) {
            cookieStr.append("; Path=").append(cookie.getPath());
        }
        if (cookie.getDomain() != null) {
            cookieStr.append("; Domain=").append(cookie.getDomain());
        }
        if (cookie.getSecure()) {
            cookieStr.append("; Secure");
        }
        if (cookie.isHttpOnly()) {
            cookieStr.append("; HttpOnly");
        }
        
        addHeader("Set-Cookie", cookieStr.toString());
    }

    @Override
    public void setLocale(Locale loc) {
        if (loc == null) {
            return;
        }
        this.locale = loc;
        String language = loc.getLanguage();
        if (!language.isEmpty()) {
            String country = loc.getCountry();
            String value = language + (country.isEmpty() ? "" : "-" + country);
            setHeader("Content-Language", value);
        }
    }

    @Override
    public Locale getLocale() {
        return this.locale;
    }

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

    /**
     * 添加标准HTTP响应头
     */
    private void addStandardHeaders() {
        // Add Date header
        if (!headers.contains("date")) {
            headers.set("Date", formatDate(System.currentTimeMillis()));
        }

        if (headers.contains(HttpHeaders.UPGRADE)) {
            return;
        }

        // Add Connection and Keep-Alive headers
        if (!headers.contains("connection")) {
            headers.set("Connection", "keep-alive");
            if (!headers.contains("keep-alive")) {
                int keepAliveTime = Config.getInstance().getHttp().getKeepAliveTime();
                headers.set("Keep-Alive", "timeout=" + keepAliveTime);
            }
        }

        // Set Content-Length if not already set
        String te = getHeader("Transfer-Encoding");
        boolean chunked = "chunked".equalsIgnoreCase(te);
        if (!headers.contains("content-length") && !chunked) {
            setContentLength(bodyBuffer.position());
        }

        // Set Content-Type if not already set
        if (contentType != null && !headers.contains("content-type")) {
            String contentTypeValue = contentType;
            contentTypeValue += "; charset=" + charset.name().toLowerCase();
            headers.set("Content-Type", contentTypeValue);
        }
    }

    /**
     * 格式化HTTP日期（RFC 1123格式）
     * @param timestamp 时间戳
     * @return 格式化后的日期字符串
     */
    private String formatDate(long timestamp) {
        // HTTP日期格式：Sun, 06 Nov 1994 08:49:37 GMT
        SimpleDateFormat sdf = new SimpleDateFormat(
            "EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(new Date(timestamp));
    }

    public Charset getCharset() {
        return charset;
    }

    public OutputStream getSocketStream() {
        return socketStream;
    }

    public ByteBuffer getBodyBuffer() {
        return bodyBuffer;
    }
}
