# Quick Start Guide

## What This Is

This Android application is a **native** version of the `tt` web experience. The screens are built with XML layouts and Java activities instead of a WebView.

## Prerequisites

- Android Studio (latest version)
- JDK 8 or higher
- Play Framework server running (`tt`)

## Quick Setup (3 Steps)

### 1. Open in Android Studio

1. Open Android Studio
2. File → Open → Select this `Android` folder
3. Wait for Gradle sync to complete

### 2. Start the Play server

```bash
cd ../tt
play run
```

If you use the Android emulator, keep the API base URL in `Android/app/src/main/res/values/strings.xml` as:

```xml
<string name="api_base_url">http://10.0.2.2:9000</string>
```

### 3. Run the App

1. Click the green "Run" button in Android Studio
2. Select an emulator or connected device
3. The app will install and launch automatically

### 4. Log in with sample users

The app ships with sample data so you can test right away:

- **Profesor**: `maria / 1234`
- **Alumno**: `nora / 1234`

You can also register new users from the login screen.

## Features

- ✅ Login/registro nativo
- ✅ Panel de alumno/profesor
- ✅ Reservas con acceso a la clase en vídeo (UI sin videollamada real)
- ✅ Consultas (estadísticas)
- ✅ Chat entre alumno/profesor
- ✅ Gestión de usuarios y materias

## Troubleshooting

**Build errors?**
- Build → Clean Project
- Build → Rebuild Project
- File → Invalidate Caches / Restart

**Datos no aparecen?**
- Cierra y vuelve a abrir la app para recargar los datos de ejemplo.

## Building APK

**Debug APK:**
```bash
./gradlew assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

**Release APK:**
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release-unsigned.apk
```

## File Structure

```
Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/playframework/webapp/  ← Activities + AppData
│   │   ├── res/                              ← Layouts + drawables
│   │   └── AndroidManifest.xml               ← App activities
│   └── build.gradle                          ← App config
├── build.gradle
├── settings.gradle
├── gradlew
├── gradlew.bat
└── README.md
```
