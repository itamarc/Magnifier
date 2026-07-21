# Magnifier

Magnifier is a simple open source Android app that turns your phone camera into a magnifying glass.

With Magnifier, you can:
- zoom in and out easily;
- turn the flashlight on or off;
- freeze the current image on screen;
- save the current view to your gallery.

## Features

- Camera-based magnification
- On-screen zoom control
- Flashlight toggle
- Freeze and unfreeze image
- Save captured images to the gallery using Android MediaStore

## Requirements

To build the project locally, you need:

- Android Studio
- JDK 17
- Android SDK
- Android SDK Build Tools 35.0.0 or newer

## Build locally

### Using Android Studio

1. Clone the repository:
   ```bash
   git clone https://github.com/itamarc/Magnifier.git
   cd Magnifier
   ```

2. Open the project in Android Studio.

3. Let Gradle sync the project and install any missing SDK components.

4. Run the app on a physical Android device or emulator.

### Using the command line

#### Build debug APK

On Linux/macOS:

```bash
./gradlew assembleDebug
```

On Windows:

```bat
gradlew.bat assembleDebug
```

The generated APK will be available in:

```text
app/build/outputs/apk/debug/
```

#### Build release APK

On Linux/macOS:

```bash
./gradlew assembleRelease
```

On Windows:

```bat
gradlew.bat assembleRelease
```

The generated APK will be available in:

```text
app/build/outputs/apk/release/
```

## Permissions

Magnifier requires camera access to display the live preview and capture images.

## Storage

Saved images are written using the modern Android MediaStore API and stored in the device gallery.

## Notes

- This project uses Gradle and AndroidX.
- The local Android SDK path should be configured in your local environment by Android Studio.
- `local.properties` is machine-specific and should not be committed.

## License

This project is licensed under the GNU General Public License v3.0.

Created by Itamar Carvalho in february 2024.
