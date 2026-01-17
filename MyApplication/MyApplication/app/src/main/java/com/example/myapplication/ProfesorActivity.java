package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ProfesorActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profesor);

        String username = getIntent().getStringExtra("username");
        String fullName = getIntent().getStringExtra("fullName");
        String rol = getIntent().getStringExtra("rol");

        TextView info = findViewById(R.id.txtProfesorInfo);
        info.setText(fullName + " (" + username + ")\nRol: " + rol);

        Button btnMisAlumnos = findViewById(R.id.btnMisAlumnos);
        Button btnReservas = findViewById(R.id.btnReservas);
        Button btnChat = findViewById(R.id.btnChat);
        Button btnGestion = findViewById(R.id.btnGestion);
        Button btnLogout = findViewById(R.id.btnLogout);

        btnMisAlumnos.setOnClickListener(v -> {
            Intent i = new Intent(this, MateriasActivity.class);
            i.putExtra("rol", rol);
            i.putExtra("modoProfesor", true);
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
            startActivity(i);
        });

        btnGestion.setOnClickListener(v -> {
            // De momento lo dejamos para después
            Intent i = new Intent(this, MateriasActivity.class);
            i.putExtra("rol", rol);
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(ProfesorActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
