package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class MateriasActivity extends AppCompatActivity {

    static class MateriaItem {
        String codigo, nombre, desc;
        boolean inscrita;

        MateriaItem(String c, String n, String d, boolean ins) {
            codigo = c; nombre = n; desc = d; inscrita = ins;
        }

        @Override public String toString() {
            return codigo + " - " + nombre + (inscrita ? " ✅" : "") + "\n" + desc;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_materias);

        String rol = getIntent().getStringExtra("rol");
        boolean soloInscritas = getIntent().getBooleanExtra("soloInscritas", false);
        boolean modoProfesor = getIntent().getBooleanExtra("modoProfesor", false);

        TextView txtSub = findViewById(R.id.txtSub);
        if (modoProfesor) txtSub.setText("Modo profesor: ver materias y alumnos (mock)");
        else if (soloInscritas) txtSub.setText("Tus inscripciones (mock)");
        else txtSub.setText("Lista de materias (mock)");

        ArrayList<MateriaItem> materias = new ArrayList<>();
        materias.add(new MateriaItem("MAT101", "Matemáticas", "Álgebra y geometría.", true));
        materias.add(new MateriaItem("PROG1", "Programación I", "Introducción a Java.", true));
        materias.add(new MateriaItem("ENG202", "Inglés Técnico", "Vocabulario profesional.", false));

        ArrayList<String> items = new ArrayList<>();
        for (MateriaItem m : materias) {
            if (soloInscritas && !m.inscrita) continue;
            items.add(m.toString());
        }

        ListView list = findViewById(R.id.listMaterias);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, items);
        list.setAdapter(adapter);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        list.setOnItemClickListener((parent, view, position, id) -> {
            String selected = items.get(position);
            if (rol != null && rol.equalsIgnoreCase("ALUMNO")) {
                Toast.makeText(this, "Próximo: detalle + inscribirse\n" + selected, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Próximo: ver alumnos de materia\n" + selected, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
