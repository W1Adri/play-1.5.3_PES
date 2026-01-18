package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GestionActivity extends AppCompatActivity {

    private static class UsuarioItem {
        String id;
        String username;
        String fullName;
        String email;
        String rol;

        UsuarioItem(String id, String username, String fullName, String email, String rol) {
            this.id = id;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.rol = rol;
        }
    }

    private static class MateriaItem {
        String id;
        String codigo;
        String nombre;
        String descripcion;

        MateriaItem(String id, String codigo, String nombre, String descripcion) {
            this.id = id;
            this.codigo = codigo;
            this.nombre = nombre;
            this.descripcion = descripcion;
        }
    }

    private final ArrayList<String> items = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final ArrayList<UsuarioItem> usuarios = new ArrayList<>();
    private final ArrayList<MateriaItem> materias = new ArrayList<>();
    private boolean mostrandoUsuarios = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gestion);

        TextView txtModo = findViewById(R.id.txtModo);
        ListView list = findViewById(R.id.listGestion);
        Button btnUsuarios = findViewById(R.id.btnUsuarios);
        Button btnMaterias = findViewById(R.id.btnMaterias);
        Button btnBack = findViewById(R.id.btnBack);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        btnUsuarios.setOnClickListener(v -> {
            mostrandoUsuarios = true;
            txtModo.setText("Gestión de usuarios");
            cargarUsuarios();
        });
        btnMaterias.setOnClickListener(v -> {
            mostrandoUsuarios = false;
            txtModo.setText("Gestión de materias");
            cargarMaterias();
        });
        btnBack.setOnClickListener(v -> finish());

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (mostrandoUsuarios) {
                if (position < 0 || position >= usuarios.size()) return;
                UsuarioItem usuario = usuarios.get(position);
                mostrarOpcionesUsuario(usuario);
            } else {
                if (position < 0 || position >= materias.size()) return;
                MateriaItem materia = materias.get(position);
                mostrarOpcionesMateria(materia);
            }
        });

        cargarUsuarios();
    }

    private void cargarUsuarios() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/gestion/usuarios");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray arr = json.optJSONArray("usuarios");
                    ArrayList<UsuarioItem> nuevos = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String id = item.optString("id", "");
                            String username = item.optString("username", "");
                            String fullName = item.optString("fullName", "");
                            String email = item.optString("email", "");
                            String rol = item.optString("rol", "");
                            nuevos.add(new UsuarioItem(id, username, fullName, email, rol));
                            labels.add(username + " · " + fullName + "\n" + email + " · " + rol);
                        }
                    }
                    runOnUiThread(() -> {
                        usuarios.clear();
                        usuarios.addAll(nuevos);
                        items.clear();
                        items.addAll(labels);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void cargarMaterias() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/gestion/materias");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray arr = json.optJSONArray("materias");
                    ArrayList<MateriaItem> nuevos = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String id = item.optString("id", "");
                            String codigo = item.optString("codigo", "");
                            String nombre = item.optString("nombre", "");
                            String descripcion = item.optString("descripcion", "");
                            nuevos.add(new MateriaItem(id, codigo, nombre, descripcion));
                            labels.add(codigo + " · " + nombre + "\n" + descripcion);
                        }
                    }
                    runOnUiThread(() -> {
                        materias.clear();
                        materias.addAll(nuevos);
                        items.clear();
                        items.addAll(labels);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void mostrarOpcionesUsuario(UsuarioItem usuario) {
        String[] opciones = new String[] {"Editar", "Eliminar"};
        new AlertDialog.Builder(this)
                .setTitle("Usuario: " + usuario.username)
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        mostrarEditarUsuario(usuario);
                    } else {
                        eliminarUsuario(usuario);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarEditarUsuario(UsuarioItem usuario) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 0);

        EditText edtUsername = new EditText(this);
        edtUsername.setHint("Username");
        edtUsername.setText(usuario.username);
        layout.addView(edtUsername);

        EditText edtFullName = new EditText(this);
        edtFullName.setHint("Nombre completo");
        edtFullName.setText(usuario.fullName);
        layout.addView(edtFullName);

        EditText edtEmail = new EditText(this);
        edtEmail.setHint("Email");
        edtEmail.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        edtEmail.setText(usuario.email);
        layout.addView(edtEmail);

        Spinner spRol = new Spinner(this);
        String[] roles = new String[] {"alumno", "profesor"};
        ArrayAdapter<String> rolAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, roles);
        rolAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spRol.setAdapter(rolAdapter);
        spRol.setSelection("PROFESOR".equalsIgnoreCase(usuario.rol) ? 1 : 0);
        layout.addView(spRol);

        new AlertDialog.Builder(this)
                .setTitle("Editar usuario")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String username = edtUsername.getText().toString().trim();
                    String fullName = edtFullName.getText().toString().trim();
                    String email = edtEmail.getText().toString().trim();
                    String rol = (String) spRol.getSelectedItem();
                    actualizarUsuario(usuario.id, username, fullName, email, rol);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarUsuario(String id, String username, String fullName, String email, String rol) {
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("id", id);
                params.put("username", username);
                params.put("fullName", fullName);
                params.put("email", email);
                params.put("rol", rol);
                ApiClient.ApiResponse response = ApiClient.postForm("/api/gestion/usuarios/actualizar", params);
                JSONObject json = ApiClient.parseJson(response.body);
                String msg = json != null ? json.optString("msg", response.body) : response.body;
                runOnUiThread(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    cargarUsuarios();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void eliminarUsuario(UsuarioItem usuario) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar usuario")
                .setMessage("¿Eliminar " + usuario.username + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            Map<String, String> params = new HashMap<>();
                            params.put("id", usuario.id);
                            ApiClient.ApiResponse response = ApiClient.postForm("/api/gestion/usuarios/eliminar", params);
                            JSONObject json = ApiClient.parseJson(response.body);
                            String msg = json != null ? json.optString("msg", response.body) : response.body;
                            runOnUiThread(() -> {
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                cargarUsuarios();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarOpcionesMateria(MateriaItem materia) {
        String[] opciones = new String[] {"Editar", "Eliminar"};
        new AlertDialog.Builder(this)
                .setTitle("Materia: " + materia.codigo)
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) {
                        mostrarEditarMateria(materia);
                    } else {
                        eliminarMateria(materia);
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void mostrarEditarMateria(MateriaItem materia) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 16, 24, 0);

        EditText edtCodigo = new EditText(this);
        edtCodigo.setHint("Código");
        edtCodigo.setText(materia.codigo);
        layout.addView(edtCodigo);

        EditText edtNombre = new EditText(this);
        edtNombre.setHint("Nombre");
        edtNombre.setText(materia.nombre);
        layout.addView(edtNombre);

        EditText edtDescripcion = new EditText(this);
        edtDescripcion.setHint("Descripción");
        edtDescripcion.setText(materia.descripcion);
        layout.addView(edtDescripcion);

        new AlertDialog.Builder(this)
                .setTitle("Editar materia")
                .setView(layout)
                .setPositiveButton("Guardar", (dialog, which) -> {
                    String codigo = edtCodigo.getText().toString().trim();
                    String nombre = edtNombre.getText().toString().trim();
                    String descripcion = edtDescripcion.getText().toString().trim();
                    actualizarMateria(materia.id, codigo, nombre, descripcion);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void actualizarMateria(String id, String codigo, String nombre, String descripcion) {
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("id", id);
                params.put("codigo", codigo);
                params.put("nombre", nombre);
                params.put("descripcion", descripcion);
                ApiClient.ApiResponse response = ApiClient.postForm("/api/gestion/materias/actualizar", params);
                JSONObject json = ApiClient.parseJson(response.body);
                String msg = json != null ? json.optString("msg", response.body) : response.body;
                runOnUiThread(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    cargarMaterias();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void eliminarMateria(MateriaItem materia) {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar materia")
                .setMessage("¿Eliminar " + materia.codigo + "?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    new Thread(() -> {
                        try {
                            Map<String, String> params = new HashMap<>();
                            params.put("id", materia.id);
                            ApiClient.ApiResponse response = ApiClient.postForm("/api/gestion/materias/eliminar", params);
                            JSONObject json = ApiClient.parseJson(response.body);
                            String msg = json != null ? json.optString("msg", response.body) : response.body;
                            runOnUiThread(() -> {
                                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                                cargarMaterias();
                            });
                        } catch (Exception e) {
                            runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    }).start();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
