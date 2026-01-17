package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsultasActivity extends Activity {

    private User currentUser;
    private Spinner tipoSpinner;
    private Spinner materiaSpinner;
    private TextView materiaLabel;
    private LinearLayout resultsList;
    private TextView resultNumber;
    private TextView resultTitle;
    private TextView resultMessage;
    private final List<String> materiaIds = new java.util.ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_consultas);

        tipoSpinner = findViewById(R.id.consulta_tipo_spinner);
        materiaSpinner = findViewById(R.id.consulta_materia_spinner);
        materiaLabel = findViewById(R.id.consulta_materia_label);
        resultsList = findViewById(R.id.consulta_results_list);
        resultNumber = findViewById(R.id.consulta_result_number);
        resultTitle = findViewById(R.id.consulta_result_title);
        resultMessage = findViewById(R.id.consulta_message);

        ArrayAdapter<String> tipoAdapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[]{
                "Usuarios totales",
                "Alumnos registrados",
                "Profesores registrados",
                "Profesores por materia",
                "Alumnos por materia",
                "Reservas por materia",
                "Top alumnos",
                "Top profesores"
            }
        );
        tipoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        tipoSpinner.setAdapter(tipoAdapter);
        tipoSpinner.setOnItemSelectedListener(new SimpleItemSelectedListener(position -> updateMateriaVisibility()));

        loadMaterias();

        Button runButton = findViewById(R.id.consulta_button);
        runButton.setOnClickListener(view -> runQuery());

        Button backButton = findViewById(R.id.consulta_back_button);
        backButton.setOnClickListener(view -> finish());

        Button logoutButton = findViewById(R.id.consulta_logout_button);
        logoutButton.setOnClickListener(view -> logout());

        updateMateriaVisibility();
    }

    private void runQuery() {
        int tipoIndex = tipoSpinner.getSelectedItemPosition();
        String tipo = tipoValue(tipoIndex);
        resultsList.removeAllViews();
        resultMessage.setVisibility(View.GONE);

        if (tipo == null) {
            return;
        }
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                Map<String, String> params = new HashMap<>();
                params.put("tipo", tipo);
                int materiaIndex = materiaSpinner.getSelectedItemPosition();
                if (materiaIndex >= 0 && materiaIndex < materiaIds.size()) {
                    String materiaId = materiaIds.get(materiaIndex);
                    if (materiaId != null && !materiaId.isEmpty()) {
                        params.put("materiaId", materiaId);
                    }
                }
                JSONObject response = api.get("/api/consultas", params);
                runOnUiThread(() -> renderConsulta(response));
            } catch (Exception e) {
                runOnUiThread(() -> showMessage("No se pudo conectar con el servidor."));
            }
        }).start();
    }

    private void setNumberResult(String title, int value) {
        resultTitle.setText(title);
        resultNumber.setText(String.valueOf(value));
        resultNumber.setVisibility(View.VISIBLE);
    }

    private void showMessage(String message) {
        resultNumber.setVisibility(View.GONE);
        resultsList.removeAllViews();
        resultMessage.setText(message);
        resultMessage.setVisibility(View.VISIBLE);
    }

    private void logout() {
        SessionStore.clear();
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void updateMateriaVisibility() {
        int tipoIndex = tipoSpinner.getSelectedItemPosition();
        boolean showMateria = tipoIndex == 3 || tipoIndex == 4 || tipoIndex == 5;
        int visibility = showMateria ? View.VISIBLE : View.GONE;
        materiaSpinner.setVisibility(visibility);
        materiaLabel.setVisibility(visibility);
    }

    private void loadMaterias() {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/materias", null);
                JSONArray materias = response.optJSONArray("materias");
                runOnUiThread(() -> bindMaterias(materias));
            } catch (Exception e) {
                runOnUiThread(() -> bindMaterias(null));
            }
        }).start();
    }

    private void bindMaterias(JSONArray materias) {
        materiaIds.clear();
        if (materias == null) {
            materias = new JSONArray();
        }
        String[] names = new String[materias.length()];
        for (int i = 0; i < materias.length(); i++) {
            JSONObject materia = materias.optJSONObject(i);
            if (materia == null) continue;
            names[i] = materia.optString("nombre");
            materiaIds.add(String.valueOf(materia.optInt("id")));
        }
        ArrayAdapter<String> materiaAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        materiaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        materiaSpinner.setAdapter(materiaAdapter);
    }

    private void renderConsulta(JSONObject response) {
        if (response == null) {
            showMessage("No se pudo obtener resultados.");
            return;
        }
        String error = response.optString("error", "");
        if (!error.isEmpty()) {
            showMessage(error);
            return;
        }
        resultTitle.setText(response.optString("descripcion", "Resultados"));
        if (!response.isNull("resultado")) {
            resultNumber.setVisibility(View.VISIBLE);
            resultNumber.setText(String.valueOf(response.optLong("resultado")));
        } else {
            resultNumber.setVisibility(View.GONE);
        }
        resultsList.removeAllViews();
        JSONArray detalleUsuarios = response.optJSONArray("detalleUsuarios");
        JSONArray ranking = response.optJSONArray("ranking");
        LayoutInflater inflater = LayoutInflater.from(this);
        if (detalleUsuarios != null && detalleUsuarios.length() > 0) {
            for (int i = 0; i < detalleUsuarios.length(); i++) {
                JSONObject user = detalleUsuarios.optJSONObject(i);
                if (user == null) continue;
                View chip = inflater.inflate(R.layout.item_chip, resultsList, false);
                TextView text = chip.findViewById(R.id.chip_text);
                text.setText(user.optString("fullName"));
                resultsList.addView(chip);
            }
            return;
        }
        if (ranking != null && ranking.length() > 0) {
            for (int i = 0; i < ranking.length(); i++) {
                JSONObject item = ranking.optJSONObject(i);
                if (item == null) continue;
                View card = inflater.inflate(R.layout.item_result_card, resultsList, false);
                TextView name = card.findViewById(R.id.result_title);
                TextView subtitle = card.findViewById(R.id.result_subtitle);
                name.setText(item.optString("label"));
                subtitle.setText("Total: " + item.optString("count"));
                resultsList.addView(card);
            }
        }
    }

    private String tipoValue(int index) {
        switch (index) {
            case 0:
                return "totalUsuarios";
            case 1:
                return "totalAlumnos";
            case 2:
                return "totalProfesores";
            case 3:
                return "profesoresMateria";
            case 4:
                return "alumnosMateria";
            case 5:
                return "reservasPorMateria";
            case 6:
                return "topAlumnos";
            case 7:
                return "topProfesores";
            default:
                return null;
        }
    }
}
