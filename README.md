# Slate

Android app (`com.bzucker4.slate`) built with Kotlin and Jetpack Compose.

Scratch frost off the screen, then sit in blackout until the timer ends. Lockout is stored in DataStore so it survives process death.

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

## How to test lockout

1. On the pre-session screen, pick **30m** (shortest), then **Begin**.
2. Scratch the frost until it dissolves (about 85% cleared). You should hear the chime and land on **Blackout**.
3. **Process death:** from Blackout, stop the app (Android Studio **Stop**, or swipe it from Recents / Force stop). Open Slate again. You should still see Blackout and the remaining countdown — not the pre-session screen.
4. **Pocket mute:** with **Blackout hum** enabled in Settings, cover the proximity sensor (or pocket the phone). The hum should mute; uncover to restore it. Emulators often have no proximity sensor.
5. There is **no Emergency Exit on Blackout**. Wait for the timer, or after you are back on pre-session open **Settings → Emergency Exit** to clear a leftover lockout flag.
6. First launch shows a dismissible tip on the pre-session screen. **Got it** stores that you dismissed it.

To wipe all local state: uninstall, or `adb shell pm clear com.bzucker4.slate`.

## Haptic OEM limits

Slate picks a haptic tier at runtime:

| Tier | When | Feel |
| --- | --- | --- |
| **Rich** | Amplitude control and/or vibration primitives (`VibrationEffect`) | Grit ticks, dissolve thunk, completion pulse |
| **Basic** | Device can vibrate, but no amplitude/primitives | Short one-shots / simple waveforms |
| **None** | No vibrator (many emulators) | Silence |

OEM layers (Samsung, Xiaomi, etc.), battery savers, Do Not Disturb, and “mute vibrations” settings can cap, delay, or drop effects even on **Rich** hardware. Treat haptics as best-effort; verify on a physical device with system vibrations enabled.

## Project layout

Single `:app` module. Application code lives under `app/src/main/java/com/bzucker4/slate/`:

| Package | Role |
| --- | --- |
| `ui/` | Compose screens (home, settings, scratch, blackout) |
| `scratch/` | Frost texture, brush, coverage |
| `haptics/` | Rich / Basic / None feedback |
| `audio/` | Scrub, hum, completion chime |
| `lockout/` | Durations, countdown, proximity (pocket) |
| `data/` | DataStore lockout + settings |

`MainActivity` hosts Compose. Pre-session: duration chips, **Begin**, first-run tip, **Settings** (hum toggle and Emergency Exit). After Begin: scratch → dissolve → Blackout until `lockoutEndsAtEpochMs`.
