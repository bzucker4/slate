# Slate

Android app (`com.bzucker4.slate`) built with Kotlin and Jetpack Compose.

## Open in Android Studio

1. Install [Android Studio](https://developer.android.com/studio) (Ladybug or newer is fine).
2. **File → Open** and select this folder (`Slate`).
3. Let Gradle sync finish. If prompted, install the Android SDK, platform **API 36**, and a JDK 17+ (Android Studio’s bundled JBR works).
4. Choose a device or emulator, then click **Run**.

## Run from the command line

From this directory, with the Android SDK installed and `ANDROID_HOME` set (or `local.properties` containing `sdk.dir`):

```bash
./gradlew assembleDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/`.

Install on a connected device:

```bash
./gradlew installDebug
```

## Project layout

Single `:app` module. Application code lives under `app/src/main/java/com/bzucker4/slate/`:

| Package | Role |
| --- | --- |
| `ui/` | Compose screens and theme |
| `scratch/` | Scratch session (placeholder) |
| `haptics/` | Haptics (placeholder) |
| `audio/` | Audio (placeholder) |
| `lockout/` | Lockout (placeholder) |
| `data/` | Persistence (placeholder) |

`MainActivity` hosts Compose and shows `HomeScreen` (title **Slate** and a **Begin** button that is not wired yet).
