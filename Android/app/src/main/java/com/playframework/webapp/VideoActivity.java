package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

public class VideoActivity extends Activity {

    private User currentUser;
    private int reservaId;
    private TextView statusView;
    private Button joinButton;
    private Button retryButton;
    private Button leaveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_video);

        reservaId = getIntent().getIntExtra("reserva_id", -1);

        TextView title = findViewById(R.id.video_title);
        TextView subtitle = findViewById(R.id.video_subtitle);
        TextView roomCode = findViewById(R.id.video_room_code);

        loadReserva(title, subtitle, roomCode);

        statusView = findViewById(R.id.video_status);
        joinButton = findViewById(R.id.video_join_button);
        retryButton = findViewById(R.id.video_retry_button);
        leaveButton = findViewById(R.id.video_leave_button);

        joinButton.setOnClickListener(view -> joinClass());
        retryButton.setOnClickListener(view -> retryClass());
        leaveButton.setOnClickListener(view -> finish());

        Button backButton = findViewById(R.id.video_back_button);
        Button logoutButton = findViewById(R.id.video_logout_button);
        backButton.setOnClickListener(view -> finish());
        logoutButton.setOnClickListener(view -> logout());
    }

    private void joinClass() {
        statusView.setText("Cámara activada. Esperando al otro participante...");
        joinButton.setEnabled(false);
        retryButton.setEnabled(true);
        leaveButton.setEnabled(true);
    }

    private void retryClass() {
        statusView.setText("Reintentando conexión...");
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void loadReserva(TextView title, TextView subtitle, TextView roomCode) {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/reservas", null);
                JSONArray reservas = response.optJSONArray("reservas");
                if (reservas == null) {
                    runOnUiThread(() -> finish());
                    return;
                }
                for (int i = 0; i < reservas.length(); i++) {
                    JSONObject reserva = reservas.optJSONObject(i);
                    if (reserva != null && reserva.optInt("id") == reservaId) {
                        JSONObject materia = reserva.optJSONObject("materia");
                        JSONObject profesor = reserva.optJSONObject("profesor");
                        JSONObject alumno = reserva.optJSONObject("alumno");
                        String partner = currentUser.getRole() == Role.ALUMNO
                            ? (profesor != null ? profesor.optString("fullName") : "")
                            : (alumno != null ? alumno.optString("fullName") : "");
                        String materiaNombre = materia != null ? materia.optString("nombre") : "";
                        String codigoSala = reserva.optString("codigoSala");
                        runOnUiThread(() -> {
                            title.setText("Clase en vídeo - " + materiaNombre);
                            subtitle.setText("Conecta con " + partner);
                            roomCode.setText("Código de sala: " + codigoSala);
                        });
                        return;
                    }
                }
                runOnUiThread(this::finish);
            } catch (Exception e) {
                runOnUiThread(this::finish);
            }
        }).start();
    }
}
