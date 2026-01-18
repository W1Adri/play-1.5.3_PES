package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        EditText edtUser = findViewById(R.id.edtUser);
        EditText edtPass = findViewById(R.id.edtPass);
        Button btnLogin = findViewById(R.id.btnLogin);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnPing = findViewById(R.id.btnPing);
        TextView txt = findViewById(R.id.txtResult);

        String prefillUsername = getIntent().getStringExtra("prefillUsername");
        if (prefillUsername != null && !prefillUsername.isEmpty()) {
            edtUser.setText(prefillUsername);
        }

        btnRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        btnPing.setOnClickListener(v -> {
            txt.setText("Comprobando servidor...");
            new Thread(() -> {
                try {
                    ApiClient.ApiResponse response = ApiClient.get("/api/ping");
                    JSONObject json = ApiClient.parseJson(response.body);
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> txt.setText("HTTP " + response.code + "\n" + msg));
                } catch (Exception e) {
                    runOnUiThread(() -> txt.setText("ERROR: " + e.getMessage()));
                }
            }).start();
        });

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
                    Map<String, String> params = new HashMap<>();
                    params.put("username", user);
                    params.put("password", pass);
                    ApiClient.ApiResponse response = ApiClient.postForm("/api/login", params);
                    JSONObject json = ApiClient.parseJson(response.body);

                    if (response.code >= 200 && response.code < 300 && json != null) {
                        String status = json.optString("status", "error");
                        if (!"ok".equalsIgnoreCase(status)) {
                            String msg = json.optString("msg", "Error");
                            runOnUiThread(() -> txt.setText("HTTP " + response.code + "\n" + msg));
                            return;
                        }

                        JSONObject userObj = json.getJSONObject("user");
                        String userId = userObj.optString("id", "");
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

                            intent.putExtra("userId", userId);
                            intent.putExtra("username", username);
                            intent.putExtra("fullName", fullName);
                            intent.putExtra("rol", rol);

                            startActivity(intent);
                        });

                    } else {
                        runOnUiThread(() -> txt.setText("HTTP " + response.code + "\n" + response.body));
                    }

                } catch (Exception e) {
                    runOnUiThread(() -> txt.setText("ERROR: " + e.getMessage()));
                }
            }).start();
        });
    }
}
