# Android + Play Framework Architecture

## Overview

This repository contains two separate applications:

- **Play Framework web app** (`tt/`), which serves the web experience.
- **Android app** (`Android/`), which recreates the same UI and flows using native Android screens backed by JSON APIs from Play.

```
┌─────────────────────────────────────────────────────────────────┐
│                        Play Framework                            │
│                      (Website Backend)                           │
│                        tt Directory                              │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                         Android App                              │
│                 (Native Activities + XML UI)                     │
│                         Android/ Directory                       │
└─────────────────────────────────────────────────────────────────┘
```

## How It Works

### Website (`tt/`)
- **Unchanged**: The Play Framework web app keeps its own routes, views, and assets.
- **Web UI**: HTML/CSS renders the same screens as the Android app.

### Android App (`Android/`)
- **Native UI**: Activities and XML layouts recreate the web design (login, panel de alumno/profesor, reservas, consultas, chat y vídeo).
- **Local Sample Data**: `AppData` seeds users, materias, reservas, and chat messages for quick testing.
- **Navigation**: Activities route to each other using explicit intents; all flows run offline.

## Directory Structure

```
play-1.5.3_PES/
│
├── tt/                        ← WEBSITE (Play Framework)
│   ├── app/                   ← Controllers + views
│   ├── public/                ← CSS, JS, images
│   └── conf/                  ← Configuration
│
└── Android/                   ← ANDROID APP (Native)
    ├── app/
    │   ├── src/main/
    │   │   ├── java/           ← Activities + AppData models
    │   │   ├── res/            ← XML layouts, drawables, strings
    │   │   └── AndroidManifest.xml
    │   └── build.gradle
    ├── build.gradle
    ├── gradlew
    └── README.md
```

## Key Benefits

✅ **Native UX**: Screens are built with Android layouts, not HTML.  
✅ **Feature Parity**: The same flows exist on web and Android.  
✅ **Offline Friendly**: Android uses in-memory sample data for quick demos.  

## Building and Running

### Website
```bash
cd tt
play run
# Visit: http://localhost:9000
```

### Android
```bash
cd Android
./gradlew assembleDebug
```

## Future Enhancements

- Connect Android screens to a real API.
- Persist data locally with Room.
- Add notifications and offline sync.
