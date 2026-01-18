package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlumnoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumno);

        String userId = getIntent().getStringExtra("userId");
        String username = getIntent().getStringExtra("username");
        String fullName = getIntent().getStringExtra("fullName");
        String rol = getIntent().getStringExtra("rol");

        TextView info = findViewById(R.id.txtAlumnoInfo);
        TextView sub = findViewById(R.id.txtSub);
        info.setText(fullName + " (" + username + ")\nRol: " + rol);
        sub.setText("Bienvenido/a · sincronizando perfil...");

        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/me");
                org.json.JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    String name = json.optJSONObject("user").optString("fullName", fullName);
                    runOnUiThread(() -> sub.setText("Bienvenido/a · " + name));
                } else {
                    runOnUiThread(() -> sub.setText("Bienvenido/a · perfil no disponible"));
                }
            } catch (Exception e) {
                runOnUiThread(() -> sub.setText("Bienvenido/a · sin conexión"));
            }
        }).start();

        Button btnMaterias = findViewById(R.id.btnMaterias);
        Button btnMisInscripciones = findViewById(R.id.btnMisInscripciones);
        Button btnReservas = findViewById(R.id.btnReservas);
        Button btnChat = findViewById(R.id.btnChat);
        Button btnConsultas = findViewById(R.id.btnConsultas);
        Button btnMenu = findViewById(R.id.btnMenu);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnMaterias.setOnClickListener(v -> {
            Intent i = new Intent(this, MateriasActivity.class);
            i.putExtra("rol", rol);
            startActivity(i);
        });

        btnMisInscripciones.setOnClickListener(v -> {
            Intent i = new Intent(this, MateriasActivity.class);
            i.putExtra("rol", rol);
            i.putExtra("soloInscritas", true);
            startActivity(i);
        });

        btnReservas.setOnClickListener(v -> {
            Intent i = new Intent(this, ReservasActivity.class);
            i.putExtra("rol", rol);
            startActivity(i);
        });

        btnChat.setOnClickListener(v -> {
            Intent i = new Intent(this, ChatActivity.class);
            i.putExtra("rol", rol);
            i.putExtra("username", username);
            i.putExtra("userId", userId);
            startActivity(i);
        });

        btnConsultas.setOnClickListener(v -> {
            Intent i = new Intent(this, ConsultasActivity.class);
            startActivity(i);
        });

        btnMenu.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, v);
            menu.getMenu().add("Materias");
            menu.getMenu().add("Mis inscripciones");
            menu.getMenu().add("Reservas");
            menu.getMenu().add("Chat");
            menu.getMenu().add("Consultas");
            menu.getMenu().add("Cerrar sesión");
            menu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();
                if ("Materias".equals(title)) {
                    btnMaterias.performClick();
                } else if ("Mis inscripciones".equals(title)) {
                    btnMisInscripciones.performClick();
                } else if ("Reservas".equals(title)) {
                    btnReservas.performClick();
                } else if ("Chat".equals(title)) {
                    btnChat.performClick();
                } else if ("Consultas".equals(title)) {
                    Intent i = new Intent(this, ConsultasActivity.class);
                    startActivity(i);
                } else if ("Cerrar sesión".equals(title)) {
                    btnLogout.performClick();
                }
                return true;
            });
            menu.show();
        });

        btnLogout.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    ApiClient.postForm("/api/logout", new java.util.HashMap<>());
                } catch (Exception ignored) {
                }
            }).start();
            Intent i = new Intent(AlumnoActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
