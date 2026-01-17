# Android WebView Application for Play Framework

This directory contains an Android application that wraps the Play Framework website into a native Android app using WebView.

## Project Structure

```
Android/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       │   └── com/playframework/webapp/
│   │       │       └── MainActivity.java          # Main Activity with WebView
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   └── activity_main.xml         # Main layout with WebView
│   │       │   ├── values/
│   │       │   │   ├── strings.xml               # App strings
│   │       │   │   ├── colors.xml                # Color definitions
│   │       │   │   └── styles.xml                # App theme
│   │       │   ├── mipmap-*/                     # App launcher icons
│   │       │   └── drawable/
│   │       │       └── ic_launcher.xml           # Launcher icon drawable
│   │       ├── AndroidManifest.xml               # App manifest with permissions
│   │       └── proguard-rules.pro                # ProGuard rules for release builds
│   └── build.gradle                              # App-level build configuration
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties             # Gradle wrapper configuration
├── build.gradle                                  # Project-level build configuration
├── settings.gradle                               # Project settings
├── gradle.properties                             # Gradle properties
└── README.md                                     # This file

```

## Features

- **Full WebView Integration**: Displays the Play Framework website within the Android app
- **JavaScript Enabled**: Full support for dynamic web content
- **Network Connectivity Check**: Warns users if there's no internet connection
- **Back Button Navigation**: Hardware back button navigates through web history
- **Responsive Design**: Supports zoom and wide viewport
- **Caching**: Enabled for better performance
- **Error Handling**: Displays error messages for failed page loads

## Prerequisites

Before building the Android app, ensure you have:

1. **Android Studio** (Arctic Fox or later recommended)
2. **JDK 8 or higher**
3. **Android SDK** with:
   - Android SDK Platform 33
   - Android SDK Build-Tools 33.0.0
   - Android SDK Platform-Tools
4. **Gradle 7.5** (included via wrapper)

## Setup Instructions

### 1. Install Android Studio

Download and install Android Studio from: https://developer.android.com/studio

### 2. Open the Project

1. Launch Android Studio
2. Select "Open an Existing Project"
3. Navigate to the `Android` directory in this repository
4. Click "OK"

Android Studio will automatically:
- Download the Gradle wrapper
- Sync Gradle dependencies
- Index the project

### 3. Configure the Server URL

Before running the app, you need to configure the URL of your Play Framework server:

**Edit `MainActivity.java`:**

```java
// For Android Emulator (accessing localhost on your computer):
private static final String WEBSITE_URL = "http://10.0.2.2:9000";

// For Real Device (use your computer's IP address on the local network):
private static final String WEBSITE_URL = "http://192.168.1.XXX:9000";

// For Production (use your actual server URL):
private static final String WEBSITE_URL = "https://your-domain.com";
```

**Important Notes:**
- `10.0.2.2` is the special IP address that Android Emulator uses to refer to the host machine's localhost
- For real devices on the same network, use your computer's local IP address
- Make sure your Play Framework server is running before testing the app

### 4. Running the Play Framework Server

Before running the Android app, start your Play Framework server:

```bash
# Navigate to the Play Framework application directory
cd ../nom_appWeb

# Start the Play server
play run
```

The server should start on `http://localhost:9000`

### 5. Build and Run

**Option A: Using Android Studio**

1. Connect an Android device via USB (with USB Debugging enabled) OR start an Android Emulator
2. Click the "Run" button (green play icon) in the toolbar
3. Select your device/emulator
4. The app will build and install automatically

**Option B: Using Command Line**

```bash
# For debug build
./gradlew assembleDebug

# For release build
./gradlew assembleRelease

# Install on connected device
./gradlew installDebug
```

The APK will be generated in:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release-unsigned.apk`

### 6. Testing on Android Emulator

1. In Android Studio, go to **Tools > Device Manager**
2. Click "Create Device"
3. Select a device definition (e.g., Pixel 4)
4. Select a system image (API 33 recommended)
5. Click "Finish"
6. Start the emulator and run the app

### 7. Testing on Real Device

1. Enable **Developer Options** on your Android device:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging** in Developer Options
3. Connect your device via USB
4. Accept the USB debugging authorization prompt
5. Run the app from Android Studio

## Configuration Options

### Minimum SDK Version

The app supports Android 5.0 (API level 21) and above. To change this:

Edit `app/build.gradle`:
```gradle
minSdkVersion 21  // Change this value
```

### App Name

To change the app name, edit `app/src/main/res/values/strings.xml`:
```xml
<string name="app_name">Your App Name</string>
```

### Theme and Colors

Customize the app theme in `app/src/main/res/values/`:
- `colors.xml` - Color definitions
- `styles.xml` - Theme styles

### Application ID

To change the package name, edit `app/build.gradle`:
```gradle
applicationId "com.your.package.name"
```

And rename the Java package directory accordingly.

## Security Considerations

### Internet Permissions

The app requires internet permission to load web content:
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

### Clear Text Traffic

For development with HTTP (not HTTPS), the app allows clear text traffic:
```xml
android:usesCleartextTraffic="true"
```

**For production apps, always use HTTPS and set this to false.**

### JavaScript Security

JavaScript is enabled in the WebView for full functionality. If you need to expose Android functions to JavaScript, use the `@JavascriptInterface` annotation:

```java
webView.addJavascriptInterface(new WebAppInterface(this), "Android");
```

## Troubleshooting

### App doesn't connect to server

1. Check that the Play Framework server is running
2. Verify the URL in `MainActivity.java`
3. For emulator: use `http://10.0.2.2:9000`
4. For real device: use your computer's local IP address
5. Ensure both devices are on the same network (for local testing)

### Build errors

1. Clean and rebuild: **Build > Clean Project** then **Build > Rebuild Project**
2. Invalidate caches: **File > Invalidate Caches / Restart**
3. Check that you have the correct SDK version installed

### Blank screen or loading issues

1. Check logcat in Android Studio for error messages
2. Verify network connectivity
3. Test the URL in a mobile browser first
4. Check for CORS issues if loading external resources

### Network Security Error (Android 9+)

If you see "NET::ERR_CLEARTEXT_NOT_PERMITTED", add to `AndroidManifest.xml`:
```xml
android:usesCleartextTraffic="true"
```

Or create a network security config for better control.

## Building for Production

### Signing the APK

1. Generate a keystore:
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

2. Add to `app/build.gradle`:
```gradle
android {
    signingConfigs {
        release {
            storeFile file("my-release-key.jks")
            storePassword "your-password"
            keyAlias "my-alias"
            keyPassword "your-password"
        }
    }
    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

3. Build release APK:
```bash
./gradlew assembleRelease
```

### ProGuard/R8 Configuration

ProGuard rules for WebView are already configured in `app/proguard-rules.pro`. For production builds, code shrinking and obfuscation are recommended.

## Advanced Features (Optional)

### Adding a Splash Screen

Create `splash_activity.xml` and `SplashActivity.java` to show a splash screen while the app loads.

### Push Notifications

Integrate Firebase Cloud Messaging (FCM) to send push notifications to users.

### Offline Support

Implement caching strategies and offline page to handle no-network scenarios:
```java
webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
```

### File Upload Support

Add file chooser support for uploading files from the WebView:
```java
webView.setWebChromeClient(new WebChromeClient() {
    // Override file chooser methods
});
```

## Support

For issues related to:
- **Android app**: Check the Android Studio logcat
- **Play Framework**: See the Play Framework documentation
- **WebView**: Refer to Android WebView documentation

## License

This Android wrapper follows the same license as the Play Framework project.

## Additional Resources

- [Android WebView Documentation](https://developer.android.com/reference/android/webkit/WebView)
- [Android Studio User Guide](https://developer.android.com/studio/intro)
- [Play Framework Documentation](https://www.playframework.com/documentation)
- [Building Your First App](https://developer.android.com/training/basics/firstapp)
