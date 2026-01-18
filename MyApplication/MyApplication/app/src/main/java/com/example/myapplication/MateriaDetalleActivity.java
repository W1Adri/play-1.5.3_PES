package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class MateriaDetalleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detalle_materia);

        String materiaId = getIntent().getStringExtra("materiaId");
        String codigo = getIntent().getStringExtra("codigo");
        String nombre = getIntent().getStringExtra("nombre");
        String descripcion = getIntent().getStringExtra("descripcion");
        boolean inscrita = getIntent().getBooleanExtra("inscrita", false);
        String profesoresJson = getIntent().getStringExtra("profesores");

        TextView txtTitulo = findViewById(R.id.txtTitulo);
        TextView txtCodigo = findViewById(R.id.txtCodigo);
        TextView txtDescripcion = findViewById(R.id.txtDescripcion);
        TextView txtEstado = findViewById(R.id.txtEstado);
        Button btnInscribirse = findViewById(R.id.btnInscribirse);
        Button btnBack = findViewById(R.id.btnBack);

        txtTitulo.setText(nombre != null ? nombre : "Materia");
        txtCodigo.setText(codigo != null ? codigo : "");
        txtDescripcion.setText(descripcion != null ? descripcion : "");
        txtEstado.setText(inscrita ? "Ya inscrito" : "No inscrito");
        btnInscribirse.setEnabled(!inscrita);

        btnBack.setOnClickListener(v -> finish());

        btnInscribirse.setOnClickListener(v -> {
            if (materiaId == null) {
                Toast.makeText(this, "Materia inválida", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONArray profesores = profesoresJson != null ? new JSONArray(profesoresJson) : new JSONArray();
                if (profesores.length() == 0) {
                    Toast.makeText(this, "No hay profesores disponibles", Toast.LENGTH_SHORT).show();
                    return;
                }
                String[] nombres = new String[profesores.length()];
                String[] ids = new String[profesores.length()];
                for (int i = 0; i < profesores.length(); i++) {
                    JSONObject prof = profesores.optJSONObject(i);
                    if (prof == null) continue;
                    ids[i] = prof.optString("id", "");
                    nombres[i] = prof.optString("nombre", "Profesor");
                }
                new AlertDialog.Builder(this)
                        .setTitle("Selecciona profesor")
                        .setItems(nombres, (dialog, which) -> inscribirse(materiaId, ids[which]))
                        .setNegativeButton("Cancelar", null)
                        .show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void inscribirse(String materiaId, String profesorId) {
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("id", materiaId);
                params.put("profesorId", profesorId);
                ApiClient.ApiResponse resp = ApiClient.postForm("/api/inscribirse", params);
                JSONObject jsonResp = ApiClient.parseJson(resp.body);
                String msg = jsonResp != null ? jsonResp.optString("msg", "OK") : resp.body;
                runOnUiThread(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
