package com.example.myapplication;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ReservasActivity extends AppCompatActivity {

    static class ReservaItem {
        String id;
        String label;

        ReservaItem(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    static class InscripcionOption {
        String id;
        String label;

        InscripcionOption(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    private ArrayList<ReservaItem> reservas;
    private ArrayList<String> etiquetas;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas);

        String rol = getIntent().getStringExtra("rol");
        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Reservas (" + (rol == null ? "?" : rol) + ")");

        reservas = new ArrayList<>();
        etiquetas = new ArrayList<>();

        ListView list = findViewById(R.id.listReservas);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, etiquetas);
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) -> {
            if (pos < 0 || pos >= reservas.size()) return;
            ReservaItem reserva = reservas.get(pos);
            Intent intent = new Intent(this, VideoCallActivity.class);
            intent.putExtra("reservaId", reserva.id);
            intent.putExtra("rol", rol);
            startActivity(intent);
        });

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnCrear = findViewById(R.id.btnCrear);
        btnCrear.setOnClickListener(v -> mostrarCrearReservaDialog());

        cargarReservas();
    }

    private void cargarReservas() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/reservas");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray arr = json.optJSONArray("reservas");
                    ArrayList<ReservaItem> nuevas = new ArrayList<>();
                    ArrayList<String> etiquetasNuevas = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String id = item.optString("id", "");
                            long fechaMillis = item.optLong("fechaReserva", 0L);
                            String fecha = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    .format(new java.util.Date(fechaMillis));
                            JSONObject materia = item.optJSONObject("materia");
                            JSONObject profesor = item.optJSONObject("profesor");
                            JSONObject alumno = item.optJSONObject("alumno");
                            String materiaNombre = materia != null ? materia.optString("nombre", "") : "";
                            String profesorNombre = profesor != null ? profesor.optString("fullName", "") : "";
                            String alumnoNombre = alumno != null ? alumno.optString("fullName", "") : "";
                            String label = "📅 " + fecha
                                    + "\nMateria: " + materiaNombre
                                    + "\nProfesor: " + profesorNombre
                                    + "\nAlumno: " + alumnoNombre;
                            nuevas.add(new ReservaItem(id, label));
                            etiquetasNuevas.add(label);
                        }
                    }
                    runOnUiThread(() -> {
                        reservas.clear();
                        reservas.addAll(nuevas);
                        etiquetas.clear();
                        etiquetas.addAll(etiquetasNuevas);
                        adapter.notifyDataSetChanged();
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

    private void mostrarCrearReservaDialog() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/inscripciones/opciones");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray arr = json.optJSONArray("inscripciones");
                    ArrayList<InscripcionOption> opciones = new ArrayList<>();
                    ArrayList<String> labels = new ArrayList<>();
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            JSONObject item = arr.optJSONObject(i);
                            if (item == null) continue;
                            String id = item.optString("id", "");
                            JSONObject materia = item.optJSONObject("materia");
                            JSONObject profesor = item.optJSONObject("profesor");
                            JSONObject alumno = item.optJSONObject("alumno");
                            String materiaNombre = materia != null ? materia.optString("nombre", "") : "";
                            String profesorNombre = profesor != null ? profesor.optString("fullName", "") : "";
                            String alumnoNombre = alumno != null ? alumno.optString("fullName", "") : "";
                            String label = materiaNombre + " · " + profesorNombre + " / " + alumnoNombre;
                            opciones.add(new InscripcionOption(id, label));
                            labels.add(label);
                        }
                    }
                    runOnUiThread(() -> {
                        if (opciones.isEmpty()) {
                            Toast.makeText(this, "No hay inscripciones disponibles", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        String[] items = labels.toArray(new String[0]);
                        new AlertDialog.Builder(this)
                                .setTitle("Selecciona inscripción")
                                .setItems(items, (dialog, which) -> seleccionarFechaHora(opciones.get(which)))
                                .setNegativeButton("Cancelar", null)
                                .show();
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

    private void seleccionarFechaHora(InscripcionOption opcion) {
        Calendar calendar = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            new TimePickerDialog(this, (timeView, hourOfDay, minute) -> {
                String hora = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                crearReserva(opcion, fecha, hora);
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void crearReserva(InscripcionOption opcion, String fecha, String hora) {
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                params.put("inscripcionId", opcion.id);
                params.put("fecha", fecha);
                params.put("hora", hora);
                ApiClient.ApiResponse response = ApiClient.postForm("/api/reservas", params);
                JSONObject json = ApiClient.parseJson(response.body);
                String msg = json != null ? json.optString("msg", "OK") : response.body;
                runOnUiThread(() -> {
                    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    cargarReservas();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
