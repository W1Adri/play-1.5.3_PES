package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MateriasActivity extends AppCompatActivity {

    static class MateriaItem {
        String id;
        String codigo, nombre, desc;
        boolean inscrita;
        List<ProfesorItem> profesores;

        MateriaItem(String id, String c, String n, String d, boolean ins, List<ProfesorItem> profesores) {
            this.id = id;
            codigo = c;
            nombre = n;
            desc = d;
            inscrita = ins;
            this.profesores = profesores;
        }

        @Override public String toString() {
            return codigo + " - " + nombre + (inscrita ? " ✅" : "") + "\n" + desc;
        }
    }

    static class ProfesorItem {
        String id;
        String nombre;

        ProfesorItem(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
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
        if (modoProfesor) txtSub.setText("Modo profesor: ver alumnos");
        else if (soloInscritas) txtSub.setText("Tus inscripciones");
        else txtSub.setText("Lista de materias");

        ListView list = findViewById(R.id.listMaterias);
        ArrayList<String> items = new ArrayList<>();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("soloInscritas", String.valueOf(soloInscritas));
                params.put("modoProfesor", String.valueOf(modoProfesor));
                ApiClient.ApiResponse response = ApiClient.get("/api/materias", params);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    ArrayList<MateriaItem> materias = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();

                    if (modoProfesor) {
                        JSONArray inscripciones = json.optJSONArray("inscripciones");
                        if (inscripciones != null) {
                            for (int i = 0; i < inscripciones.length(); i++) {
                                JSONObject item = inscripciones.optJSONObject(i);
                                JSONObject alumno = item != null ? item.optJSONObject("alumno") : null;
                                JSONObject materia = item != null ? item.optJSONObject("materia") : null;
                                if (alumno == null || materia == null) continue;
                                String alumnoNombre = alumno.optString("fullName", alumno.optString("username"));
                                String materiaNombre = materia.optString("nombre", "Materia");
                                String materiaCodigo = materia.optString("codigo", "");
                                labels.add(materiaCodigo + " - " + materiaNombre + "\nAlumno: " + alumnoNombre);
                            }
                        }
                    } else {
                        JSONArray arr = json.optJSONArray("materias");
                        if (arr != null) {
                            for (int i = 0; i < arr.length(); i++) {
                                JSONObject item = arr.optJSONObject(i);
                                if (item == null) continue;
                                String id = item.optString("id", "");
                                String codigo = item.optString("codigo", "");
                                String nombre = item.optString("nombre", "");
                                String desc = item.optString("descripcion", "");
                                boolean inscrita = item.optBoolean("inscrita", false);
                                List<ProfesorItem> profesores = new ArrayList<>();
                                JSONArray profs = item.optJSONArray("profesores");
                                if (profs != null) {
                                    for (int p = 0; p < profs.length(); p++) {
                                        JSONObject prof = profs.optJSONObject(p);
                                        if (prof == null) continue;
                                        profesores.add(new ProfesorItem(
                                                prof.optString("id", ""),
                                                prof.optString("fullName", prof.optString("username", "Profesor"))
                                        ));
                                    }
                                }
                                MateriaItem materiaItem = new MateriaItem(id, codigo, nombre, desc, inscrita, profesores);
                                materias.add(materiaItem);
                                labels.add(materiaItem.toString());
                            }
                        }
                    }

                    runOnUiThread(() -> {
                        items.clear();
                        items.addAll(labels);
                        adapter.notifyDataSetChanged();

                        list.setOnItemClickListener((parent, view, position, id) -> {
                            if (modoProfesor) {
                                Toast.makeText(this, "Alumno seleccionado", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            if (position < 0 || position >= materias.size()) return;
                            MateriaItem selected = materias.get(position);
                            try {
                                org.json.JSONArray profs = new org.json.JSONArray();
                                if (selected.profesores != null) {
                                    for (ProfesorItem profesor : selected.profesores) {
                                        org.json.JSONObject p = new org.json.JSONObject();
                                        p.put("id", profesor.id);
                                        p.put("nombre", profesor.nombre);
                                        profs.put(p);
                                    }
                                }
                                android.content.Intent intent = new android.content.Intent(this, MateriaDetalleActivity.class);
                                intent.putExtra("materiaId", selected.id);
                                intent.putExtra("codigo", selected.codigo);
                                intent.putExtra("nombre", selected.nombre);
                                intent.putExtra("descripcion", selected.desc);
                                intent.putExtra("inscrita", selected.inscrita);
                                intent.putExtra("profesores", profs.toString());
                                startActivity(intent);
                            } catch (Exception e) {
                                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            }
                        });
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + msg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
