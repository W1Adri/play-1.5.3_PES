# Android App - Clases online

This directory contains a native Android application that recreates the `tt` Play Framework UI as Android screens. It mirrors the same flows (login, panel de alumno/profesor, reservas, consultas, chat y vídeo) without embedding the website.

## Project Structure

```
Android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/playframework/webapp/
│   │       │       ├── MainActivity.java           # Native login/registro
│   │       │       ├── PanelAlumnoActivity.java    # Panel alumno
│   │       │       ├── PanelProfesorActivity.java  # Panel profesor
│   │       │       ├── ReservasActivity.java       # Reservas + vídeo
│   │       │       ├── ConsultasActivity.java      # Consultas
│   │       │       ├── GestionActivity.java        # Gestión académica
│   │       │       ├── ChatActivity.java           # Chat nativo
│   │       │       └── VideoActivity.java          # Vista de vídeo
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml          # Login/registro UI
│   │       │   └── values/
│   │       │       ├── strings.xml                # App name
│   │       │       └── styles.xml                 # App theme
│   │       └── AndroidManifest.xml                # App manifest + activities
│   └── build.gradle                               # App-level build configuration
├── gradle/                                        # Gradle wrapper
├── build.gradle                                   # Project-level build configuration
└── settings.gradle                                # Project settings
```

## How It Works

- The Android app uses native activities and XML layouts to replicate the `tt` UI.
- The Android app consumes JSON endpoints from the Play Framework server to load users, materias, reservas, consultas y chat.
- The video screen mirrors the web layout and provides the controls/state flow without needing a WebView (no real call implementation).

## Running the App

### Android Studio

1. Open the `Android` directory in Android Studio.
2. Let Gradle sync complete.
3. Run the app on an emulator or connected device.

Before launching the app, ensure the Play server is running (from `tt`) and update the API base URL if needed:

```xml
<string name="api_base_url">http://10.0.2.2:9000</string>
```

### Command Line

```bash
./gradlew assembleDebug
```

The APK will be generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- The app is self-contained and does not require running the Play Framework server.
- Sample users are preloaded so you can log in with `maria / 1234` (profesor) or `nora / 1234` (alumno).
