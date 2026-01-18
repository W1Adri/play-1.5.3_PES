package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ChatActivity extends AppCompatActivity {

    private ArrayList<String> mensajes;
    private ArrayAdapter<String> adapter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean activo = false;
    private long lastId = 0;
    private final Set<Long> ids = new HashSet<>();
    private String userId;
    private String rol;
    private String partnerId;
    private String partnerName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String username = getIntent().getStringExtra("username");
        userId = getIntent().getStringExtra("userId");
        rol = getIntent().getStringExtra("rol");

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
            if (text.isEmpty() || partnerId == null) return;

            new Thread(() -> {
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("receptorId", partnerId);
                    params.put("contenido", text);
                    ApiClient.ApiResponse response = ApiClient.postForm("/api/chat/enviar", params);
                    JSONObject json = ApiClient.parseJson(response.body);
                    if (response.code >= 200 && response.code < 300 && json != null && json.has("id")) {
                        long msgId = json.optLong("id", 0L);
                        String emisor = json.optJSONObject("emisor").optString("username", "Yo");
                        String contenido = json.optString("contenido", text);
                        runOnUiThread(() -> {
                            addMensaje(msgId, emisor + ": " + contenido);
                            edt.setText("");
                        });
                    } else {
                        String msg = json != null ? json.optString("error", response.body) : response.body;
                        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            }).start();
        });

        cargarPartners();
    }

    @Override
    protected void onDestroy() {
        activo = false;
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void cargarPartners() {
        new Thread(() -> {
            try {
                ApiClient.ApiResponse response = ApiClient.get("/api/chat/partners");
                JSONObject json = ApiClient.parseJson(response.body);
                if (response.code >= 200 && response.code < 300 && json != null
                        && "ok".equalsIgnoreCase(json.optString("status"))) {
                    JSONArray partners = json.optJSONArray("partners");
                    if (partners == null || partners.length() == 0) {
                        runOnUiThread(() -> Toast.makeText(this, "No hay chats disponibles", Toast.LENGTH_LONG).show());
                        return;
                    }
                    String[] nombres = new String[partners.length()];
                    String[] ids = new String[partners.length()];
                    for (int i = 0; i < partners.length(); i++) {
                        JSONObject partner = partners.optJSONObject(i);
                        ids[i] = partner.optString("id", "");
                        nombres[i] = partner.optString("fullName", partner.optString("username", "Usuario"));
                    }
                    runOnUiThread(() -> {
                        if (partners.length() == 1) {
                            seleccionarPartner(ids[0], nombres[0]);
                        } else {
                            new AlertDialog.Builder(this)
                                    .setTitle("Selecciona chat")
                                    .setItems(nombres, (dialog, which) -> seleccionarPartner(ids[which], nombres[which]))
                                    .setNegativeButton("Cancelar", null)
                                    .show();
                        }
                    });
                } else {
                    String msg = json != null ? json.optString("msg", response.body) : response.body;
                    runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void seleccionarPartner(String id, String nombre) {
        partnerId = id;
        partnerName = nombre;
        TextView sub = findViewById(R.id.txtSub);
        sub.setText("Chat con: " + partnerName);
        activo = true;
        lastId = 0;
        mensajes.clear();
        adapter.notifyDataSetChanged();
        handler.post(this::pollMensajes);
    }

    private void pollMensajes() {
        if (!activo || partnerId == null || userId == null || rol == null) return;
        new Thread(() -> {
            try {
                Map<String, String> params = new HashMap<>();
                String alumnoId = rol.equalsIgnoreCase("ALUMNO") ? userId : partnerId;
                String profesorId = rol.equalsIgnoreCase("PROFESOR") ? userId : partnerId;
                params.put("alumnoId", alumnoId);
                params.put("profesorId", profesorId);
                if (lastId > 0) {
                    params.put("lastId", String.valueOf(lastId));
                }
                ApiClient.ApiResponse response = ApiClient.get("/api/chat/mensajes", params);
                JSONArray arr = new JSONArray(response.body);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject item = arr.optJSONObject(i);
                    if (item == null) continue;
                    long msgId = item.optLong("id", 0L);
                    JSONObject emisor = item.optJSONObject("emisor");
                    String emisorNombre = emisor != null ? emisor.optString("username", "") : "";
                    String contenido = item.optString("contenido", "");
                    if (msgId > 0 && !ids.contains(msgId)) {
                        runOnUiThread(() -> addMensaje(msgId, emisorNombre + ": " + contenido));
                    }
                }
            } catch (Exception ignored) {
            } finally {
                if (activo) {
                    handler.postDelayed(this::pollMensajes, 3000);
                }
            }
        }).start();
    }

    private void addMensaje(long msgId, String texto) {
        ids.add(msgId);
        mensajes.add(texto);
        adapter.notifyDataSetChanged();
        if (msgId > lastId) {
            lastId = msgId;
        }
    }
}
