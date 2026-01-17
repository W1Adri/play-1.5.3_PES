package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

public class PanelAlumnoActivity extends Activity {

    private User currentUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_panel_alumno);

        TextView greeting = findViewById(R.id.alumno_greeting);
        greeting.setText("Hola, " + currentUser.getFullName());

        Button consultasButton = findViewById(R.id.button_consultas);
        Button reservasButton = findViewById(R.id.button_reservas);
        Button logoutButton = findViewById(R.id.button_logout);

        consultasButton.setOnClickListener(view -> startActivity(new Intent(this, ConsultasActivity.class)));
        reservasButton.setOnClickListener(view -> startActivity(new Intent(this, ReservasActivity.class)));
        logoutButton.setOnClickListener(view -> logout());

        loadPanelData();
    }

    private void loadPanelData() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/panel/alumno", null);
                JSONObject totals = response.optJSONObject("totals");
                JSONArray materias = response.optJSONArray("materias");
                JSONArray inscripciones = response.optJSONArray("inscripciones");
                runOnUiThread(() -> {
                    populateStats(totals);
                    populateMaterias(materias);
                    populateInscripciones(inscripciones);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    populateStats(null);
                });
            }
        }).start();
    }

    private void populateStats(JSONObject totals) {
        TextView statUsuarios = findViewById(R.id.stat_usuarios);
        TextView statAlumnos = findViewById(R.id.stat_alumnos);
        TextView statProfesores = findViewById(R.id.stat_profesores);
        if (totals == null) {
            statUsuarios.setText("-");
            statAlumnos.setText("-");
            statProfesores.setText("-");
            return;
        }
        statUsuarios.setText(String.valueOf(totals.optLong("usuarios")));
        statAlumnos.setText(String.valueOf(totals.optLong("alumnos")));
        statProfesores.setText(String.valueOf(totals.optLong("profesores")));
    }

    private void populateMaterias(JSONArray materias) {
        LinearLayout list = findViewById(R.id.materias_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (materias == null || materias.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Todavía no hay materias disponibles.");
            empty.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            list.addView(empty);
            return;
        }
        for (int i = 0; i < materias.length(); i++) {
            JSONObject materia = materias.optJSONObject(i);
            if (materia == null) continue;
            View card = inflater.inflate(R.layout.item_subject, list, false);
            TextView name = card.findViewById(R.id.subject_name);
            TextView description = card.findViewById(R.id.subject_description);
            TextView code = card.findViewById(R.id.subject_code);
            Button detailButton = card.findViewById(R.id.subject_button);

            int materiaId = materia.optInt("id");
            name.setText(materia.optString("nombre"));
            description.setText(materia.optString("descripcion"));
            code.setText("Código: " + materia.optString("codigo"));
            detailButton.setOnClickListener(view -> openDetail(materiaId));

            list.addView(card);
        }
    }

    private void populateInscripciones(JSONArray inscripciones) {
        LinearLayout list = findViewById(R.id.inscripciones_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        if (inscripciones == null || inscripciones.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Inscríbete en una materia para ver a tus profesores aquí.");
            empty.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            list.addView(empty);
            return;
        }
        for (int i = 0; i < inscripciones.length(); i++) {
            JSONObject inscripcion = inscripciones.optJSONObject(i);
            if (inscripcion == null) continue;
            JSONObject materia = inscripcion.optJSONObject("materia");
            JSONObject profesor = inscripcion.optJSONObject("profesor");
            View card = inflater.inflate(R.layout.item_inscripcion, list, false);
            TextView materiaName = card.findViewById(R.id.inscripcion_materia);
            TextView profesorName = card.findViewById(R.id.inscripcion_profesor);
            Button chatButton = card.findViewById(R.id.inscripcion_chat_button);

            int profesorId = profesor != null ? profesor.optInt("id") : -1;
            materiaName.setText(materia != null ? materia.optString("nombre") : "");
            String profesorFullName = profesor != null ? profesor.optString("fullName") : "";
            profesorName.setText(!profesorFullName.isEmpty() ? "Profesor: " + profesorFullName : "Profesor no asignado");
            chatButton.setOnClickListener(view -> openChat(profesorId, profesorFullName));

            list.addView(card);
        }
    }

    private void openDetail(int materiaId) {
        Intent intent = new Intent(this, MateriaDetailActivity.class);
        intent.putExtra("materia_id", materiaId);
        startActivity(intent);
    }

    private void openChat(int profesorId, String profesorNombre) {
        if (profesorId <= 0) {
            return;
        }
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("alumno_id", currentUser.getId());
        intent.putExtra("profesor_id", profesorId);
        intent.putExtra("alumno_name", currentUser.getFullName());
        intent.putExtra("profesor_name", profesorNombre);
        startActivity(intent);
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
