package com.minicat.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class BannerPrinter {
    private static final Logger logger = LoggerFactory.getLogger(BannerPrinter.class);

    // ANSI颜色代码
    public static class AnsiColor {
        public static final String RESET = "\u001B[0m";
        public static final String BRIGHT_BLUE = "\u001B[94m";
        public static final String BRIGHT_GREEN = "\u001B[92m";
        public static final String DEFAULT = RESET;
    }

    public static void printBanner() {
        try (InputStream inputStream = BannerPrinter.class.getResourceAsStream("/banner.txt")) {
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    String line;
                    StringBuilder banner = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        // 替换颜色占位符
                        line = line.replace("${AnsiColor.BRIGHT_BLUE}", AnsiColor.BRIGHT_BLUE)
                                 .replace("${AnsiColor.BRIGHT_GREEN}", AnsiColor.BRIGHT_GREEN)
                                 .replace("${AnsiColor.DEFAULT}", AnsiColor.DEFAULT);
                        banner.append(line).append("\n");
                    }
                    // 使用System.out直接打印，以保持颜色格式
                    System.out.print(banner.toString());
                }
            }
        } catch (IOException e) {
            logger.error("Error printing banner", e);
        }
    }
}
