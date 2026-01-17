# Quick Start Guide

## What This Is

This Android application is a WebView wrapper for the Play Framework website. It allows the website to run as a native Android application without modifying the original website code.

## Prerequisites

- Android Studio (latest version)
- JDK 8 or higher
- Play Framework server running (from ../nom_appWeb)

## Quick Setup (3 Steps)

### 1. Start the Play Server

```bash
cd ../nom_appWeb
play run
```

Server will start at `http://localhost:9000`

### 2. Open in Android Studio

1. Open Android Studio
2. File → Open → Select this `Android` folder
3. Wait for Gradle sync to complete

### 3. Configure URL

Edit `app/src/main/java/com/playframework/webapp/MainActivity.java`:

```java
// Line 23 - Change this URL based on your setup:

// For Android Emulator:
private static final String WEBSITE_URL = "http://10.0.2.2:9000";

// For Real Device (replace with your computer's IP):
private static final String WEBSITE_URL = "http://192.168.1.XXX:9000";

// For Production:
private static final String WEBSITE_URL = "https://your-domain.com";
```

## Run the App

1. Click the green "Run" button in Android Studio
2. Select an emulator or connected device
3. The app will install and launch automatically

## Troubleshooting

**Can't connect to server?**
- Emulator: Use `http://10.0.2.2:9000`
- Real device: Use your computer's local IP address
- Check that the Play server is running

**Build errors?**
- Build → Clean Project
- Build → Rebuild Project
- File → Invalidate Caches / Restart

**Blank screen?**
- Check Android Studio Logcat for errors
- Verify the URL is correct
- Test the URL in a mobile browser first

## Features

- ✅ Full website functionality in Android app
- ✅ JavaScript enabled
- ✅ Back button navigation
- ✅ Network connectivity check
- ✅ Responsive design
- ✅ Caching for better performance

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

## Important Notes

1. **Website code is NOT modified** - The Android app only wraps the existing website
2. **Server must be running** - The app loads content from the Play Framework server
3. **Network required** - The app needs internet/network access to load the website
4. **Development vs Production** - Update the URL in MainActivity.java for production

## Need More Help?

See the detailed [README.md](README.md) for:
- Complete setup instructions
- Configuration options
- Security considerations
- Advanced features
- Production build guide

## File Structure

```
Android/
├── app/
│   ├── src/main/
│   │   ├── java/com/playframework/webapp/
│   │   │   └── MainActivity.java          ← Main app logic (EDIT URL HERE)
│   │   ├── res/
│   │   │   ├── layout/activity_main.xml   ← UI layout
│   │   │   └── values/                     ← Strings, colors, styles
│   │   └── AndroidManifest.xml            ← App permissions
│   └── build.gradle                       ← App dependencies
├── build.gradle                           ← Project config
├── settings.gradle                        ← Project settings
├── gradlew                                ← Build script (Linux/Mac)
├── gradlew.bat                            ← Build script (Windows)
└── README.md                              ← Full documentation
```
