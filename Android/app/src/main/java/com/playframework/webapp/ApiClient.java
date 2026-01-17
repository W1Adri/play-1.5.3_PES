package com.playframework.webapp;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

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
import java.util.Map;

public class ApiClient {
    private static final CookieManager COOKIE_MANAGER = new CookieManager();

    static {
        CookieHandler.setDefault(COOKIE_MANAGER);
    }

    private final String baseUrl;

    public ApiClient(Context context) {
        this.baseUrl = context.getString(R.string.api_base_url);
    }

    public JSONObject get(String path, Map<String, String> params) throws IOException, JSONException {
        String url = buildUrl(path, params);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", "application/json");
        return readJson(connection);
    }

    public JSONObject post(String path, Map<String, String> params) throws IOException, JSONException {
        String url = buildUrl(path, null);
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setDoOutput(true);
        if (params != null && !params.isEmpty()) {
            String body = buildQuery(params);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body.getBytes("UTF-8"));
            }
        }
        return readJson(connection);
    }

    private String buildUrl(String path, Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append(baseUrl);
        if (!path.startsWith("/")) {
            sb.append("/");
        }
        sb.append(path);
        if (params != null && !params.isEmpty()) {
            sb.append("?").append(buildQuery(params));
        }
        return sb.toString();
    }

    private String buildQuery(Map<String, String> params) throws IOException {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                sb.append("&");
            }
            first = false;
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return sb.toString();
    }

    private JSONObject readJson(HttpURLConnection connection) throws IOException, JSONException {
        int status = connection.getResponseCode();
        InputStream stream = status >= 200 && status < 300
            ? connection.getInputStream()
            : connection.getErrorStream();
        String payload = readStream(stream);
        Object parsed = new JSONTokener(payload).nextValue();
        if (parsed instanceof JSONObject) {
            JSONObject object = (JSONObject) parsed;
            object.put("_status", status);
            return object;
        }
        JSONObject wrapper = new JSONObject();
        wrapper.put("_status", status);
        wrapper.put("data", parsed instanceof JSONArray ? parsed : payload);
        return wrapper;
    }

    private String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
