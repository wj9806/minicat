package com.minicat.example.servlet;

import com.minicat.server.HttpServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.*;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "multipartServlet", urlPatterns = {"/file/upload"})
@MultipartConfig(
    location = "file",           // 上传文件的存储目录
    maxFileSize = 1024 * 1024 * 5,    // 单个文件最大5MB
    maxRequestSize = 1024 * 1024 * 10, // 整个请求最大10MB
    fileSizeThreshold = 0             // 超过该值时写入磁盘
)
public class MultipartServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(MultipartServlet.class);
    private static final String UPLOAD_DIR = "file";

    @Override
    public void init() throws ServletException {
        // 确保上传目录存在
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
        } catch (IOException e) {
            throw new ServletException("Could not create upload directory", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) 
            throws ServletException, IOException {

        System.out.println(req.getParameter("p1"));
        Collection<Part> parts = req.getParts();
        System.out.println(req.getContentType());
        for (Part part : parts) {
            System.out.println("---------------");
            System.out.println(part.getContentType());
            System.out.println(part.getName());
            System.out.println(part.getSubmittedFileName());
            System.out.println(part.getSize());
            System.out.println(part.getHeaderNames());
        }

        System.out.println("==================================");

        // 设置响应类型
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();
        try {
            // 获取文件部分
            Part filePart = req.getPart("file");
            if (filePart == null) {
                throw new ServletException("No file uploaded");
            }

            // 获取文件名并解码
            String fileName = filePart.getSubmittedFileName();
            if (fileName == null || fileName.trim().isEmpty()) {
                throw new ServletException("No filename specified");
            }
            
            // URL解码文件名，处理中文
            fileName = URLDecoder.decode(fileName, "UTF-8");
            
            // 移除文件名中的非法字符
            fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");

            // 构建保存路径
            Path filePath = Paths.get(UPLOAD_DIR, fileName);
            boolean fileExists = Files.exists(filePath);
            
            // 如果文件存在，先删除它
            if (fileExists) {
                try {
                    Files.delete(filePath);
                } catch (IOException e) {
                    logger.error("Failed to delete existing file: {}", filePath, e);
                    throw new ServletException("Could not overwrite existing file", e);
                }
            }
            
            // 保存新文件
            try (InputStream input = filePart.getInputStream();
                 OutputStream output = new FileOutputStream(filePath.toFile())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            // 获取其他表单字段
            String description = req.getParameter("description");

            // 构建成功响应
            result.put("status", "success");
            result.put("message", fileExists ? "File overwritten successfully" : "File uploaded successfully");
            result.put("fileName", fileName);
            result.put("description", description);
            result.put("size", filePart.getSize());
            result.put("path", filePath.toString());
            result.put("overwritten", fileExists);
            result.put("content-type", req.getContentType());

        } catch (Exception e) {
            logger.error("Error processing file upload", e);
            result.put("status", "error");
            result.put("message", e.getMessage());
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }

        // 发送响应
        PrintWriter writer = resp.getWriter();
        writer.write(toJson(result));
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            
            json.append("\"").append(entry.getKey()).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJsonString((String)value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJsonString(value.toString())).append("\"");
            }
        }
        
        json.append("}");
        return json.toString();
    }

    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            switch (ch) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (ch < ' ') {
                        String hex = String.format("\\u%04x", (int) ch);
                        result.append(hex);
                    } else {
                        result.append(ch);
                    }
                    break;
            }
        }
        return result.toString();
    }
}
