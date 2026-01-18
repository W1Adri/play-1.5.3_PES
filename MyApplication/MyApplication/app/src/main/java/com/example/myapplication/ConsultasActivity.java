package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsultasActivity extends AppCompatActivity {

    private static class MateriaOption {
        String id;
        String nombre;

        MateriaOption(String id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }
    }

    private final List<MateriaOption> materias = new ArrayList<>();
    private ArrayAdapter<String> materiasAdapter;
    private ArrayAdapter<String> resultadosAdapter;
    private ArrayList<String> resultados = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultas);

        Spinner spTipo = findViewById(R.id.spTipo);
        Spinner spMateria = findViewById(R.id.spMateria);
        TextView txtResultado = findViewById(R.id.txtResultado);
        ListView listResultados = findViewById(R.id.listResultados);
        Button btnConsultar = findViewById(R.id.btnConsultar);
        Button btnBack = findViewById(R.id.btnBack);

        String[] tipos = new String[] {
                "totalUsuarios",
                "totalAlumnos",
                "totalProfesores",
                "profesoresMateria",
                "alumnosMateria",
                "reservasPorMateria",
                "topAlumnos",
                "topProfesores"
        };
        ArrayAdapter<String> tiposAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, tipos);
        tiposAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(tiposAdapter);

        materiasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        materiasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMateria.setAdapter(materiasAdapter);

        resultadosAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultados);
        listResultados.setAdapter(resultadosAdapter);

        btnBack.setOnClickListener(v -> finish());

        spTipo.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String tipo = (String) spTipo.getSelectedItem();
                boolean requiereMateria = "profesoresMateria".equals(tipo) || "alumnosMateria".equals(tipo);
                spMateria.setVisibility(requiereMateria ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        btnConsultar.setOnClickListener(v -> {
            String tipo = (String) spTipo.getSelectedItem();
            String materiaId = null;
            if ("profesoresMateria".equals(tipo) || "alumnosMateria".equals(tipo)) {
                int idx = spMateria.getSelectedItemPosition();
                if (idx >= 0 && idx < materias.size()) {
                    materiaId = materias.get(idx).id;
                }
            }
            consultar(tipo, materiaId, txtResultado);
        });

        cargarMaterias();
    }

    private void cargarMaterias() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/materias/todas");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray arr = json.optJSONArray("materias");
                    ArrayList<String> labels = new ArrayList<>();
                    materias.clear();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String id = item.optString("id", "");
                            String nombre = item.optString("nombre", "");
                            materias.add(new MateriaOption(id, nombre));
                            labels.add(nombre);
                        }
                    }
                    runOnUiThread(() -> {
                        materiasAdapter.clear();
                        materiasAdapter.addAll(labels);
                        materiasAdapter.notifyDataSetChanged();
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

    private void consultar(String tipo, String materiaId, TextView txtResultado) {
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("tipo", tipo);
                if (materiaId != null) {
                    params.put("materiaId", materiaId);
                }
                ApiClient.ApiResponse response = ApiClient.get("/api/consultas", params);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    String descripcion = json.optString("descripcion", "");
                    long resultado = json.optLong("resultado", 0L);
                    JSONArray detalleUsuarios = json.optJSONArray("detalleUsuarios");
                    JSONArray ranking = json.optJSONArray("ranking");
                    ArrayList<String> items = new ArrayList<>();
                    if (detalleUsuarios != null) {
                        for (int i = 0; i < detalleUsuarios.length(); i++) {
                            JSONObject u = detalleUsuarios.optJSONObject(i);
                            if (u == null) continue;
                            String name = u.optString("fullName", u.optString("username", "Usuario"));
                            items.add("👤 " + name);
                        }
                    }
                    if (ranking != null) {
                        for (int i = 0; i < ranking.length(); i++) {
                            JSONObject r = ranking.optJSONObject(i);
                            if (r == null) continue;
                            items.add(r.optString("label", "") + " · " + r.optLong("count", 0));
                        }
                    }
                    runOnUiThread(() -> {
                        txtResultado.setText(descripcion + " · " + resultado);
                        resultados.clear();
                        resultados.addAll(items);
                        resultadosAdapter.notifyDataSetChanged();
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
}
