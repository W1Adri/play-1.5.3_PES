package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://10.0.2.2:9000";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText edtUser = findViewById(R.id.edtUser);
        EditText edtPass = findViewById(R.id.edtPass);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView txt = findViewById(R.id.txtResult);

        btnLogin.setOnClickListener(v -> {

            String user = edtUser.getText().toString().trim();
            String pass = edtPass.getText().toString();

            if (user.isEmpty() || pass.isEmpty()) {
                txt.setText("Rellena usuario y password.");
                return;
            }

            txt.setText("Consultando...");

            new Thread(() -> {
                try {
                    URL url = new URL(BASE_URL + "/api/login");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                    conn.setRequestMethod("POST");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

                    String postData =
                            "username=" + URLEncoder.encode(user, "UTF-8") +
                                    "&password=" + URLEncoder.encode(pass, "UTF-8");

                    conn.getOutputStream().write(postData.getBytes("UTF-8"));
                    conn.getOutputStream().close();

                    int code = conn.getResponseCode();

                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(
                                    (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream()
                            )
                    );

                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);
                    br.close();

                    String body = sb.toString();

                    if (code >= 200 && code < 300) {
                        JSONObject json = new JSONObject(body);

                        String status = json.optString("status", "error");
                        if (!"ok".equalsIgnoreCase(status)) {
                            String msg = json.optString("msg", "Error");
                            runOnUiThread(() -> txt.setText("HTTP " + code + "\n" + msg));
                            return;
                        }

                        JSONObject userObj = json.getJSONObject("user");
                        String username = userObj.optString("username", "");
                        String fullName = userObj.optString("fullName", "");
                        String rol = userObj.optString("rol", "");

                        runOnUiThread(() -> {
                            // (opcional) para ver que entra aquí
                            txt.setText("Login OK (" + rol + ")");

                            Intent intent;
                            if ("ALUMNO".equalsIgnoreCase(rol)) {
                                intent = new Intent(MainActivity.this, AlumnoActivity.class);
                            } else {
                                intent = new Intent(MainActivity.this, ProfesorActivity.class);
                            }

                            intent.putExtra("username", username);
                            intent.putExtra("fullName", fullName);
                            intent.putExtra("rol", rol);

                            startActivity(intent);
                        });

                    } else {
                        runOnUiThread(() -> txt.setText("HTTP " + code + "\n" + body));
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> txt.setText("ERROR: " + e.getMessage()));
                }
            }).start();
        });
    }
}
