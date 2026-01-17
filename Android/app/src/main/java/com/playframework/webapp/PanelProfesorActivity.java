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

public class PanelProfesorActivity extends Activity {

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
        setContentView(R.layout.activity_panel_profesor);

        TextView greeting = findViewById(R.id.profesor_greeting);
        greeting.setText("Hola, " + currentUser.getFullName());

        Button consultasButton = findViewById(R.id.button_consultas);
        Button gestionButton = findViewById(R.id.button_gestion);
        Button reservasButton = findViewById(R.id.button_reservas);
        Button logoutButton = findViewById(R.id.button_logout);

        consultasButton.setOnClickListener(view -> startActivity(new Intent(this, ConsultasActivity.class)));
        gestionButton.setOnClickListener(view -> startActivity(new Intent(this, GestionActivity.class)));
        reservasButton.setOnClickListener(view -> startActivity(new Intent(this, ReservasActivity.class)));
        logoutButton.setOnClickListener(view -> logout());

        loadPanelData();
    }

    private void loadPanelData() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/panel/profesor", null);
                JSONObject totals = response.optJSONObject("totals");
                JSONArray alumnos = response.optJSONArray("alumnos");
                runOnUiThread(() -> {
                    populateStats(totals);
                    populateAlumnos(alumnos);
                });
            } catch (Exception e) {
                runOnUiThread(() -> populateStats(null));
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

    private void populateAlumnos(JSONArray alumnos) {
        LinearLayout list = findViewById(R.id.alumnos_list);
        list.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);

        if (alumnos == null || alumnos.length() == 0) {
            TextView empty = new TextView(this);
            empty.setText("Aún no tienes alumnos inscritos.");
            empty.setTextAppearance(this, android.R.style.TextAppearance_Medium);
            list.addView(empty);
            return;
        }

        for (int i = 0; i < alumnos.length(); i++) {
            JSONObject inscripcion = alumnos.optJSONObject(i);
            if (inscripcion == null) continue;
            JSONObject alumno = inscripcion.optJSONObject("alumno");
            JSONObject materia = inscripcion.optJSONObject("materia");
            View card = inflater.inflate(R.layout.item_student, list, false);
            TextView name = card.findViewById(R.id.student_name);
            TextView username = card.findViewById(R.id.student_username);
            Button chatButton = card.findViewById(R.id.student_chat_button);

            if (alumno != null) {
                String alumnoName = alumno.optString("fullName");
                name.setText(alumnoName);
                username.setText("Usuario: " + alumno.optString("username")
                    + (materia != null ? " · " + materia.optString("nombre") : ""));
                int alumnoId = alumno.optInt("id");
                chatButton.setOnClickListener(view -> openChat(alumnoId, alumnoName));
            }

            list.addView(card);
        }
    }

    private void openChat(int alumnoId, String alumnoNombre) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("alumno_id", alumnoId);
        intent.putExtra("profesor_id", currentUser.getId());
        intent.putExtra("alumno_name", alumnoNombre);
        intent.putExtra("profesor_name", currentUser.getFullName());
        startActivity(intent);
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
