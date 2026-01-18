package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ChatActivity extends AppCompatActivity {

    private ArrayList<String> mensajes;
    private ArrayAdapter<String> adapter;
    private String alumnoId;
    private String profesorId;
    private String rol;
    private long lastMessageId = 0L;

    private static class ChatTarget {
        final String id;
        final String label;

        ChatTarget(String id, String label) {
            this.id = id;
            this.label = label;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String username = getIntent().getStringExtra("username");
        rol = getIntent().getStringExtra("rol");
        String userId = getIntent().getStringExtra("userId");

        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Usuario: " + username + " · Rol: " + rol);

        mensajes = new ArrayList<>();

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

            if (alumnoId == null || profesorId == null) {
                Toast.makeText(this, "Selecciona un chat primero.", Toast.LENGTH_SHORT).show();
                return;
            }

            String receptorId = "ALUMNO".equalsIgnoreCase(rol) ? profesorId : alumnoId;
            new Thread(() -> {
                try {
                    JSONObject payload = new JSONObject();
                    payload.put("receptorId", receptorId);
                    payload.put("contenido", text);
                    ApiClient.ApiResponse response = ApiClient.postJson("/api/chat/enviar", payload);
                    JSONObject json = ApiClient.parseJson(response.body);
                    if (response.code >= 200 && response.code < 300 && json != null && !json.has("error")) {
                        String emisor = json.optJSONObject("emisor").optString("username", username == null ? "Yo" : username);
                        String contenido = json.optString("contenido", text);
                        long msgId = json.optLong("id", 0L);
                        runOnUiThread(() -> {
                            mensajes.add(emisor + ": " + contenido);
                            adapter.notifyDataSetChanged();
                            edt.setText("");
                        });
                        if (msgId > lastMessageId) {
                            lastMessageId = msgId;
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Error enviando mensaje", Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        cargarChatsDisponibles(userId);
    }

    private void cargarChatsDisponibles(String userId) {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/inscripciones");
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code < 200 || response.code >= 300 || json == null) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay chats disponibles.", Toast.LENGTH_SHORT).show());
                    return;
                }

                Map<String, ChatTarget> targets = new LinkedHashMap<>();
                for (int i = 0; i < json.length(); i++) {
                    JSONObject item = json.optJSONObject(i);
                    if (item == null) continue;
                    JSONObject alumno = item.optJSONObject("alumno");
                    JSONObject profesor = item.optJSONObject("profesor");
                    if (alumno == null || profesor == null) continue;

                    if ("ALUMNO".equalsIgnoreCase(rol)) {
                        String id = profesor.optString("id", "");
                        String label = profesor.optString("fullName", profesor.optString("username", "Profesor"));
                        targets.put(id, new ChatTarget(id, label));
                    } else {
                        String id = alumno.optString("id", "");
                        String label = alumno.optString("fullName", alumno.optString("username", "Alumno"));
                        targets.put(id, new ChatTarget(id, label));
                    }
                }

                if (targets.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "No hay chats disponibles.", Toast.LENGTH_SHORT).show());
                    return;
                }

                ArrayList<ChatTarget> list = new ArrayList<>(targets.values());
                if (list.size() == 1) {
                    seleccionarChat(userId, list.get(0));
                } else {
                    runOnUiThread(() -> mostrarSelectorChat(userId, list));
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void mostrarSelectorChat(String userId, ArrayList<ChatTarget> targets) {
        String[] labels = new String[targets.size()];
        for (int i = 0; i < targets.size(); i++) {
            labels[i] = targets.get(i).label;
        }

        new AlertDialog.Builder(this)
            .setTitle("Selecciona un chat")
            .setItems(labels, (dialog, which) -> seleccionarChat(userId, targets.get(which)))
            .setCancelable(false)
            .show();
    }

    private void seleccionarChat(String userId, ChatTarget target) {
        if ("ALUMNO".equalsIgnoreCase(rol)) {
            alumnoId = userId;
            profesorId = target.id;
        } else {
            alumnoId = target.id;
            profesorId = userId;
        }
        lastMessageId = 0L;
        mensajes.clear();
        adapter.notifyDataSetChanged();
        cargarMensajes();
    }

    private void cargarMensajes() {
        if (alumnoId == null || profesorId == null) return;
        new Thread(() -> {
            try {
                String path = "/api/chat/mensajes?alumnoId=" + alumnoId + "&profesorId=" + profesorId;
                if (lastMessageId > 0) {
                    path = path + "&lastId=" + lastMessageId;
                }
                ApiClient.ApiResponse response = ApiClient.get(path);
                JSONArray json = ApiClient.parseJsonArray(response.body);
                if (response.code >= 200 && response.code < 300 && json != null) {
                    ArrayList<String> nuevos = new ArrayList<>();
                    long maxId = lastMessageId;
                    for (int i = 0; i < json.length(); i++) {
                        JSONObject msg = json.optJSONObject(i);
                        if (msg == null) continue;
                        String emisor = msg.optJSONObject("emisor").optString("username", "Usuario");
                        String contenido = msg.optString("contenido", "");
                        long msgId = msg.optLong("id", 0L);
                        nuevos.add(emisor + ": " + contenido);
                        if (msgId > maxId) maxId = msgId;
                    }
                    long finalMaxId = maxId;
                    runOnUiThread(() -> {
                        mensajes.addAll(nuevos);
                        adapter.notifyDataSetChanged();
                        lastMessageId = finalMaxId;
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
