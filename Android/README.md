# Android App - Clases online

This directory contains an Android application that wraps the `tt` Play Framework site inside a WebView, so the Android UI matches the website and all web flows (login, reservas, chat, video) continue to work exactly as they do in the browser.

## Project Structure

```
Android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/playframework/webapp/
│   │       │       └── MainActivity.java           # WebView host activity
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml          # WebView + loading/error UI
│   │       │   └── values/
│   │       │       ├── strings.xml                # App name + website URL
│   │       │       └── styles.xml                 # App theme
│   │       └── AndroidManifest.xml                # App manifest + permissions
│   └── build.gradle                               # App-level build configuration
├── gradle/                                        # Gradle wrapper
├── build.gradle                                   # Project-level build configuration
└── settings.gradle                                # Project settings
```

## How It Works

- The Android app hosts a single WebView pointed at the Play Framework server.
- Because the UI is rendered by the website, the Android app matches the `tt` design and supports the same features (login, reservas, chat, video, etc.).
- Camera and microphone permissions are requested automatically when the video room page needs them.

## Configure the Website URL

Edit `Android/app/src/main/res/values/strings.xml` to point the WebView at your server:

```xml
<string name="website_url">http://10.0.2.2:9000</string>
```

- `10.0.2.2` is the Android emulator loopback to your host machine.
- On a physical device, replace it with your machine's LAN IP (e.g., `http://192.168.1.10:9000`).

## Running the App

### Android Studio

1. Open the `Android` directory in Android Studio.
2. Let Gradle sync complete.
3. Run the app on an emulator or connected device.

### Command Line

```bash
./gradlew assembleDebug
```

The APK will be generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

## Notes

- Ensure the Play Framework server for `tt` is running before launching the app.
- Cleartext traffic is enabled for local development over HTTP.
