package io.github.wj9806.minicat.core.test.util;

import java.util.Map;

public class JSON {

    @SuppressWarnings("unchecked")
    public static String toJSONString(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;

            json.append("\"").append(escapeJsonString(entry.getKey())).append("\":");
            Object value = entry.getValue();
            if (value instanceof String) {
                json.append("\"").append(escapeJsonString((String)value)).append("\"");
            } else if (value instanceof Number) {
                json.append(value);
            } else if (value instanceof Map) {
                json.append(toJSONString((Map<String, Object>) value));
            } else if (value == null) {
                json.append("null");
            } else {
                json.append("\"").append(escapeJsonString(value.toString())).append("\"");
            }
        }

        json.append("}");
        return json.toString();
    }

    private static String escapeJsonString(String input) {
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
