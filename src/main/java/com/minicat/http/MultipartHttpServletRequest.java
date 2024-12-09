package com.minicat.http;

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
public class MultipartHttpServletRequest extends HttpServletRequestWrapper {

    // multipart/form-data
    private Map<String, Part> parts;
    private String boundary;
    private boolean multipartResolved;
    private final com.minicat.http.HttpServletRequest request;
    private javax.servlet.MultipartConfigElement multipartConfig;
    private long totalSize;

    /**
     * Constructs a request object wrapping the given request.
     *
     * @param request
     * @throws IllegalArgumentException if the request is null
     */
    public MultipartHttpServletRequest(HttpServletRequest request) {
        super(request);
        this.request = (com.minicat.http.HttpServletRequest) request;
        // 设置默认的multipart配置
        this.multipartConfig = new javax.servlet.MultipartConfigElement(
            System.getProperty("java.io.tmpdir"),  // 临时文件目录
            -1L,  // maxFileSize
            -1L,  // maxRequestSize
            0     // fileSizeThreshold
        );
        this.totalSize = 0;
    }

    public void setMultipartConfig(javax.servlet.MultipartConfigElement multipartConfig) {
        this.multipartConfig = multipartConfig;
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
        this.boundary = "--" + buildBoundary();

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
        try {
            while ((b = bis.read()) != -1) {
                if (b == '\r') {
                    inLineEnding = true;
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
                                currentPartData.write('\r');
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
                    lineBuffer.write('\r');
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
        if (colonIndex > 0) {
            String headerName = headerLine.substring(0, colonIndex).trim().toLowerCase();
            String headerValue = headerLine.substring(colonIndex + 1).trim();
            headers.put(headerName, headerValue);
        }
    }

    private void addPart(String name, String fileName, Map<String, String> headers, byte[] content) {
        // 检查文件大小限制
        if (fileName != null && multipartConfig.getMaxFileSize() > 0 && content.length > multipartConfig.getMaxFileSize()) {
            throw new RequestParseException(String.format(
                "File size %d exceeds the maximum allowed size %d",
                content.length, multipartConfig.getMaxFileSize()
            ));
        }

        // 检查总请求大小限制
        totalSize += content.length;
        if (multipartConfig.getMaxRequestSize() > 0 && totalSize > multipartConfig.getMaxRequestSize()) {
            throw new RequestParseException(String.format(
                "Total request size %d exceeds the maximum allowed size %d",
                totalSize, multipartConfig.getMaxRequestSize()
            ));
        }

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
}
