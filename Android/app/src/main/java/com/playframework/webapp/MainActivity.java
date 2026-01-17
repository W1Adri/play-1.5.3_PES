package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private LinearLayout loginLayout;
    private LinearLayout registerLayout;
    private TextView messageView;

    private EditText loginUsername;
    private EditText loginPassword;

    private EditText registerUsername;
    private EditText registerPassword;
    private EditText registerEmail;
    private EditText registerFullName;
    private Spinner registerRole;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        loginLayout = findViewById(R.id.login_layout);
        registerLayout = findViewById(R.id.register_layout);
        messageView = findViewById(R.id.message_view);

        loginUsername = findViewById(R.id.login_username);
        loginPassword = findViewById(R.id.login_password);

        registerUsername = findViewById(R.id.register_username);
        registerPassword = findViewById(R.id.register_password);
        registerEmail = findViewById(R.id.register_email);
        registerFullName = findViewById(R.id.register_full_name);
        registerRole = findViewById(R.id.register_role);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[]{"Alumno", "Profesor"}
        );
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        registerRole.setAdapter(roleAdapter);

        Button loginButton = findViewById(R.id.login_button);
        Button registerButton = findViewById(R.id.register_button);
        TextView showRegister = findViewById(R.id.show_register);
        TextView showLogin = findViewById(R.id.show_login);

        loginButton.setOnClickListener(view -> handleLogin());
        registerButton.setOnClickListener(view -> handleRegister());
        showRegister.setOnClickListener(view -> showRegister());
        showLogin.setOnClickListener(view -> showLogin());
    }

    private void showRegister() {
        loginLayout.setVisibility(View.GONE);
        registerLayout.setVisibility(View.VISIBLE);
        clearMessage();
    }

    private void showLogin() {
        registerLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
        clearMessage();
    }

    private void handleLogin() {
        String username = loginUsername.getText().toString().trim();
        String password = loginPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showMessage("Introduce usuario y contraseña.", false);
            return;
        }

        setLoading(true);
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                JSONObject response = api.post("/api/login", params);
                JSONObject userJson = response.optJSONObject("user");
                if (userJson == null) {
                    runOnUiThread(() -> showMessage("Usuario o contraseña incorrectos.", false));
                    return;
                }
                User user = toUser(userJson);
                SessionStore.setCurrentUser(user);
                runOnUiThread(() -> {
                    showMessage("✅ Bienvenido, redirigiendo al panel...", true);
                    navigateToPanel(user);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("No se pudo conectar con el servidor.", false));
            } finally {
                runOnUiThread(() -> setLoading(false));
            }
        }).start();
    }

    private void handleRegister() {
        String username = registerUsername.getText().toString().trim();
        String password = registerPassword.getText().toString().trim();
        String email = registerEmail.getText().toString().trim();
        String fullName = registerFullName.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)
            || TextUtils.isEmpty(email) || TextUtils.isEmpty(fullName)) {
            showMessage("Completa todos los campos para registrarte.", false);
            return;
        }

        Role role = registerRole.getSelectedItemPosition() == 0 ? Role.ALUMNO : Role.PROFESOR;
        setLoading(true);
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                Map<String, String> params = new HashMap<>();
                params.put("username", username);
                params.put("password", password);
                params.put("email", email);
                params.put("fullName", fullName);
                params.put("rol", role == Role.ALUMNO ? "alumno" : "profesor");
                JSONObject response = api.post("/api/register", params);
                JSONObject userJson = response.optJSONObject("user");
                if (userJson == null) {
                    runOnUiThread(() -> showMessage("No se pudo registrar el usuario.", false));
                    return;
                }
                User user = toUser(userJson);
                SessionStore.setCurrentUser(user);
                runOnUiThread(() -> {
                    showMessage("✅ Registro completado. Bienvenido/a!", true);
                    navigateToPanel(user);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("No se pudo conectar con el servidor.", false));
            } finally {
                runOnUiThread(() -> setLoading(false));
            }
        }).start();
    }

    private void navigateToPanel(User user) {
        Intent intent;
        if (user.getRole() == Role.ALUMNO) {
            intent = new Intent(this, PanelAlumnoActivity.class);
        } else {
            intent = new Intent(this, PanelProfesorActivity.class);
        }
        startActivity(intent);
    }

    private void showMessage(String message, boolean success) {
        messageView.setVisibility(View.VISIBLE);
        messageView.setText(message);
        messageView.setBackgroundResource(success ? R.drawable.bg_message_success : R.drawable.bg_message_error);
    }

    private void clearMessage() {
        messageView.setVisibility(View.GONE);
        messageView.setText("");
    }

    private void setLoading(boolean loading) {
        findViewById(R.id.login_button).setEnabled(!loading);
        findViewById(R.id.register_button).setEnabled(!loading);
    }

    private User toUser(JSONObject json) {
        int id = json.optInt("id");
        String username = json.optString("username");
        String email = json.optString("email");
        String fullName = json.optString("fullName");
        String rol = json.optString("rol");
        Role role = "profesor".equalsIgnoreCase(rol) ? Role.PROFESOR : Role.ALUMNO;
        return new User(id, username, email, fullName, role);
    }
}
