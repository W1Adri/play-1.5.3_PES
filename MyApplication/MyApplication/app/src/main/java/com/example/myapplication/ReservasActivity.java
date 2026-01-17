package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ReservasActivity extends AppCompatActivity {

    private ArrayList<String> reservas;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reservas);

        String rol = getIntent().getStringExtra("rol");
        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Reservas (" + (rol == null ? "?" : rol) + ") - mock");

        reservas = new ArrayList<>();
        reservas.add("📅 2026-01-18 10:00\nMateria: Matemáticas\nProfesor: Laura\nAlumno: Roger");
        reservas.add("📅 2026-01-19 16:30\nMateria: Programación I\nProfesor: Carlos\nAlumno: Roger");

        ListView list = findViewById(R.id.listReservas);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, reservas);
        list.setAdapter(adapter);

        list.setOnItemClickListener((p, v, pos, id) ->
                Toast.makeText(this, "Próximo: detalle + vídeo + reset", Toast.LENGTH_SHORT).show()
        );
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        Button btnCrear = findViewById(R.id.btnCrear);
        btnCrear.setOnClickListener(v -> {
            reservas.add(0, "📅 2026-01-20 12:00\nMateria: Inglés Técnico\nProfesor: Marta\nAlumno: Roger");
            adapter.notifyDataSetChanged();
            Toast.makeText(this, "Reserva mock creada", Toast.LENGTH_SHORT).show();
        });
    }
}
