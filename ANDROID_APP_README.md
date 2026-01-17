# Android Application Added

## Summary

The Android application in `Android/` is now a **native** implementation of the `tt` web experience. It uses Java activities and XML layouts while consuming JSON endpoints from the Play server.

## What Was Done

✅ **Native screens** for login, panel alumno/profesor, reservas, consultas, chat y vídeo
✅ **In-memory sample data** to make the app usable right away in Android Studio
✅ **Documentation updated** to reflect the native approach

## Key Features

- **Login/registro** con roles alumno y profesor
- **Panel alumno** con materias y reservas
- **Panel profesor** con estadísticas y acceso a chats
- **Reservas** con acceso a clase en vídeo (solo UI, sin llamada real)
- **Consultas** con estadísticas de usuarios, materias y reservas
- **Gestión académica** para crear/editar usuarios y materias

## Quick Start

### Run the Android App

1. Open Android Studio
2. File → Open → Select the `Android` folder
3. Start the Play server from `tt` (default URL `http://10.0.2.2:9000` for emulator)
4. Run the app on emulator or device
5. Login with sample users:
   - `maria / 1234` (profesor)
   - `nora / 1234` (alumno)

## Documentation

| File | Purpose |
|------|---------|
| `Android/README.md` | Complete setup guide |
| `Android/QUICKSTART.md` | Quick reference for fast setup |
| `Android/ARCHITECTURE.md` | System architecture and design |

## Technical Details

### Android Application
- **Package Name**: `com.playframework.webapp`
- **Minimum SDK**: Android 8.0 (API 26)
- **Target SDK**: Android 13 (API 33)
- **Build System**: Gradle 7.5
- **Language**: Java

## File Structure

```
play-1.5.3_PES/
│
├── tt/                       ← WEBSITE (UNCHANGED)
│   ├── app/
│   ├── public/
│   └── conf/
│
└── Android/                  ← NATIVE ANDROID APP
    ├── app/
    │   ├── src/main/
    │   │   ├── java/com/playframework/webapp/  ← Activities + AppData
    │   │   ├── res/                             ← Layouts + drawables
    │   │   └── AndroidManifest.xml
    │   ├── build.gradle
    │   └── proguard-rules.pro
    ├── build.gradle
    ├── settings.gradle
    ├── gradle.properties
    ├── gradlew
    ├── gradlew.bat
    ├── README.md
    ├── QUICKSTART.md
    └── ARCHITECTURE.md
```

## License

This Android app follows the same license as the Play Framework project.
