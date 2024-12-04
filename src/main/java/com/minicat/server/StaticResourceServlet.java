package com.minicat.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPOutputStream;

public class StaticResourceServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(StaticResourceServlet.class);
    private static final int BUFFER_SIZE = 16 * 1024; // 16KB buffer
    private static final long CACHE_DURATION = 86400L; // 24 hours in seconds
    private static final Map<String, String> CONTENT_TYPES = new HashMap<>();
    private static final Map<String, CachedResource> resourceCache = new ConcurrentHashMap<>();
    private static final long MAX_CACHE_SIZE = 10 * 1024 * 1024; // 10MB
    private static long currentCacheSize = 0;
    private final String staticPath;
    
    static {
        CONTENT_TYPES.put(".html", "text/html");
        CONTENT_TYPES.put(".css", "text/css");
        CONTENT_TYPES.put(".js", "application/javascript");
        CONTENT_TYPES.put(".json", "application/json");
        CONTENT_TYPES.put(".png", "image/png");
        CONTENT_TYPES.put(".jpg", "image/jpeg");
        CONTENT_TYPES.put(".jpeg", "image/jpeg");
        CONTENT_TYPES.put(".gif", "image/gif");
        CONTENT_TYPES.put(".ico", "image/x-icon");
        CONTENT_TYPES.put(".svg", "image/svg+xml");
        CONTENT_TYPES.put(".txt", "text/plain");
    }
    
    public StaticResourceServlet(String staticPath) {
        this.staticPath = staticPath;
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        
        // 处理默认首页
        if (uri.equals("/") || uri.equals("")) {
            uri = "/index.html";
        }
        
        // 获取资源路径
        String resourcePath = staticPath + uri;
        logger.debug("Looking for static resource: {}", resourcePath);
        
        try {
            // 检查资源是否存在
            CachedResource cachedResource = resourceCache.get(resourcePath);
            if (cachedResource == null) {
                // 资源不在缓存中，尝试加载
                try (InputStream resourceStream = getClass().getResourceAsStream(resourcePath)) {
                    if (resourceStream == null) {
                        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + uri);
                        return;
                    }
                    cachedResource = loadAndCacheResource(resourcePath, resourceStream);
                }
            }
            
            // 检查If-None-Match头
            String clientEtag = req.getHeader("If-None-Match");
            if (clientEtag != null && clientEtag.equals(cachedResource.etag)) {
                resp.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                return;
            }
            
            // 设置Content-Type
            String contentType = getContentType(uri);
            resp.setContentType(contentType);
            if (contentType.startsWith("text/") || contentType.equals("application/javascript")) {
                resp.setCharacterEncoding("UTF-8");
            }
            
            // 设置缓存控制头
            resp.setHeader("Cache-Control", "public, max-age=" + CACHE_DURATION);
            resp.setHeader("ETag", cachedResource.etag);
            
            // 检查是否支持GZIP
            String acceptEncoding = req.getHeader("Accept-Encoding");
            boolean supportsGzip = acceptEncoding != null && acceptEncoding.contains("gzip");
            
            if (supportsGzip && cachedResource.gzippedContent != null) {
                // 发送GZIP压缩的内容
                resp.setHeader("Content-Encoding", "gzip");
                resp.setHeader("Content-Length", String.valueOf(cachedResource.gzippedContent.length));
                resp.getOutputStream().write(cachedResource.gzippedContent);
            } else {
                // 发送原始内容
                resp.setHeader("Content-Length", String.valueOf(cachedResource.content.length));
                resp.getOutputStream().write(cachedResource.content);
            }
            
        } catch (Exception e) {
            logger.error("Error serving static resource: {}", resourcePath, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, 
                    "Error reading resource: " + e.getMessage());
        }
    }
    
    private CachedResource loadAndCacheResource(String resourcePath, InputStream inputStream) 
            throws IOException {
        // 读取资源内容
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, bytesRead);
        }
        byte[] content = baos.toByteArray();
        
        // 如果缓存已满，清除一些旧条目
        while (currentCacheSize + content.length > MAX_CACHE_SIZE && !resourceCache.isEmpty()) {
            String oldestKey = resourceCache.keySet().iterator().next();
            CachedResource removed = resourceCache.remove(oldestKey);
            currentCacheSize -= removed.content.length;
            if (removed.gzippedContent != null) {
                currentCacheSize -= removed.gzippedContent.length;
            }
        }
        
        // 创建GZIP版本（仅对文本内容）
        String contentType = getContentType(resourcePath);
        byte[] gzippedContent = null;
        if (contentType.startsWith("text/") || 
            contentType.equals("application/javascript") || 
            contentType.equals("application/json")) {
            gzippedContent = gzip(content);
        }
        
        // 计算ETag
        String etag = calculateETag(content);
        
        // 创建并缓存资源
        CachedResource resource = new CachedResource(content, gzippedContent, etag);
        resourceCache.put(resourcePath, resource);
        currentCacheSize += content.length;
        if (gzippedContent != null) {
            currentCacheSize += gzippedContent.length;
        }
        
        return resource;
    }
    
    private byte[] gzip(byte[] input) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(input);
        }
        return baos.toByteArray();
    }
    
    private String calculateETag(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(content);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            // 如果MD5不可用，使用内容长度作为ETag
            return String.valueOf(content.length);
        }
    }
    
    private String getContentType(String uri) {
        int dotIndex = uri.lastIndexOf('.');
        if (dotIndex > 0) {
            String extension = uri.substring(dotIndex).toLowerCase();
            String contentType = CONTENT_TYPES.get(extension);
            if (contentType != null) {
                return contentType;
            }
        }
        return "application/octet-stream";
    }
    
    private static class CachedResource {
        final byte[] content;
        final byte[] gzippedContent;
        final String etag;
        
        CachedResource(byte[] content, byte[] gzippedContent, String etag) {
            this.content = content;
            this.gzippedContent = gzippedContent;
            this.etag = etag;
        }
    }
}
