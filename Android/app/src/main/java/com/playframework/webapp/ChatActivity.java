package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends Activity {

    private User currentUser;
    private int alumnoId;
    private int profesorId;
    private LinearLayout chatList;
    private EditText messageInput;
    private long lastMessageId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_chat);

        alumnoId = getIntent().getIntExtra("alumno_id", -1);
        profesorId = getIntent().getIntExtra("profesor_id", -1);
        if (currentUser.getRole() == Role.PROFESOR && profesorId <= 0) {
            profesorId = currentUser.getId();
        }

        TextView subtitle = findViewById(R.id.chat_subtitle);
        String alumnoName = getIntent().getStringExtra("alumno_name");
        String profesorName = getIntent().getStringExtra("profesor_name");
        if (alumnoName != null && profesorName != null) {
            subtitle.setText(alumnoName + " · " + profesorName);
        } else {
            subtitle.setText("Conversación de clase");
        }

        Button backButton = findViewById(R.id.chat_back_button);
        Button logoutButton = findViewById(R.id.chat_logout_button);

        backButton.setOnClickListener(view -> finish());
        logoutButton.setOnClickListener(view -> logout());

        chatList = findViewById(R.id.chat_list);
        messageInput = findViewById(R.id.chat_message_input);
        Button sendButton = findViewById(R.id.chat_send_button);
        sendButton.setOnClickListener(view -> sendMessage());

        loadMessages();
    }

    private void loadMessages() {
        chatList.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                Map<String, String> params = new HashMap<>();
                params.put("alumnoId", String.valueOf(alumnoId));
                params.put("profesorId", String.valueOf(profesorId));
                params.put("lastId", String.valueOf(lastMessageId));
                JSONObject response = api.get("/api/chat/mensajes", params);
                JSONArray messages = response.optJSONArray("data");
                runOnUiThread(() -> renderMessages(messages, inflater));
            } catch (Exception e) {
                runOnUiThread(() -> renderMessages(new JSONArray(), inflater));
            }
        }).start();
    }

    private void renderMessages(JSONArray messages, LayoutInflater inflater) {
        chatList.removeAllViews();
        if (messages == null) {
            return;
        }
        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i);
            if (message == null) continue;
            JSONObject author = message.optJSONObject("emisor");
            String text = message.optString("contenido");
            String time = message.optString("fecha");
            int authorId = author != null ? author.optInt("id") : -1;
            boolean isMine = authorId == currentUser.getId();

            View bubble = inflater.inflate(R.layout.item_chat_bubble, chatList, false);
            TextView meta = bubble.findViewById(R.id.chat_meta);
            TextView body = bubble.findViewById(R.id.chat_body);
            meta.setText((author != null ? author.optString("username") : "") + " · " + time);
            body.setText(text);
            bubble.setBackgroundResource(isMine ? R.drawable.bg_chat_bubble_me : R.drawable.bg_chat_bubble_other);
            body.setTextColor(getColor(isMine ? R.color.textOnPrimary : R.color.textPrimary));
            meta.setTextColor(getColor(isMine ? R.color.textOnPrimary : R.color.textSecondary));
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) bubble.getLayoutParams();
            params.gravity = isMine ? android.view.Gravity.END : android.view.Gravity.START;
            bubble.setLayoutParams(params);

            long messageId = message.optLong("id");
            if (messageId > lastMessageId) {
                lastMessageId = messageId;
            }

            chatList.addView(bubble);
        }
    }

    private void sendMessage() {
        String text = messageInput.getText().toString().trim();
        if (text.isEmpty()) {
            return;
        }
        messageInput.setText("");
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                Map<String, String> params = new HashMap<>();
                int receptorId = currentUser.getRole() == Role.PROFESOR ? alumnoId : profesorId;
                params.put("receptorId", String.valueOf(receptorId));
                params.put("contenido", text);
                api.post("/api/chat/enviar", params);
                loadMessages();
            } catch (Exception e) {
                runOnUiThread(this::loadMessages);
            }
        }).start();
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
