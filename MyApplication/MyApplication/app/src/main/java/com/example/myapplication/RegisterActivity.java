package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        EditText edtUser = findViewById(R.id.edtRegUser);
        EditText edtFullName = findViewById(R.id.edtRegFullName);
        EditText edtEmail = findViewById(R.id.edtRegEmail);
        EditText edtPass = findViewById(R.id.edtRegPass);
        Spinner spRole = findViewById(R.id.spRegRole);
        Button btnRegister = findViewById(R.id.btnRegister);
        Button btnBack = findViewById(R.id.btnBackLogin);
        TextView txtResult = findViewById(R.id.txtRegisterResult);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.roles_array,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRole.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnRegister.setOnClickListener(v -> {
            String username = edtUser.getText().toString().trim();
            String fullName = edtFullName.getText().toString().trim();
            String email = edtEmail.getText().toString().trim();
            String pass = edtPass.getText().toString();
            String role = spRole.getSelectedItem().toString().toLowerCase();

            if (username.isEmpty() || fullName.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                txtResult.setText("Rellena todos los campos.");
                return;
            }

            txtResult.setText("Registrando...");

            new Thread(() -> {
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("username", username);
                    params.put("password", pass);
                    params.put("email", email);
                    params.put("fullName", fullName);
                    params.put("rol", role);

                    ApiClient.ApiResponse response = ApiClient.postForm("/api/register", params);
                    JSONObject json = ApiClient.parseJson(response.body);

                    if (response.code >= 200 && response.code < 300 && json != null
                            && "ok".equalsIgnoreCase(json.optString("status"))) {
                        runOnUiThread(() -> {
                            txtResult.setText("Registro OK. Puedes iniciar sesión.");
                            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                            intent.putExtra("prefillUsername", username);
                            startActivity(intent);
                            finish();
                        });
                    } else {
                        String msg = json != null ? json.optString("msg", response.body) : response.body;
                        runOnUiThread(() -> txtResult.setText("HTTP " + response.code + "\n" + msg));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> txtResult.setText("ERROR: " + e.getMessage()));
                }
            }).start();
        });
    }
}
