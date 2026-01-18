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

public class MateriaDetalleActivity extends AppCompatActivity {

    private String materiaId;
    private final ArrayList<String> profesores = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private final ArrayList<String> profesoresIds = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materia_detalle);

        materiaId = getIntent().getStringExtra("materiaId");
        TextView txtTitulo = findViewById(R.id.txtTitulo);
        TextView txtDesc = findViewById(R.id.txtDescripcion);
        Button btnInscribir = findViewById(R.id.btnInscribir);
        Button btnBack = findViewById(R.id.btnBack);
        ListView list = findViewById(R.id.listProfesores);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, profesores);
        list.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());

        btnInscribir.setOnClickListener(v -> {
            int selected = list.getCheckedItemPosition();
            if (selected == ListView.INVALID_POSITION || selected >= profesoresIds.size()) {
                Toast.makeText(this, "Selecciona un profesor.", Toast.LENGTH_SHORT).show();
                return;
            }
            inscribirse(profesoresIds.get(selected));
        });

        cargarDetalle(txtTitulo, txtDesc);
    }

    private void cargarDetalle(TextView titulo, TextView descripcion) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/materia/" + materiaId);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    String nombre = json.optString("nombre", "");
                    String codigo = json.optString("codigo", "");
                    String desc = json.optString("descripcion", "");
                    JSONArray profesoresJson = json.optJSONArray("profesores");

                    profesores.clear();
                    profesoresIds.clear();
                    if (profesoresJson != null) {
                        for (int i = 0; i < profesoresJson.length(); i++) {
                            JSONObject profesor = profesoresJson.optJSONObject(i);
                            if (profesor == null) continue;
                            String id = profesor.optString("id", "");
                            String label = profesor.optString("fullName", profesor.optString("username", ""));
                            profesoresIds.add(id);
                            profesores.add(label);
                        }
                    }

                    runOnUiThread(() -> {
                        titulo.setText(codigo + " · " + nombre);
                        descripcion.setText(desc.isEmpty() ? "Sin descripción" : desc);
                        adapter.notifyDataSetChanged();
                    });
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo cargar la materia", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void inscribirse(String profesorId) {
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("materiaId", materiaId);
                payload.put("profesorId", profesorId);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/inscripciones", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> Toast.makeText(this, "Inscripción creada.", Toast.LENGTH_SHORT).show());
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
