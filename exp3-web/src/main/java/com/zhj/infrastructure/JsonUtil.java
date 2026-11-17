package com.zhj.infrastructure;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class JsonUtil {

    private static final int MAX_BODY_LENGTH = 1024 * 1024; // 1MB，防止超大请求体打满内存

    private JsonUtil() {
    }

    /**
     * 从请求体中读取 JSON。请求体为空时返回空对象；
     * 格式不合法时抛出 IllegalArgumentException，由 BaseServlet 统一转换成 400。
     */
    public static JSONObject readRequest(HttpServletRequest req) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
                if (sb.length() > MAX_BODY_LENGTH) {
                    throw new IllegalArgumentException("请求体过大");
                }
            }
        }
        if (sb.length() == 0) {
            return new JSONObject();
        }
        try {
            return JSON.parseObject(sb.toString());
        } catch (Exception e) {
            throw new IllegalArgumentException("请求体不是合法的 JSON", e);
        }
    }

    public static void writeResponse(HttpServletResponse resp, int code, String message, Object data) throws IOException {
        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setContentType("application/json;charset=UTF-8");
        JSONObject result = new JSONObject();
        result.put("code", code);
        result.put("message", message);
        result.put("data", data);

        try (PrintWriter out = resp.getWriter()) {
            out.print(result.toJSONString());
            out.flush();
        }
    }

    public static void success(HttpServletResponse resp, Object data) throws IOException {
        writeResponse(resp, 200, "success", data);
    }

    public static void error(HttpServletResponse resp, int code, String message) throws IOException {
        writeResponse(resp, code, message, null);
    }
}
