package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class GestionActivity extends AppCompatActivity {

    private ArrayAdapter<String> adapter;
    private final ArrayList<String> items = new ArrayList<>();
    private final ArrayList<UsuarioItem> usuarios = new ArrayList<>();
    private final ArrayList<MateriaItem> materias = new ArrayList<>();
    private boolean mostrandoUsuarios = true;

    static class UsuarioItem {
        String id;
        String username;
        String fullName;
        String email;
        String rol;

        @Override
        public String toString() {
            return fullName + " · " + username + " (" + rol + ")\n" + email;
        }
    }

    static class MateriaItem {
        String id;
        String codigo;
        String nombre;
        String descripcion;

        @Override
        public String toString() {
            return codigo + " - " + nombre + "\n" + descripcion;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion);

        Button btnUsuarios = findViewById(R.id.btnUsuarios);
        Button btnMaterias = findViewById(R.id.btnMaterias);
        Button btnBack = findViewById(R.id.btnBack);
        ListView list = findViewById(R.id.listGestion);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        btnUsuarios.setOnClickListener(v -> {
            mostrandoUsuarios = true;
            cargarUsuarios();
        });
        btnMaterias.setOnClickListener(v -> {
            mostrandoUsuarios = false;
            cargarMaterias();
        });
        btnBack.setOnClickListener(v -> finish());

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (mostrandoUsuarios) {
                mostrarDialogoUsuario(usuarios.get(position));
            } else {
                mostrarDialogoMateria(materias.get(position));
            }
        });

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/gestion/usuarios");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    usuarios.clear();
                    items.clear();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        UsuarioItem item = new UsuarioItem();
                        item.id = obj.optString("id", "");
                        item.username = obj.optString("username", "");
                        item.fullName = obj.optString("fullName", "");
                        item.email = obj.optString("email", "");
                        item.rol = obj.optString("rol", "");
                        usuarios.add(item);
                        items.add(item.toString());
                    }
                    runOnUiThread(adapter::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudieron cargar usuarios", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void cargarMaterias() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/gestion/materias");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    materias.clear();
                    items.clear();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        MateriaItem item = new MateriaItem();
                        item.id = obj.optString("id", "");
                        item.codigo = obj.optString("codigo", "");
                        item.nombre = obj.optString("nombre", "");
                        item.descripcion = obj.optString("descripcion", "");
                        materias.add(item);
                        items.add(item.toString());
                    }
                    runOnUiThread(adapter::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudieron cargar materias", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarDialogoUsuario(UsuarioItem usuario) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 12);

        EditText edtUsername = new EditText(this);
        edtUsername.setHint("Usuario");
        edtUsername.setText(usuario.username);

        EditText edtFullName = new EditText(this);
        edtFullName.setHint("Nombre completo");
        edtFullName.setText(usuario.fullName);

        EditText edtEmail = new EditText(this);
        edtEmail.setHint("Email");
        edtEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtEmail.setText(usuario.email);

        EditText edtRol = new EditText(this);
        edtRol.setHint("Rol (ALUMNO/PROFESOR)");
        edtRol.setText(usuario.rol);

        layout.addView(edtUsername);
        layout.addView(edtFullName);
        layout.addView(edtEmail);
        layout.addView(edtRol);

        new AlertDialog.Builder(this)
            .setTitle("Editar usuario")
            .setView(layout)
            .setPositiveButton("Guardar", (dialog, which) -> actualizarUsuario(
                usuario.id,
                edtUsername.getText().toString().trim(),
                edtEmail.getText().toString().trim(),
                edtFullName.getText().toString().trim(),
                edtRol.getText().toString().trim()
            ))
            .setNeutralButton("Eliminar", (dialog, which) -> eliminarUsuario(usuario.id))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void mostrarDialogoMateria(MateriaItem materia) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 12);

        EditText edtCodigo = new EditText(this);
        edtCodigo.setHint("Código");
        edtCodigo.setText(materia.codigo);

        EditText edtNombre = new EditText(this);
        edtNombre.setHint("Nombre");
        edtNombre.setText(materia.nombre);

        EditText edtDescripcion = new EditText(this);
        edtDescripcion.setHint("Descripción");
        edtDescripcion.setText(materia.descripcion);

        layout.addView(edtCodigo);
        layout.addView(edtNombre);
        layout.addView(edtDescripcion);

        new AlertDialog.Builder(this)
            .setTitle("Editar materia")
            .setView(layout)
            .setPositiveButton("Guardar", (dialog, which) -> actualizarMateria(
                materia.id,
                edtCodigo.getText().toString().trim(),
                edtNombre.getText().toString().trim(),
                edtDescripcion.getText().toString().trim()
            ))
            .setNeutralButton("Eliminar", (dialog, which) -> eliminarMateria(materia.id))
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void actualizarUsuario(String id, String username, String email, String fullName, String rol) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", id);
                payload.put("username", username);
                payload.put("email", email);
                payload.put("fullName", fullName);
                payload.put("rol", rol);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/gestion/usuarios/actualizar", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Usuario actualizado", Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void eliminarUsuario(String id) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", id);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/gestion/usuarios/eliminar", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Usuario eliminado", Toast.LENGTH_SHORT).show();
                        cargarUsuarios();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void actualizarMateria(String id, String codigo, String nombre, String descripcion) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", id);
                payload.put("codigo", codigo);
                payload.put("nombre", nombre);
                payload.put("descripcion", descripcion);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/gestion/materias/actualizar", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Materia actualizada", Toast.LENGTH_SHORT).show();
                        cargarMaterias();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void eliminarMateria(String id) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", id);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/gestion/materias/eliminar", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Materia eliminada", Toast.LENGTH_SHORT).show();
                        cargarMaterias();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
