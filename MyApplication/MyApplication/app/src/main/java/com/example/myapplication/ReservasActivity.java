package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class ReservasActivity extends AppCompatActivity {

    private ArrayList<ReservaItem> reservas;
    private ArrayAdapter<String> adapter;
    private ArrayList<String> items;

    static class ReservaItem {
        String id;
        String materia;
        String profesor;
        String alumno;
        String fecha;

        @Override
        public String toString() {
            return "📅 " + fecha + "\nMateria: " + materia + "\nProfesor: " + profesor + "\nAlumno: " + alumno;
        }
    }

    static class InscripcionItem {
        String id;
        String materia;
        String profesor;

        @Override
        public String toString() {
            return materia + " · " + profesor;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas);

        String rol = getIntent().getStringExtra("rol");
        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Reservas (" + (rol == null ? "?" : rol) + ")");

        ListView list = findViewById(R.id.listReservas);
        reservas = new ArrayList<>();
        items = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) -> {
            ReservaItem seleccion = reservas.get(pos);
            mostrarDetalleReserva(seleccion.id);
        });
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnCrear = findViewById(R.id.btnCrear);
        btnCrear.setOnClickListener(v -> mostrarDialogoReserva());

        cargarReservas();
    }

    private void cargarReservas() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/reservas");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    reservas.clear();
                    items.clear();
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject obj = json.optJSONObject(i);
                        if (obj == null) continue;
                        ReservaItem item = new ReservaItem();
                        item.id = obj.optString("id", "");
                        item.fecha = obj.optString("fecha", "");
                        JSONObject materia = obj.optJSONObject("materia");
                        JSONObject profesor = obj.optJSONObject("profesor");
                        JSONObject alumno = obj.optJSONObject("alumno");
                        item.materia = materia == null ? "" : materia.optString("nombre", "");
                        item.profesor = profesor == null ? "" : profesor.optString("fullName", profesor.optString("username", ""));
                        item.alumno = alumno == null ? "" : alumno.optString("fullName", alumno.optString("username", ""));
                        reservas.add(item);
                        items.add(item.toString());
                    }
                    runOnUiThread(adapter::notifyDataSetChanged);
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudieron cargar reservas", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarDialogoReserva() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/inscripciones");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code < 200 || response.code >= 300 || json == null) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay inscripciones disponibles.", Toast.LENGTH_SHORT).show());
                    return;
                }
                ArrayList<InscripcionItem> inscripciones = new ArrayList<>();
                for (int i = 0; i < json.length(); i++) {
                    JSONObject obj = json.optJSONObject(i);
                    if (obj == null) continue;
                    JSONObject materia = obj.optJSONObject("materia");
                    JSONObject profesor = obj.optJSONObject("profesor");
                    InscripcionItem item = new InscripcionItem();
                    item.id = obj.optString("id", "");
                    item.materia = materia == null ? "" : materia.optString("nombre", "");
                    item.profesor = profesor == null ? "" : profesor.optString("fullName", profesor.optString("username", ""));
                    inscripciones.add(item);
                }
                runOnUiThread(() -> mostrarFormularioReserva(inscripciones));
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarFormularioReserva(ArrayList<InscripcionItem> inscripciones) {
        if (inscripciones.isEmpty()) {
            Toast.makeText(this, "No hay inscripciones disponibles.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[inscripciones.size()];
        for (int i = 0; i < inscripciones.size(); i++) {
            labels[i] = inscripciones.get(i).toString();
        }

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(24, 12, 24, 12);

        EditText edtFecha = new EditText(this);
        edtFecha.setHint("Fecha (YYYY-MM-DD)");
        edtFecha.setInputType(InputType.TYPE_CLASS_DATETIME);

        EditText edtHora = new EditText(this);
        edtHora.setHint("Hora (HH:MM)");
        edtHora.setInputType(InputType.TYPE_CLASS_DATETIME);

        layout.addView(edtFecha);
        layout.addView(edtHora);

        new AlertDialog.Builder(this)
            .setTitle("Crear reserva")
            .setSingleChoiceItems(labels, 0, null)
            .setView(layout)
            .setPositiveButton("Crear", (dialog, which) -> {
                AlertDialog alert = (AlertDialog) dialog;
                int selected = alert.getListView().getCheckedItemPosition();
                InscripcionItem inscripcion = inscripciones.get(selected);
                crearReserva(inscripcion.id, edtFecha.getText().toString().trim(), edtHora.getText().toString().trim());
            })
            .setNegativeButton("Cancelar", null)
            .show();
    }

    private void crearReserva(String inscripcionId, String fecha, String hora) {
        if (fecha.isEmpty() || hora.isEmpty()) {
            Toast.makeText(this, "Indica fecha y hora.", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                JSONObject payload = new JSONObject();
                payload.put("inscripcionId", inscripcionId);
                payload.put("fecha", fecha);
                payload.put("hora", hora);
                ApiClient.ApiResponse response = ApiClient.postJson("/api/reservas", payload);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null && "ok".equalsIgnoreCase(json.optString("status"))) {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Reserva creada.", Toast.LENGTH_SHORT).show();
                        cargarReservas();
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

    private void mostrarDetalleReserva(String reservaId) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/reservas/" + reservaId);
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    String fecha = json.optString("fecha", "");
                    JSONObject materia = json.optJSONObject("materia");
                    JSONObject profesor = json.optJSONObject("profesor");
                    JSONObject alumno = json.optJSONObject("alumno");
                    String codigo = json.optString("codigoSala", "");
                    String detalle = "Fecha: " + fecha
                        + "\nMateria: " + (materia == null ? "" : materia.optString("nombre", ""))
                        + "\nProfesor: " + (profesor == null ? "" : profesor.optString("fullName", profesor.optString("username", "")))
                        + "\nAlumno: " + (alumno == null ? "" : alumno.optString("fullName", alumno.optString("username", "")))
                        + "\nCódigo sala: " + codigo;
                    runOnUiThread(() -> new AlertDialog.Builder(this)
                        .setTitle("Detalle de reserva")
                        .setMessage(detalle)
                        .setPositiveButton("Iniciar videollamada", (dialog, which) -> {
                            android.content.Intent intent = new android.content.Intent(this, VideoCallActivity.class);
                            intent.putExtra("reservaId", reservaId);
                            intent.putExtra("rol", getIntent().getStringExtra("rol"));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cerrar", null)
                        .show());
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "No se pudo cargar el detalle", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
