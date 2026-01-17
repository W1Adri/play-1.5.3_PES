package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlumnoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alumno);

        String username = getIntent().getStringExtra("username");
        String fullName = getIntent().getStringExtra("fullName");
        String rol = getIntent().getStringExtra("rol");

        TextView info = findViewById(R.id.txtAlumnoInfo);
        info.setText(fullName + " (" + username + ")\nRol: " + rol);

        Button btnMaterias = findViewById(R.id.btnMaterias);
        Button btnMisInscripciones = findViewById(R.id.btnMisInscripciones);
        Button btnReservas = findViewById(R.id.btnReservas);
        Button btnChat = findViewById(R.id.btnChat);
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
            startActivity(i);
        });

        btnLogout.setOnClickListener(v -> {
            Intent i = new Intent(AlumnoActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        });
    }
}
