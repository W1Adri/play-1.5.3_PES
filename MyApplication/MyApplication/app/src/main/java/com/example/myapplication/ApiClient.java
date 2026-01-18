package com.example.myapplication;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class ApiClient {

    public static final String BASE_URL = "http://10.0.2.2:9000";
    private static final CookieManager COOKIE_MANAGER = new CookieManager();

    static {
        CookieHandler.setDefault(COOKIE_MANAGER);
    }

    private ApiClient() {
    }

    public static ApiResponse get(String path) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);

        int code = conn.getResponseCode();
        String body = readBody(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        return new ApiResponse(code, body);
    }

    public static ApiResponse postForm(String path, Map<String, String> params) throws IOException {
        URL url = new URL(BASE_URL + path);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

        String payload = encodeParams(params);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        String body = readBody(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());
        return new ApiResponse(code, body);
    }

    public static JSONObject parseJson(String body) {
        try {
            return new JSONObject(body);
        } catch (Exception e) {
            return null;
        }
    }

    private static String encodeParams(Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append('&');
            }
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"))
                .append('=')
                .append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return sb.toString();
    }

    private static String readBody(InputStream is) throws IOException {
        if (is == null) {
            return "";
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    public static final class ApiResponse {
        public final int code;
        public final String body;

        ApiResponse(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }
}
