package com.playframework.webapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MateriaDetailActivity extends Activity {

    private User currentUser;
    private Materia materia;
    private final List<User> profesores = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentUser = SessionStore.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_materia_detail);

        int materiaId = getIntent().getIntExtra("materia_id", -1);
        materia = new Materia(materiaId, "", "", "");

        TextView title = findViewById(R.id.materia_title);
        TextView description = findViewById(R.id.materia_description);
        TextView message = findViewById(R.id.materia_message);
        Spinner profesorSpinner = findViewById(R.id.materia_profesor_spinner);
        Button enrollButton = findViewById(R.id.materia_enroll_button);
        Button backButton = findViewById(R.id.materia_back_button);

        loadMateria(materiaId, title, description, profesorSpinner);

        enrollButton.setOnClickListener(view -> {
            if (currentUser.getRole() != Role.ALUMNO) {
                message.setText("Solo los alumnos pueden inscribirse en materias.");
                message.setVisibility(View.VISIBLE);
                return;
            }
            User profesor = selectedProfesor(profesores, profesorSpinner.getSelectedItemPosition());
            if (profesor == null) {
                message.setText("Selecciona un profesor para continuar.");
                message.setVisibility(View.VISIBLE);
                return;
            }
            new Thread(() -> {
                try {
                    ApiClient api = new ApiClient(this);
                    JSONObject resp = api.post("/api/inscripciones", new java.util.HashMap<String, String>() {{
                        put("materiaId", String.valueOf(materia.getId()));
                        put("profesorId", String.valueOf(profesor.getId()));
                    }});
                    runOnUiThread(() -> {
                        if (resp.optString("error", "").isEmpty()) {
                            message.setText("✅ Inscripción realizada con éxito.");
                        } else {
                            message.setText(resp.optString("error", "No se pudo inscribir."));
                        }
                        message.setVisibility(View.VISIBLE);
                    });
                } catch (Exception e) {
                    runOnUiThread(() -> {
                        message.setText("No se pudo conectar con el servidor.");
                        message.setVisibility(View.VISIBLE);
                    });
                }
            }).start();
        });

        backButton.setOnClickListener(view -> finish());
    }

    private String[] profesorNames(List<User> profesores) {
        String[] names = new String[profesores.size()];
        for (int i = 0; i < profesores.size(); i++) {
            names[i] = profesores.get(i).getFullName();
        }
        return names;
    }

    private User selectedProfesor(List<User> profesores, int index) {
        if (index < 0 || index >= profesores.size()) {
            return null;
        }
        return profesores.get(index);
    }

    private void loadMateria(int materiaId, TextView title, TextView description, Spinner profesorSpinner) {
        new Thread(() -> {
            try {
                ApiClient api = new ApiClient(this);
                JSONObject response = api.get("/api/materias/" + materiaId, null);
                JSONObject materiaJson = response.optJSONObject("materia");
                JSONArray profesoresJson = response.optJSONArray("profesores");
                if (materiaJson != null) {
                    materia.setCodigo(materiaJson.optString("codigo"));
                    materia.setNombre(materiaJson.optString("nombre"));
                    materia.setDescripcion(materiaJson.optString("descripcion"));
                }
                profesores.clear();
                if (profesoresJson != null) {
                    for (int i = 0; i < profesoresJson.length(); i++) {
                        JSONObject profesor = profesoresJson.optJSONObject(i);
                        if (profesor != null) {
                            profesores.add(new User(
                                profesor.optInt("id"),
                                profesor.optString("username"),
                                profesor.optString("email"),
                                profesor.optString("fullName"),
                                Role.PROFESOR
                            ));
                        }
                    }
                }
                runOnUiThread(() -> {
                    title.setText(materia.getNombre());
                    description.setText(materia.getDescripcion());
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        profesorNames(profesores)
                    );
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    profesorSpinner.setAdapter(adapter);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    title.setText("Materia");
                    description.setText("No se pudo cargar el detalle.");
                });
            }
        }).start();
    }
}
