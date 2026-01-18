package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class MateriasActivity extends AppCompatActivity {

    static class MateriaItem {
        String id, codigo, nombre, desc;
        boolean inscrita;
        ArrayList<ProfesorItem> profesores = new ArrayList<>();

        MateriaItem(String id, String c, String n, String d, boolean ins) {
            this.id = id; codigo = c; nombre = n; desc = d; inscrita = ins;
        }

        @Override public String toString() {
            return codigo + " - " + nombre + (inscrita ? " ✅" : "") + "\n" + desc;
        }
    }

    static class ProfesorItem {
        String id, nombre;

        ProfesorItem(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    static class InscripcionItem {
        String id;
        String materia;
        String alumno;
        String profesor;

        InscripcionItem(String id, String materia, String alumno, String profesor) {
            this.id = id;
            this.materia = materia;
            this.alumno = alumno;
            this.profesor = profesor;
        }

        @Override public String toString() {
            return materia + "\nAlumno: " + alumno + "\nProfesor: " + profesor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materias);

        String rol = getIntent().getStringExtra("rol");
        boolean soloInscritas = getIntent().getBooleanExtra("soloInscritas", false);
        boolean modoProfesor = getIntent().getBooleanExtra("modoProfesor", false);

        TextView txtSub = findViewById(R.id.txtSub);
        if (modoProfesor) txtSub.setText("Modo profesor: tus alumnos");
        else if (soloInscritas) txtSub.setText("Tus inscripciones");
        else txtSub.setText("Lista de materias");

        ListView list = findViewById(R.id.listMaterias);
        ArrayList<String> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        ArrayList<MateriaItem> materias = new ArrayList<>();
        ArrayList<InscripcionItem> inscripciones = new ArrayList<>();
        if (modoProfesor) {
            cargarInscripciones(items, inscripciones, adapter);
        } else {
            cargarMaterias(items, materias, adapter, soloInscritas);
        }
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        list.setOnItemClickListener((parent, view, position, id) -> {
            if (modoProfesor) {
                Toast.makeText(this, "Detalle de inscripción disponible", Toast.LENGTH_SHORT).show();
                return;
            }

            MateriaItem materia = materias.get(position);
            if (rol != null && rol.equalsIgnoreCase("ALUMNO")) {
                Intent intent = new Intent(this, MateriaDetalleActivity.class);
                intent.putExtra("materiaId", materia.id);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Solo alumnos pueden inscribirse.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarMaterias(ArrayList<String> items, ArrayList<MateriaItem> materias,
                                ArrayAdapter<String> adapter, boolean soloInscritas) {
        new Thread(() -> {
            try {
                String path = "/api/materias";
                if (soloInscritas) {
                    path = path + "?soloInscritas=true";
                }
                ApiClient.ApiResponse response = ApiClient.get(path);
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    materias.clear();
                    items.clear();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        MateriaItem materia = new MateriaItem(
                            obj.optString("id", ""),
                            obj.optString("codigo", ""),
                            obj.optString("nombre", ""),
                            obj.optString("descripcion", ""),
                            obj.optBoolean("inscrita", false)
                        );
                        materias.add(materia);
                        items.add(materia.toString());
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

    private void cargarInscripciones(ArrayList<String> items, ArrayList<InscripcionItem> inscripciones,
                                     ArrayAdapter<String> adapter) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/inscripciones");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    inscripciones.clear();
                    items.clear();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        JSONObject materia = obj.optJSONObject("materia");
                        JSONObject alumno = obj.optJSONObject("alumno");
                        JSONObject profesor = obj.optJSONObject("profesor");
                        InscripcionItem item = new InscripcionItem(
                            obj.optString("id", ""),
                            materia == null ? "" : materia.optString("nombre", ""),
                            alumno == null ? "" : alumno.optString("fullName", alumno.optString("username", "")),
                            profesor == null ? "" : profesor.optString("fullName", profesor.optString("username", ""))
                        );
                        inscripciones.add(item);
                        items.add(item.toString());
                    }
                    runOnUiThread(adapter::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudieron cargar inscripciones", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void seleccionarProfesorEInscribirse(MateriaItem materia, ArrayAdapter<String> adapter,
                                                 ArrayList<String> items, ArrayList<MateriaItem> materias) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/materia/" + materia.id);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    JSONArray profesoresJson = json.optJSONArray("profesores");
                    if (profesoresJson == null || profesoresJson.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(this, "No hay profesores disponibles.", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    ArrayList<ProfesorItem> profesores = new ArrayList<>();
                    String[] labels = new String[profesoresJson.length()];
                    for (int i = 0; i < profesoresJson.length(); i++) {
                        JSONObject profesor = profesoresJson.optJSONObject(i);
                        if (profesor == null) continue;
                        String id = profesor.optString("id", "");
                        String nombre = profesor.optString("fullName", profesor.optString("username", ""));
                        profesores.add(new ProfesorItem(id, nombre));
                        labels[i] = nombre;
                    }

                    runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Selecciona profesor")
                        .setItems(labels, (dialog, which) -> inscribirse(materia, profesores.get(which), adapter, items, materias))
                        .show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo cargar la materia.", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void inscribirse(MateriaItem materia, ProfesorItem profesor, ArrayAdapter<String> adapter,
                             ArrayList<String> items, ArrayList<MateriaItem> materias) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("materiaId", materia.id);
                payload.put("profesorId", profesor.id);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/inscripciones", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> Toast.makeText(this, "Inscripción creada.", Toast.LENGTH_SHORT).show());
                    cargarMaterias(items, materias, adapter, false);
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
