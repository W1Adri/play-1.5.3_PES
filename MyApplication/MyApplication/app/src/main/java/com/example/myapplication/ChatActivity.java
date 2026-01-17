package com.example.myapplication;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private ArrayList<String> mensajes;
    private ArrayAdapter<String> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String username = getIntent().getStringExtra("username");
        String rol = getIntent().getStringExtra("rol");

        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Usuario: " + username + " · Rol: " + rol + " (mock)");

        mensajes = new ArrayList<>();
        mensajes.add("Carlos: Hola, ¿podemos ver derivadas mañana?");
        mensajes.add((username == null ? "Yo" : username) + ": Sí, a las 17:00 me va bien.");

        ListView list = findViewById(R.id.listChat);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, mensajes);
        list.setAdapter(adapter);

        EditText edt = findViewById(R.id.edtMsg);
        Button btn = findViewById(R.id.btnSend);
        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btn.setOnClickListener(v -> {
            String text = edt.getText().toString().trim();
            if (text.isEmpty()) return;

            mensajes.add((username == null ? "Yo" : username) + ": " + text);
            adapter.notifyDataSetChanged();
            edt.setText("");

            Toast.makeText(this, "Enviado (mock)", Toast.LENGTH_SHORT).show();
        });
    }
}
