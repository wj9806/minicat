package com.minicat.util;

import java.util.HashMap;
import java.util.Map;

public class UriTemplate {

    private final String template;

    private final String contextPath;

    /**
     * @param template 模板字符串，例如 "/a/{b}/c"
     */
    public UriTemplate(String template, String contextPath) {
        this.template = template;
        this.contextPath = contextPath;
    }

    /**
     * 提取 URL 路径参数
     *
     *
     * @param actualUrl 实际 URL，例如 "/a/1/c"
     * @return 参数与值的映射，例如 {b=1}
     */
    public Map<String, String> extractParams(String actualUrl) {
        if (contextPath != null && !"/".equals(contextPath)) {
            actualUrl = actualUrl.substring(contextPath.length());
        }

        actualUrl = actualUrl.split("\\?")[0]; // 只取路径部分
        Map<String, String> params = new HashMap<>();
        String[] templateParts = template.split("/");
        String[] urlParts = actualUrl.split("/");

        if (templateParts.length != urlParts.length) {
            throw new IllegalArgumentException("Template and actual URL do not match in structure.");
        }

        for (int i = 0; i < templateParts.length; i++) {
            String templatePart = templateParts[i];
            String urlPart = urlParts[i];

            if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
                // 获取参数名（去掉大括号）
                String paramName = templatePart.substring(1, templatePart.length() - 1);
                params.put(paramName, urlPart);
            }
        }

        return params;
    }

    /**
     * 判断传入的 requestURI 是否与模板匹配
     *
     * @param requestURI 实际的 URI，例如 "/a/1/c"
     * @return 如果匹配返回 true，否则返回 false
     */
    public boolean canHandle(String requestURI) {
        if (contextPath != null && !"/".equals(contextPath)) {
            requestURI = requestURI.substring(contextPath.length());
        }

        String path = requestURI.split("\\?")[0]; // 只取路径部分
        String[] templateParts = template.split("/");
        String[] urlParts = path.split("/");

        if (templateParts.length != urlParts.length) {
            return false;
        }

        for (int i = 0; i < templateParts.length; i++) {
            String templatePart = templateParts[i];
            String urlPart = urlParts[i];

            if (templatePart.startsWith("{") && templatePart.endsWith("}")) {
                // 参数部分，总是匹配
                continue;
            } else if (!templatePart.equals(urlPart)) {
                // 静态路径部分不匹配
                return false;
            }
        }

        return true;
    }

    /**
     * 判断模板路径中是否包含模板参数
     */
    public boolean hasTemplateParams() {
        String[] templateParts = template.split("/");
        for (String part : templateParts) {
            if (part.startsWith("{") && part.endsWith("}")) {
                return true;
            }
        }
        return false;
    }
}
