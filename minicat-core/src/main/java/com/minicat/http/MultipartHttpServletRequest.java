package com.minicat.http;

import com.minicat.core.Lifecycle;

import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;

/**
 * HttpServletRequest的包装类，用于处理multipart/form-data请求
 */
public class MultipartHttpServletRequest extends HttpServletRequestWrapper implements Lifecycle {

    // multipart/form-data
    private Map<String, Part> parts;
    private boolean multipartResolved;
    private long totalSize;

    private final ApplicationRequest request;

    private MultipartConfigElement multipartConfig;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @throws IllegalArgumentException if the request is null
     */
    public MultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
        this.request = (ApplicationRequest) request;
        this.totalSize = 0;
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        if (!multipartResolved) {
            parseMultipart();
        }
        return parts.values();
    }

    @Override
    public Part getPart(String name) throws IOException, ServletException {
        if (!multipartResolved) {
            parseMultipart();
        }
        return parts.get(name);
    }

    @Override
    public String getParameter(String name) {
        if (!multipartResolved) {
            parseMultipart();
        }
        return super.getParameter(name);
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        if (!multipartResolved) {
            parseMultipart();
        }
        return super.getParameterMap();
    }

    @Override
    public Enumeration<String> getParameterNames() {
        if (!multipartResolved) {
            parseMultipart();
        }
        return super.getParameterNames();
    }

    @Override
    public String[] getParameterValues(String name) {
        if (!multipartResolved) {
            parseMultipart();
        }
        return super.getParameterValues(name);
    }

    private void parseMultipart() {
        String boundary = "--" + buildBoundary();

        ByteArrayInputStream bis = new ByteArrayInputStream(request.body);
        ByteArrayOutputStream currentPartData = new ByteArrayOutputStream();
        Map<String, String> currentPartHeaders = new HashMap<>();
        String currentPartName = null;
        String currentFileName = null;

        // 读取状态
        int state = 0; // 0:寻找boundary, 1:读取headers, 2:读取内容
        ByteArrayOutputStream lineBuffer = new ByteArrayOutputStream();

        int b;
        boolean inLineEnding = false;
        int rnum = 0;
        try {
            while ((b = bis.read()) != -1) {
                if (b == '\r') {
                    inLineEnding = true;
                    if (state == 2)
                        rnum++;
                    continue;
                }

                if (b == '\n' && inLineEnding) {
                    inLineEnding = false;
                    byte[] lineBytes = lineBuffer.toByteArray();
                    String line = new String(lineBytes, StandardCharsets.US_ASCII);
                    lineBuffer.reset();

                    switch (state) {
                        case 0: // 寻找boundary
                            if (line.equals(boundary) || line.equals(boundary + "--")) {
                                // 保存前一个part（如果存在）
                                if (currentPartName != null && currentPartData.size() > 0) {
                                    // 移除末尾的\r\n
                                    byte[] data = currentPartData.toByteArray();
                                    int dataLength = data.length;
                                    if (dataLength >= 2 && data[dataLength - 2] == '\r' && data[dataLength - 1] == '\n') {
                                        dataLength -= 2;
                                    }
                                    partCheck(currentFileName, dataLength);
                                    addPart(currentPartName, currentFileName, currentPartHeaders,
                                            Arrays.copyOfRange(data, 0, dataLength));
                                }
                                // 重置状态
                                currentPartData = new ByteArrayOutputStream();
                                currentPartHeaders.clear();
                                currentPartName = null;
                                currentFileName = null;
                                state = 1; // 切换到读取headers状态
                            } else {
                                currentPartData.write(lineBytes);
                                currentPartData.write('\r');
                                currentPartData.write('\n');
                            }
                            break;

                        case 1: // 读取headers
                            if (line.isEmpty()) {
                                state = 2; // headers结束，切换到读取内容状态
                                rnum = 0;
                            } else {
                                // 解析header
                                parsePartHeader(line, currentPartHeaders);
                                if (line.toLowerCase().startsWith("content-disposition:")) {
                                    // 解析name和filename
                                    String[] parts = line.split(";");
                                    for (String part : parts) {
                                        part = part.trim();
                                        if (part.startsWith("name=")) {
                                            currentPartName = part.substring(5).trim().replace("\"", "");
                                        } else if (part.startsWith("filename=")) {
                                            currentFileName = part.substring(9).trim().replace("\"", "");
                                        }
                                    }
                                }
                            }
                            break;

                        case 2: // 读取内容
                            // 检查是否是boundary
                            if (!line.startsWith(boundary)) {
                                currentPartData.write(lineBytes);
                                for (int i = 0; i < rnum; i++) {
                                    currentPartData.write('\r');
                                }
                                rnum = 0;
                                currentPartData.write('\n');
                            } else {
                                state = 1; // 找到新的boundary，切换到读取headers状态
                                // 保存当前part
                                if (currentPartName != null && currentPartData.size() > 0) {
                                    // 移除末尾的\r\n
                                    byte[] data = currentPartData.toByteArray();
                                    int dataLength = data.length;
                                    if (dataLength >= 2 && data[dataLength - 2] == '\r' && data[dataLength - 1] == '\n') {
                                        dataLength -= 2;
                                    }
                                    partCheck(currentFileName, dataLength);
                                    addPart(currentPartName, currentFileName, currentPartHeaders,
                                            Arrays.copyOfRange(data, 0, dataLength));
                                }
                                // 重置状态
                                currentPartData = new ByteArrayOutputStream();
                                currentPartHeaders.clear();
                                currentPartName = null;
                                currentFileName = null;
                            }
                            break;
                    }
                    continue;
                }

                if (inLineEnding) {
                    if (state == 2) {
                        for (int i = 0; i < rnum; i++) {
                            lineBuffer.write('\r');
                        }
                        rnum = 0;
                    } else {
                        lineBuffer.write('\r');
                    }
                    inLineEnding = false;
                }
                lineBuffer.write(b);
            }
        } catch (IOException e) {
            throw new RequestParseException(e);
        }

        // 保存最后一个part（如果存在且不是最终boundary）
        if (currentPartName != null && currentPartData.size() > 0) {
            byte[] data = currentPartData.toByteArray();
            String lastLine = new String(data, StandardCharsets.US_ASCII);
            if (!lastLine.contains(boundary)) {
                partCheck(currentFileName, data.length);
                addPart(currentPartName, currentFileName, currentPartHeaders, data);
            }
        }

        this.multipartResolved = true;
    }

    private String buildBoundary() {
        String contentType = getContentType();

        // 获取boundary
        int boundaryIndex = contentType.indexOf("boundary=");
        if (boundaryIndex == -1) {
            throw new RequestParseException("No boundary found in multipart request");
        }

        String boundaryValue = contentType.substring(boundaryIndex + 9).trim();
        // 如果boundary被引号包围，去掉引号
        if (boundaryValue.startsWith("\"") && boundaryValue.endsWith("\"")) {
            boundaryValue = boundaryValue.substring(1, boundaryValue.length() - 1);
        }
        return boundaryValue;
    }

    private void parsePartHeader(String headerLine, Map<String, String> headers) {
        int colonIndex = headerLine.indexOf(':');
        String headerName = headerLine.substring(0, colonIndex).trim().toLowerCase();
        if (colonIndex > 0) {
            String headerValue = headerLine.substring(colonIndex + 1).trim();
            headers.put(headerName, headerValue);
        }
    }

    private void addPart(String name, String fileName, Map<String, String> headers, byte[] content) {
        Part part = new PartImpl(name, fileName, headers, content);
        if (parts == null)
            parts = new HashMap<>();
        parts.put(name, part);

        if (fileName == null) {
            // 如果没有fileName，说明是普通表单字段
            String value = new String(content, getCharacterEncoding() != null ?
                    Charset.forName(getCharacterEncoding()) : StandardCharsets.UTF_8);
            // 添加到parameters中
            String[] existingValues = request.parameters.get(name);
            if (existingValues == null) {
                request.parameters.put(name, new String[]{value});
            } else {
                String[] newValues = Arrays.copyOf(existingValues, existingValues.length + 1);
                newValues[existingValues.length] = value;
                request.parameters.put(name, newValues);
            }
        }
    }

    private void partCheck(String fileName, long dataLength) {
        if (this.multipartConfig == null) {
            this.multipartConfig = this.request.getServletRegistration().getMultipartConfig();
            if (this.multipartConfig == null)
                throw new NullPointerException("MultipartConfigElement is null");
        }

        // 检查文件大小限制
        if (fileName != null && multipartConfig.getMaxFileSize() > 0 && dataLength > multipartConfig.getMaxFileSize()) {
            throw new RequestParseException(String.format(
                "File size %d exceeds the maximum allowed size %d",
                    dataLength, multipartConfig.getMaxFileSize()
            ));
        }

        // 检查总请求大小限制
        totalSize += dataLength;
        if (multipartConfig.getMaxRequestSize() > 0 && totalSize > multipartConfig.getMaxRequestSize()) {
            throw new RequestParseException(String.format(
                "Total request size %d exceeds the maximum allowed size %d",
                totalSize, multipartConfig.getMaxRequestSize()
            ));
        }
    }

    @Override
    public void init() throws Exception {

    }

    @Override
    public void start() throws Exception {

    }

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void destroy() throws Exception {
        request.destroy();
        if (parts != null) {
            for (String key : parts.keySet()) {
                parts.get(key).delete();
            }
        }
        parts = null;
    }
}
