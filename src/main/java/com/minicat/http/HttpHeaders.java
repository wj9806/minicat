package com.minicat.http;

import java.util.*;

/**
 * HTTP请求/响应头处理类
 */
public class HttpHeaders {
    private final Map<String, List<String>> headers = new HashMap<>();

    /**
     * 添加请求头
     * @param name 请求头名称
     * @param value 请求头值
     */
    public void add(String name, String value) {
        String lowerName = name.toLowerCase();
        headers.computeIfAbsent(lowerName, k -> new ArrayList<>()).add(value);
    }

    /**
     * 设置请求头，会覆盖已有的值
     * @param name 请求头名称
     * @param value 请求头值
     */
    public void set(String name, String value) {
        String lowerName = name.toLowerCase();
        List<String> values = new ArrayList<>();
        values.add(value);
        headers.put(lowerName, values);
    }

    /**
     * 获取请求头的第一个值
     * @param name 请求头名称
     * @return 请求头值，如果不存在返回null
     */
    public String getFirst(String name) {
        List<String> values = get(name);
        return values != null && !values.isEmpty() ? values.get(0) : null;
    }

    /**
     * 获取请求头的所有值
     * @param name 请求头名称
     * @return 请求头值列表
     */
    public List<String> get(String name) {
        return headers.get(name.toLowerCase());
    }

    /**
     * 获取所有请求头名称
     * @return 请求头名称集合
     */
    public Set<String> names() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    /**
     * 获取所有请求头
     * @return 请求头Map的不可修改视图
     */
    public Map<String, List<String>> getAll() {
        return Collections.unmodifiableMap(headers);
    }

    /**
     * 检查是否包含指定请求头
     * @param name 请求头名称
     * @return 是否包含
     */
    public boolean contains(String name) {
        return headers.containsKey(name.toLowerCase());
    }

    /**
     * 移除请求头
     * @param name 请求头名称
     * @return 被移除的值列表
     */
    public List<String> remove(String name) {
        return headers.remove(name.toLowerCase());
    }

    /**
     * 清空所有请求头
     */
    public void clear() {
        headers.clear();
    }

    /**
     * 从HTTP请求行解析请求头
     * @param lines HTTP请求行数组
     * @return 包含解析后请求头的HttpHeaders对象
     */
    public static HttpHeaders parse(String[] lines) {
        HttpHeaders headers = new HttpHeaders();
        StringBuilder currentHeaderValue = new StringBuilder();
        String currentHeaderName = null;

        for (int i = 1; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                // 遇到空行，说明请求头部分结束
                if (currentHeaderName != null && currentHeaderValue.length() > 0) {
                    // 保存最后一个请求头
                    addHeaderValues(headers, currentHeaderName, currentHeaderValue.toString().trim());
                }
                break;
            }

            if (line.charAt(0) == ' ' || line.charAt(0) == '\t') {
                // 多行请求头的延续行
                if (currentHeaderName != null) {
                    currentHeaderValue.append(" ").append(line.trim());
                }
            } else {
                // 新的请求头
                if (currentHeaderName != null && currentHeaderValue.length() > 0) {
                    // 保存之前的请求头
                    addHeaderValues(headers, currentHeaderName, currentHeaderValue.toString().trim());
                }

                // 解析新的请求头
                int colonIndex = line.indexOf(':');
                if (colonIndex > 0) {
                    currentHeaderName = line.substring(0, colonIndex).trim();
                    currentHeaderValue = new StringBuilder(line.substring(colonIndex + 1).trim());
                }
            }
        }
        return headers;
    }

    /**
     * 处理请求头值，支持逗号分隔的多值
     * @param headers HttpHeaders对象
     * @param name 请求头名称
     * @param value 请求头值
     */
    private static void addHeaderValues(HttpHeaders headers, String name, String value) {
        // 检查是否是允许多值的请求头
        if (isMultiValueHeader(name)) {
            // 分割值并去除每个值的空白
            String[] values = value.split(",");
            for (String v : values) {
                headers.add(name, v.trim());
            }
        } else {
            // 不支持多值的请求头直接添加
            headers.add(name, value);
        }
    }

    /**
     * 检查请求头是否允许多个值
     * @param name 请求头名称
     * @return 是否允许多值
     */
    public static boolean isMultiValueHeader(String name) {
        String lowerName = name.toLowerCase();
        return lowerName.equals("accept") ||
               lowerName.equals("accept-language") ||
               lowerName.equals("accept-encoding") ||
               lowerName.equals("accept-charset") ||
               lowerName.equals("allow") ||
               lowerName.equals("cache-control") ||
               lowerName.equals("connection") ||
               lowerName.equals("content-encoding") ||
               lowerName.equals("content-language") ||
               lowerName.equals("if-match") ||
               lowerName.equals("if-none-match") ||
               lowerName.equals("pragma") ||
               lowerName.equals("prefer") ||
               lowerName.equals("te") ||
               lowerName.equals("trailer") ||
               lowerName.equals("transfer-encoding") ||
               lowerName.equals("upgrade") ||
               lowerName.equals("via") ||
               lowerName.equals("warning");
    }
}
