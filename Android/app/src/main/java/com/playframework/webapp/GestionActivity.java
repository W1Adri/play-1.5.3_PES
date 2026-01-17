package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GestionActivity extends Activity {

    private User currentUser;
    private LinearLayout usersList;
    private LinearLayout materiasList;
    private TextView flashMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_gestion);

        usersList = findViewById(R.id.gestion_users_list);
        materiasList = findViewById(R.id.gestion_materias_list);
        flashMessage = findViewById(R.id.gestion_flash);

        Button backButton = findViewById(R.id.gestion_back_button);
        Button logoutButton = findViewById(R.id.gestion_logout_button);
        backButton.setOnClickListener(view -> finish());
        logoutButton.setOnClickListener(view -> logout());

        setupAddUser();
        setupAddMateria();

        loadGestion();
    }

    private void setupAddUser() {
        EditText usernameInput = findViewById(R.id.add_user_username);
        EditText passwordInput = findViewById(R.id.add_user_password);
        EditText emailInput = findViewById(R.id.add_user_email);
        EditText fullNameInput = findViewById(R.id.add_user_full_name);
        Spinner roleSpinner = findViewById(R.id.add_user_role);

        ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[]{"Alumno", "Profesor"}
        );
        roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(roleAdapter);

        Button addButton = findViewById(R.id.add_user_button);
        addButton.setOnClickListener(view -> {
            String username = usernameInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String email = emailInput.getText().toString().trim();
            String fullName = fullNameInput.getText().toString().trim();
            if (username.isEmpty() || password.isEmpty() || email.isEmpty() || fullName.isEmpty()) {
                showFlash("Completa todos los campos para crear un usuario.", false);
                return;
            }
            Role role = roleSpinner.getSelectedItemPosition() == 0 ? Role.ALUMNO : Role.PROFESOR;
            new Thread(() -> {
                try {
                    ApiClient api = new ApiClient(this);
                    Map<String, String> params = new HashMap<>();
                    params.put("username", username);
                    params.put("password", password);
                    params.put("email", email);
                    params.put("fullName", fullName);
                    params.put("rol", role == Role.ALUMNO ? "alumno" : "profesor");
                    JSONObject response = api.post("/api/gestion/usuarios", params);
                    runOnUiThread(() -> {
                        if (response.optString("error", "").isEmpty()) {
                            usernameInput.setText("");
                            passwordInput.setText("");
                            emailInput.setText("");
                            fullNameInput.setText("");
                            showFlash("✅ Usuario creado.", true);
                            loadGestion();
                        } else {
                            showFlash(response.optString("error", "No se pudo crear el usuario."), false);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                }
            }).start();
        });
    }

    private void setupAddMateria() {
        EditText nameInput = findViewById(R.id.add_materia_name);
        EditText descriptionInput = findViewById(R.id.add_materia_description);
        Button addButton = findViewById(R.id.add_materia_button);

        addButton.setOnClickListener(view -> {
            String name = nameInput.getText().toString().trim();
            String description = descriptionInput.getText().toString().trim();
            if (name.isEmpty() || description.isEmpty()) {
                showFlash("Completa nombre y descripción para crear la materia.", false);
                return;
            }
            new Thread(() -> {
                try {
                    ApiClient api = new ApiClient(this);
                    Map<String, String> params = new HashMap<>();
                    params.put("codigo", "MAT-" + System.currentTimeMillis());
                    params.put("nombre", name);
                    params.put("descripcion", description);
                    JSONObject response = api.post("/api/gestion/materias", params);
                    runOnUiThread(() -> {
                        if (response.optString("error", "").isEmpty()) {
                            nameInput.setText("");
                            descriptionInput.setText("");
                            showFlash("✅ Materia creada.", true);
                            loadGestion();
                        } else {
                            showFlash(response.optString("error", "No se pudo crear la materia."), false);
                        }
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                }
            }).start();
        });
    }

    private void renderUsers(JSONArray users) {
        usersList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (users == null) {
            return;
        }
        for (int i = 0; i < users.length(); i++) {
            JSONObject user = users.optJSONObject(i);
            if (user == null) continue;
            View row = inflater.inflate(R.layout.item_user_row, usersList, false);
            EditText username = row.findViewById(R.id.user_row_username);
            EditText email = row.findViewById(R.id.user_row_email);
            EditText fullName = row.findViewById(R.id.user_row_full_name);
            Spinner roleSpinner = row.findViewById(R.id.user_row_role);
            Button saveButton = row.findViewById(R.id.user_row_save);
            Button deleteButton = row.findViewById(R.id.user_row_delete);

            long userId = user.optLong("id");
            String userRole = user.optString("rol");
            username.setText(user.optString("username"));
            username.setEnabled(false);
            email.setText(user.optString("email"));
            fullName.setText(user.optString("fullName"));

            ArrayAdapter<String> roleAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Alumno", "Profesor"}
            );
            roleAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            roleSpinner.setAdapter(roleAdapter);
            roleSpinner.setSelection("alumno".equalsIgnoreCase(userRole) ? 0 : 1);

            saveButton.setOnClickListener(view -> {
                new Thread(() -> {
                    try {
                        ApiClient api = new ApiClient(this);
                        Map<String, String> params = new HashMap<>();
                        params.put("id", String.valueOf(userId));
                        params.put("email", email.getText().toString().trim());
                        params.put("fullName", fullName.getText().toString().trim());
                        params.put("rol", roleSpinner.getSelectedItemPosition() == 0 ? "alumno" : "profesor");
                        JSONObject response = api.post("/api/gestion/usuarios/actualizar", params);
                        runOnUiThread(() -> {
                            if (response.optString("error", "").isEmpty()) {
                                showFlash("✅ Usuario actualizado.", true);
                            } else {
                                showFlash(response.optString("error", "No se pudo actualizar."), false);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                    }
                }).start();
            });

            deleteButton.setOnClickListener(view -> {
                if (userId == currentUser.getId()) {
                    showFlash("No puedes eliminar el usuario activo.", false);
                    return;
                }
                new Thread(() -> {
                    try {
                        ApiClient api = new ApiClient(this);
                        Map<String, String> params = new HashMap<>();
                        params.put("id", String.valueOf(userId));
                        JSONObject response = api.post("/api/gestion/usuarios/eliminar", params);
                        runOnUiThread(() -> {
                            if (response.optString("error", "").isEmpty()) {
                                showFlash("Usuario eliminado.", true);
                                loadGestion();
                            } else {
                                showFlash(response.optString("error", "No se pudo eliminar."), false);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                    }
                }).start();
            });

            usersList.addView(row);
        }
    }

    private void renderMaterias(JSONArray materias) {
        materiasList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (materias == null) {
            return;
        }
        for (int i = 0; i < materias.length(); i++) {
            JSONObject materia = materias.optJSONObject(i);
            if (materia == null) continue;
            View row = inflater.inflate(R.layout.item_materia_row, materiasList, false);
            EditText name = row.findViewById(R.id.materia_row_name);
            EditText description = row.findViewById(R.id.materia_row_description);
            Button saveButton = row.findViewById(R.id.materia_row_save);
            Button deleteButton = row.findViewById(R.id.materia_row_delete);

            long materiaId = materia.optLong("id");
            name.setText(materia.optString("nombre"));
            description.setText(materia.optString("descripcion"));

            saveButton.setOnClickListener(view -> {
                new Thread(() -> {
                    try {
                        ApiClient api = new ApiClient(this);
                        Map<String, String> params = new HashMap<>();
                        params.put("id", String.valueOf(materiaId));
                        params.put("codigo", materia.optString("codigo"));
                        params.put("nombre", name.getText().toString().trim());
                        params.put("descripcion", description.getText().toString().trim());
                        JSONObject response = api.post("/api/gestion/materias/actualizar", params);
                        runOnUiThread(() -> {
                            if (response.optString("error", "").isEmpty()) {
                                showFlash("✅ Materia actualizada.", true);
                            } else {
                                showFlash(response.optString("error", "No se pudo actualizar."), false);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                    }
                }).start();
            });

            deleteButton.setOnClickListener(view -> {
                new Thread(() -> {
                    try {
                        ApiClient api = new ApiClient(this);
                        Map<String, String> params = new HashMap<>();
                        params.put("id", String.valueOf(materiaId));
                        JSONObject response = api.post("/api/gestion/materias/eliminar", params);
                        runOnUiThread(() -> {
                            if (response.optString("error", "").isEmpty()) {
                                showFlash("Materia eliminada.", true);
                                loadGestion();
                            } else {
                                showFlash(response.optString("error", "No se pudo eliminar."), false);
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
                    }
                }).start();
            });

            materiasList.addView(row);
        }
    }

    private void showFlash(String message, boolean success) {
        flashMessage.setText(message);
        flashMessage.setVisibility(View.VISIBLE);
        flashMessage.setBackgroundResource(success ? R.drawable.bg_message_success : R.drawable.bg_message_error);
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void loadGestion() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/gestion", null);
                JSONArray users = response.optJSONArray("usuarios");
                JSONArray materias = response.optJSONArray("materias");
                runOnUiThread(() -> {
                    renderUsers(users);
                    renderMaterias(materias);
                });
            } catch (Exception e) {
                runOnUiThread(() -> showFlash("No se pudo cargar la gestión.", false));
            }
        }).start();
    }
}
