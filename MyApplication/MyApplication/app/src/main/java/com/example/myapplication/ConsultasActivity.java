package com.example.myapplication;

import android.os.Bundle;
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

public class ConsultasActivity extends AppCompatActivity {

    private final ArrayList<String> resultados = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final ArrayList<MateriaItem> materias = new ArrayList<>();

    static class MateriaItem {
        String id;
        String nombre;

        @Override
        public String toString() {
            return nombre;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_consultas);

        Spinner spTipo = findViewById(R.id.spTipo);
        Spinner spMateria = findViewById(R.id.spMateria);
        TextView txtResultado = findViewById(R.id.txtResultado);
        Button btnConsultar = findViewById(R.id.btnConsultar);
        Button btnBack = findViewById(R.id.btnBack);
        ListView listDetalle = findViewById(R.id.listDetalle);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, resultados);
        listDetalle.setAdapter(adapter);

        ArrayAdapter<CharSequence> tiposAdapter = ArrayAdapter.createFromResource(
            this,
            R.array.consultas_array,
            android.R.layout.simple_spinner_item
        );
        tiposAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spTipo.setAdapter(tiposAdapter);

        ArrayAdapter<MateriaItem> materiasAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, materias);
        materiasAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spMateria.setAdapter(materiasAdapter);

        btnBack.setOnClickListener(v -> finish());

        btnConsultar.setOnClickListener(v -> {
            String tipo = spTipo.getSelectedItem().toString();
            MateriaItem materia = (MateriaItem) spMateria.getSelectedItem();
            ejecutarConsulta(tipo, materia, txtResultado);
        });

        cargarMaterias(materiasAdapter);
    }

    private void cargarMaterias(ArrayAdapter<MateriaItem> adapterMaterias) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/materias");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    materias.clear();
                    MateriaItem vacia = new MateriaItem();
                    vacia.id = "";
                    vacia.nombre = "Selecciona una materia…";
                    materias.add(vacia);
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        MateriaItem item = new MateriaItem();
                        item.id = obj.optString("id", "");
                        item.nombre = obj.optString("nombre", "");
                        materias.add(item);
                    }
                    runOnUiThread(adapterMaterias::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudieron cargar materias", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void ejecutarConsulta(String tipo, MateriaItem materia, TextView txtResultado) {
        resultados.clear();
        adapter.notifyDataSetChanged();
        txtResultado.setText("Consultando...");
        new Thread(() -> {
            try {
                String path = "/api/consultas?tipo=" + tipo;
                if (materia != null && materia.id != null && !materia.id.isEmpty()) {
                    path += "&materiaId=" + materia.id;
                }
                ApiClient.ApiResponse response = ApiClient.get(path);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    String status = json.optString("status", "error");
                    if (!"ok".equalsIgnoreCase(status)) {
                        String error = json.optString("error", "Consulta inválida");
                        runOnUiThread(() -> txtResultado.setText(error));
                        return;
                    }
                    Long resultado = json.has("resultado") ? json.optLong("resultado") : null;
                    String descripcion = json.optString("descripcion", "");
                    JSONArray detalleUsuarios = json.optJSONArray("detalleUsuarios");
                    JSONArray ranking = json.optJSONArray("ranking");

                    runOnUiThread(() -> {
                        if (resultado != null) {
                            txtResultado.setText(String.valueOf(resultado) + " · " + descripcion);
                        } else {
                            txtResultado.setText(descripcion.isEmpty() ? "Sin resultados" : descripcion);
                        }
                    });

                    if (detalleUsuarios != null) {
                        for (int i = 0; i < detalleUsuarios.length(); i++) {
                            JSONObject usuario = detalleUsuarios.optJSONObject(i);
                            if (usuario == null) continue;
                            String label = usuario.optString("fullName", "") + " · " + usuario.optString("username", "");
                            resultados.add(label.trim());
                        }
                    } else if (ranking != null) {
                        for (int i = 0; i < ranking.length(); i++) {
                            JSONObject item = ranking.optJSONObject(i);
                            if (item == null) continue;
                            resultados.add(item.optString("label", "") + " (" + item.optString("count", "") + ")");
                        }
                    }

                    runOnUiThread(adapter::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> txtResultado.setText("No se pudo ejecutar la consulta"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> txtResultado.setText("Error: " + e.getMessage()));
            }
        }).start();
    }
}
