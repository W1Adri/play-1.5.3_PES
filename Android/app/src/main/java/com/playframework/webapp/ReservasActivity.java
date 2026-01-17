package com.playframework.webapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ReservasActivity extends Activity {

    private User currentUser;
    private Spinner inscripcionSpinner;
    private TextView fechaInput;
    private TextView horaInput;
    private LinearLayout reservasList;
    private TextView flashMessage;
    private TextView emptyView;
    private LinearLayout formContainer;
    private List<Inscripcion> opciones = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_reservas);

        inscripcionSpinner = findViewById(R.id.reserva_inscripcion_spinner);
        fechaInput = findViewById(R.id.reserva_fecha_input);
        horaInput = findViewById(R.id.reserva_hora_input);
        reservasList = findViewById(R.id.reserva_list);
        flashMessage = findViewById(R.id.reserva_flash);
        emptyView = findViewById(R.id.reserva_empty);
        formContainer = findViewById(R.id.reserva_form_container);

        loadReservas();

        fechaInput.setOnClickListener(view -> showDatePicker());
        horaInput.setOnClickListener(view -> showTimePicker());

        Button submitButton = findViewById(R.id.reserva_submit_button);
        submitButton.setOnClickListener(view -> createReserva());

        Button backButton = findViewById(R.id.reserva_back_button);
        backButton.setOnClickListener(view -> finish());

        Button logoutButton = findViewById(R.id.reserva_logout_button);
        logoutButton.setOnClickListener(view -> logout());

    }

    private void loadReservas() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/reservas", null);
                JSONArray opcionesJson = response.optJSONArray("opciones");
                JSONArray reservasJson = response.optJSONArray("reservas");
                opciones = parseInscripciones(opcionesJson);
                runOnUiThread(() -> {
                    updateOpciones();
                    renderReservas(reservasJson);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    formContainer.setVisibility(View.GONE);
                    emptyView.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void updateOpciones() {
        if (opciones == null || opciones.isEmpty()) {
            formContainer.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            return;
        }
        formContainer.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            opcionesLabels(opciones)
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inscripcionSpinner.setAdapter(adapter);
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(
            this,
            (view, year, month, dayOfMonth) -> {
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                fechaInput.setText(date);
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        );
        dialog.show();
    }

    private void showTimePicker() {
        Calendar calendar = Calendar.getInstance();
        TimePickerDialog dialog = new TimePickerDialog(
            this,
            (view, hourOfDay, minute) -> {
                String time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);
                horaInput.setText(time);
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        );
        dialog.show();
    }

    private void createReserva() {
        Inscripcion inscripcion = selectedInscripcion();
        String fecha = fechaInput.getText().toString();
        String hora = horaInput.getText().toString();

        if (inscripcion == null || fecha.isEmpty() || hora.isEmpty()) {
            showFlash("Completa todos los campos para reservar.", false);
            return;
        }

        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("inscripcionId", String.valueOf(inscripcion.getId()));
                params.put("fecha", fecha);
                params.put("hora", hora);
                JSONObject response = api.post("/api/reservas", params);
                runOnUiThread(() -> {
                    if (response.optString("error", "").isEmpty()) {
                        showFlash("✅ Reserva creada con éxito.", true);
                        loadReservas();
                    } else {
                        showFlash(response.optString("error", "No se pudo crear la reserva."), false);
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> showFlash("No se pudo conectar con el servidor.", false));
            }
        }).start();
    }

    private void renderReservas(JSONArray reservas) {
        reservasList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (reservas == null || reservas.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("No tienes reservas todavía.");
            empty.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            reservasList.addView(empty);
            return;
        }
        for (int i = 0; i < reservas.length(); i++) {
            JSONObject reserva = reservas.optJSONObject(i);
            if (reserva == null) continue;
            View card = inflater.inflate(R.layout.item_reserva, reservasList, false);
            TextView title = card.findViewById(R.id.reserva_title);
            TextView meta = card.findViewById(R.id.reserva_meta);
            TextView badge = card.findViewById(R.id.reserva_badge);
            Button joinButton = card.findViewById(R.id.reserva_join_button);

            JSONObject materia = reserva.optJSONObject("materia");
            JSONObject profesor = reserva.optJSONObject("profesor");
            JSONObject alumno = reserva.optJSONObject("alumno");
            String partner = currentUser.getRole() == Role.ALUMNO
                ? (profesor != null ? profesor.optString("fullName") : "")
                : (alumno != null ? alumno.optString("fullName") : "");
            title.setText(materia != null ? materia.optString("nombre") : "");
            meta.setText(formatFecha(reserva.optString("fecha")) + " · " + reserva.optString("hora") + " · " + partner);
            badge.setText("Sala " + reserva.optString("codigoSala"));
            int reservaId = reserva.optInt("id");
            joinButton.setOnClickListener(view -> openVideo(reservaId));

            reservasList.addView(card);
        }
    }

    private void openVideo(int reservaId) {
        Intent intent = new Intent(this, VideoActivity.class);
        intent.putExtra("reserva_id", reservaId);
        startActivity(intent);
    }

    private void showFlash(String message, boolean success) {
        flashMessage.setText(message);
        flashMessage.setVisibility(View.VISIBLE);
        flashMessage.setBackgroundResource(success ? R.drawable.bg_message_success : R.drawable.bg_message_error);
    }

    private Inscripcion selectedInscripcion() {
        int index = inscripcionSpinner.getSelectedItemPosition();
        if (index < 0 || index >= opciones.size()) {
            return null;
        }
        return opciones.get(index);
    }

    private String[] opcionesLabels(List<Inscripcion> opciones) {
        String[] labels = new String[opciones.size()];
        for (int i = 0; i < opciones.size(); i++) {
            Inscripcion inscripcion = opciones.get(i);
            if (currentUser.getRole() == Role.ALUMNO) {
                labels[i] = inscripcion.getMateria().getNombre() + " con " + inscripcion.getProfesor().getFullName();
            } else {
                labels[i] = inscripcion.getMateria().getNombre() + " con " + inscripcion.getAlumno().getFullName();
            }
        }
        return labels;
    }

    private String formatFecha(String raw) {
        try {
            java.text.SimpleDateFormat input = new java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            java.text.SimpleDateFormat output = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return output.format(input.parse(raw));
        } catch (Exception e) {
            return raw;
        }
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private List<Inscripcion> parseInscripciones(JSONArray array) {
        List<Inscripcion> list = new ArrayList<>();
        if (array == null) {
            return list;
        }
        for (int i = 0; i < array.length(); i++) {
            JSONObject inscripcion = array.optJSONObject(i);
            if (inscripcion == null) continue;
            JSONObject materiaJson = inscripcion.optJSONObject("materia");
            JSONObject alumnoJson = inscripcion.optJSONObject("alumno");
            JSONObject profesorJson = inscripcion.optJSONObject("profesor");
            if (materiaJson == null || alumnoJson == null || profesorJson == null) {
                continue;
            }
            Materia materia = new Materia(
                materiaJson.optInt("id"),
                materiaJson.optString("codigo"),
                materiaJson.optString("nombre"),
                materiaJson.optString("descripcion")
            );
            User alumno = new User(
                alumnoJson.optInt("id"),
                alumnoJson.optString("username"),
                alumnoJson.optString("email"),
                alumnoJson.optString("fullName"),
                Role.ALUMNO
            );
            User profesor = new User(
                profesorJson.optInt("id"),
                profesorJson.optString("username"),
                profesorJson.optString("email"),
                profesorJson.optString("fullName"),
                Role.PROFESOR
            );
            list.add(new Inscripcion(inscripcion.optInt("id"), materia, alumno, profesor));
        }
        return list;
    }
}
