# ShakeFlash — Shake to Flashlight

A complete, native Android app (Kotlin + Jetpack Compose) that turns the flashlight on when you
shake the phone and off when you shake it again — **even when the screen is locked, another app is
in the foreground, or ShakeFlash was swiped away from Recent Apps.**

> **Jaldi start karna hai?** Repo clone karo → Android Studio me kholo → Run dabao.
> Ya GitHub Actions tab se `ShakeFlash-debug-apk` artifact download kar lo (niche steps hain).

<p align="center">
  <em>Dark, shadcn/ui-inspired UI · real foreground service · no internet permission</em>
</p>

---

## Table of contents

1. [What it does](#1-what-it-does)
2. [Screens & UI](#2-screens--ui)
3. [How the shake detection works](#3-how-the-shake-detection-works)
4. [Background behaviour (the important part)](#4-background-behaviour-the-important-part)
5. [Permissions and exactly why each exists](#5-permissions-and-exactly-why-each-exists)
6. [Project structure](#6-project-structure)
7. [Build the APK](#7-build-the-apk)
8. [Install & first run](#8-install--first-run)
9. [Testing](#9-testing)
10. [Troubleshooting by OEM](#10-troubleshooting-by-oem)
11. [Technical summary & honest limitations](#11-technical-summary--honest-limitations)

---

## 1. What it does

| Feature | Implementation |
| --- | --- |
| Shake to toggle the torch | Accelerometer → `ShakeProcessor` → `CameraManager.setTorchMode()` |
| Works while locked / backgrounded / after recents swipe | Android **foreground service** + ongoing notification, `stopWithTask="false"`, `START_STICKY` |
| Comes back after reboot | `BootReceiver` on `BOOT_COMPLETED` / `MY_PACKAGE_REPLACED`, only if you left it enabled |
| Configurable sensitivity | 0–100 slider, mapped to a 26 → 9.5 m/s² linear-acceleration threshold, applied live |
| Big ON/OFF toggle + status | Circular power dial with animated glow, plus ON/OFF status chip |
| Battery information | Live battery %, charging state, temperature, power-save flag, estimated extra drain |
| Permission status | Per-permission card with "Granted / Needed" chips and one-tap request |
| Settings screen | Bottom sheet: haptics, low-power sensor, torch-off-on-stop, battery optimisation, OEM autostart |
| Privacy | **No `INTERNET` permission at all** — nothing leaves the device |

Turning the service **off** really stops detection: the sensor is unregistered, the foreground
notification is removed, and the preference is persisted so a reboot will not bring it back.

---

## 2. Screens & UI

Everything is Jetpack Compose, dark-only (a white UI would destroy your night vision — the whole
point of a flashlight app). The visual language borrows shadcn/ui: near-black `#09090B` background,
`#18181B` surfaces, hairline `#27272A` borders, one amber accent (`#FBBF24`) and generous 22 dp radii.

**See it without building:** open [`preview/index.html`](preview/index.html) in any browser (or run
`python3 -m http.server -d preview 8000`). It is a self-contained mock of the same design — tap the
dial, flip the switch, drag the slider and open the settings sheet to feel the button/sliding effects.

**Home screen**

- **Header** — logo mark, "ShakeFlash / Shake to flashlight", settings button.
- **Flashlight card** — `ON/OFF` status chip, a 226 dp power dial (expanding amber rings while on,
  spring press-in, tap ripple), status copy, and a battery-drain warning when the torch is lit.
- **Shake detection card** — big pill switch, listening strip with animated level bars, green dot.
- **Shake sensitivity card** — custom slider (spring thumb, gradient fill) with a `0–100` chip and a
  "Gentle ↔ Vigorous" scale.
- **Permissions card** — one row per permission, why it is needed, and "Grant permissions".
- **Battery card** — level bar, state / sensor-rate / temperature mini-stats, and a warning with a
  shortcut to the battery-optimisation list when ShakeFlash is still optimised.
- **Footer** — privacy line and version.

**Settings sheet** — haptics, low-power sensor (16 Hz vs 50 Hz), "torch off when service stops",
battery optimisation, OEM autostart helper, app info.

**Custom components** (all hand-written in `ui/components/`) rather than stock Material widgets:

| Component | The effect |
| --- | --- |
| `SFButton` | spring press-scale + a light **sheen that slides** across the face while pressed |
| `SFSwitch` | pill track, thumb **slides with a spring**, colour animates |
| `SFSlider` | custom gesture handling, animated gradient fill + **sliding glowing thumb** |
| `PowerDial` | pulsing concentric rings, radial glow, ripple |
| `SFCard` / `SFIconBadge` / `SFStatusChip` | shadcn-style surfaces, badges and status pills |

---

## 3. How the shake detection works

`ShakeProcessor` is pure Kotlin (no Android imports) so the maths is unit-tested on the JVM.
`ShakeDetector` is the thin Android wrapper that registers `TYPE_ACCELEROMETER` on a dedicated
`HandlerThread` (never the main thread).

A shake is accepted only when **all** of these are true:

1. **Gravity is filtered out.** A low-pass filter (α = 0.8) tracks gravity; subtracting it leaves
   linear acceleration, so tilting the phone or carrying it does not look like a shake.
2. **A sharp spike crosses the threshold.** Sensitivity 0 → 26 m/s², 100 → 9.5 m/s².
3. **The signal relaxes between spikes** (falls below 55 % of the threshold). One long, sustained
   acceleration — a bump, a car door, a pocket press — is therefore rejected.
4. **Two distinct spikes land within 450 ms.** A single knock never toggles the torch.
5. **Spikes are ≥ 80 ms apart** and the gesture ends with a **1 s cooldown**, so one shake cannot
   toggle twice.
6. **The first 350 ms after registration is ignored**, because you are still holding the phone.

The first 350 ms rule, peak counting and the release test were tuned against a reference simulation
of real sensor traces, then frozen as unit tests in
`app/src/test/java/com/shakeflash/app/sensor/ShakeProcessorTest.kt`:

| Scenario | Result |
| --- | --- |
| single sharp knock | ignored |
| sustained 600 ms bump | ignored |
| walking jitter / pocket steps | ignored |
| two-beat shake | one toggle |
| shake, then shake again inside 1 s | one toggle (cooldown) |
| shake, wait, shake | two toggles |
| gentle shake at sensitivity 20 vs 100 | ignored / accepted |

If your phone feels over-eager, drop the slider; if it needs a violent shake, raise it.

---

## 4. Background behaviour (the important part)

`FlashlightService` is a **foreground service** (`specialUse` type on Android 14+). It owns the
sensor listener and a low-importance, silent, ongoing notification with **Turn off** and **Stop**
actions.

| Situation | What happens | Mechanism |
| --- | --- | --- |
| Home button pressed | keeps detecting | foreground service |
| Screen locked | keeps detecting + torch works | `setTorchMode()` is not tied to a visible UI |
| Another app in foreground | keeps detecting | foreground service outranks background limits |
| App swiped from Recent Apps | **stays alive** | `android:stopWithTask="false"` |
| Process killed by the OS | system restarts the service | `START_STICKY` |
| Device rebooted | restored if enabled + camera permission present | `BootReceiver` (`BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`) |
| User turns the switch off | sensor unregistered, notification removed, preference cleared, torch off (configurable) | `ACTION_STOP` + `SettingsRepository` |
| Camera permission revoked | service keeps the notification but does not pretend to listen | `startDetectionIfPossible()` |

**No fake tricks.** ShakeFlash never tries to bypass Android's background policies: it does not
request `SYSTEM_ALERT_WINDOW`, does not abuse Accessibility, does not loop-restart itself, and does
not use `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`. Battery/autostart changes are always made by *you* in
system settings via the buttons in the app.

---

## 5. Permissions and exactly why each exists

| Permission | Type | Why it is needed |
| --- | --- | --- |
| `CAMERA` | runtime (dangerous) | Android requires it to drive the LED through `CameraManager.setTorchMode()`. No photo or video is ever captured. |
| `POST_NOTIFICATIONS` | runtime, Android 13+ | The ongoing "listening" notification is what makes the service a *foreground* service. Without it Android can freeze detection. |
| `FOREGROUND_SERVICE` | normal | Permission to run a foreground service at all. |
| `FOREGROUND_SERVICE_SPECIAL_USE` | normal, Android 14+ | Declares the service type; a sensor-driven torch does not match any predefined type, so `specialUse` is the correct, documented choice (a `<property>` explains the subtype to the system). |
| `RECEIVE_BOOT_COMPLETED` | normal | Restore detection after a reboot — only while the user has it enabled. |
| `VIBRATE` | normal | Optional haptic confirmation on a recognised shake. |

Deliberately **not** requested: `INTERNET`, `ACCESSIBILITY_SERVICE`, `SYSTEM_ALERT_WINDOW`,
`READ_PHONE_STATE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, location, contacts, storage.

On Android 8–12 there is no notification permission; the app reports it as granted.

---

## 6. Project structure

```
Shakeflash/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/libs.versions.toml            # version catalog (AGP 8.7.3, Kotlin 2.0.21, Compose BOM)
├── gradle/wrapper/…                     # Gradle 8.11.1 wrapper
├── .github/workflows/build.yml          # CI: builds debug + release APK artifacts
└── app/
    ├── build.gradle.kts                 # minSdk 26, targetSdk 35, Compose, R8
    ├── proguard-rules.pro
    └── src/
        ├── main/AndroidManifest.xml
        ├── main/java/com/shakeflash/app/
        │   ├── MainActivity.kt              # edge-to-edge Compose host + permission launcher
        │   ├── ShakeFlashApplication.kt     # AppContainer (SettingsRepository, FlashlightController, PermissionManager)
        │   ├── data/SettingsRepository.kt   # DataStore + ShakeFlashSettings
        │   ├── flashlight/FlashlightController.kt   # setTorchMode + TorchCallback state
        │   ├── sensor/ShakeProcessor.kt     # pure shake maths (unit tested)
        │   ├── sensor/ShakeDetector.kt      # SensorEventListener on a HandlerThread
        │   ├── service/FlashlightService.kt # foreground service + lifecycle
        │   ├── service/FlashlightNotifications.kt
        │   ├── service/ServiceState.kt      # StateFlow seen by the UI + one-shot events
        │   ├── receiver/BootReceiver.kt
        │   ├── permissions/PermissionManager.kt
        │   ├── util/{BatteryInfo,Haptics,SystemIntents}.kt
        │   └── ui/
        │       ├── ShakeFlashViewModel.kt
        │       ├── screens/{HomeScreen,SettingsSheet}.kt
        │       ├── components/{SFCard,SFButton,SFSwitch,SFSlider,PowerDial}.kt
        │       ├── icons/SFIcons.kt
        │       └── theme/{Color,Type,Theme}.kt
        ├── main/res/…                    # strings, dark theme, adaptive icon, notification icon
        └── test/java/…/ShakeProcessorTest.kt
```

The UI and the service never talk through a binder: both observe the same application-scoped
`FlashlightController` (torch truth) and `ServiceState` (service truth). That is why the dial, the
notification and the real LED can never disagree.

---

## 7. Build the APK

### Requirements

- **Android Studio** Ladybug (2024.2) or newer, **or** JDK 17 + Android SDK 35 on the command line.
- Android SDK Platform **35** and Build-Tools installed (Android Studio will prompt).

### Option A — Android Studio

1. `File ▸ Open…` and select the project folder.
2. Wait for the Gradle sync (it downloads AGP, Compose and the SDK bits once).
3. `Run ▸ Run 'app'` on a device/emulator (API 26+).
4. For a shareable file: `Build ▸ Build App Bundle(s) / APK(s) ▸ Build APK(s)`.
   The APK lands in `app/build/outputs/apk/debug/app-debug.apk`.

### Option B — command line

```bash
cd Shakeflash

# Linux/macOS
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

Output:

```
app/build/outputs/apk/debug/app-debug.apk          # debug, package id com.shakeflash.app.debug
app/build/outputs/apk/release/app-release.apk      # minified, package id com.shakeflash.app
```

Install it:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

> The `gradle-wrapper.jar` is committed, so `./gradlew` works immediately. If your machine has a
> globally installed Gradle instead, `gradle wrapper --gradle-version 8.11.1` regenerates it.

### Option C — build in the cloud (no local SDK needed)

`.github/workflows/build.yml` runs on every push and pull request. Open the **Actions** tab →
select the latest *Build APK* run → download the artifact:

- `ShakeFlash-debug-apk` → installable immediately
- `ShakeFlash-release-apk` → minified release build

You can also trigger it manually: *Actions ▸ Build APK ▸ Run workflow*.

### Release builds and signing

`assembleRelease` is wired to the debug signing config so it produces an installable APK out of the
box. For anything you publish, create your own keystore and signing config:

```bash
keytool -genkeypair -v -keystore shakeflash-release.jks \
  -alias shakeflash -keyalg RSA -keysize 2048 -validity 10000
```

```kotlin
// app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            storeFile = file("../shakeflash-release.jks")
            storePassword = System.getenv("SHAKEFLASH_STORE_PASSWORD")
            keyAlias = "shakeflash"
            keyPassword = System.getenv("SHAKEFLASH_KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

Never commit the keystore or its passwords — `.gitignore` already blocks `*.jks`, `*.keystore` and
`keystore.properties`.

---

## 8. Install & first run

1. Install the APK (or press Run in Android Studio).
2. Tap **Grant permissions** → allow **Camera** (and **Notifications** on Android 13+).
3. Flip **Shake detection** on. A silent ongoing notification appears — that is the service.
4. **Flip ON/OFF** — but first, tap the dial to test the torch without shaking.
5. Shake the phone: the torch toggles and (optionally) buzzes.

To confirm it survives everything: lock the screen, shake again; open another app, shake again;
swipe ShakeFlash out of Recents, shake again.

---

## 9. Testing

**Unit tests** (shake maths, no device needed):

```bash
./gradlew testDebugUnitTest
# report: app/build/reports/tests/testDebugUnitTest/index.html
```

**Manual checklist on a device**

| Check | Expected |
| --- | --- |
| Tap the dial | torch on, chip shows `ON`, battery warning appears |
| Shake twice quickly | exactly one toggle (1 s cooldown) |
| Single hard knock | nothing happens |
| Lock screen → shake | torch toggles |
| Open Camera app → shake | service reports "camera is busy" in the notification/snackbar |
| Recents swipe → shake | still toggles |
| Toggle service off | notification disappears, shaking does nothing, reboot does not restore it |
| Reboot with service on | notification returns after boot |

**What was already verified in CI-less environments:** the Kotlin sources were parsed with the real
Kotlin compiler (no syntax errors), and `ShakeProcessor` + `ShakeProcessorTest` were compiled and
executed directly — 10/10 behavioural tests pass. A full APK build needs the Android SDK, which is
why the GitHub Actions workflow exists.

**Emulator:** `Extended controls ▸ Virtual sensors ▸ Device pose` and rotate/move the device to feed
the accelerometer; or use the *Accelerometer* tab's virtual values.

---

## 10. Troubleshooting by OEM

| Symptom | Fix |
| --- | --- |
| Detection stops after a while with the screen off | Exempt ShakeFlash from battery optimisation (Battery card → *Open battery settings*). |
| Service dies on Xiaomi/POCO/Redmi after a while | Settings sheet → *Autostart / background permissions*, enable autostart, set Battery saver to *No restrictions*. |
| Same on Oppo/Realme/Vivo/iQOO | Allow "Auto-startup" and "Allow background activity" in the OEM settings screen the app opens. |
| Torch does nothing | Make sure Camera permission is granted and no other app holds the camera. Some devices only expose the torch while the app has `CAMERA`. |
| Notification missing on Android 13+ | The service still runs, but Android asks for notification permission — grant it so the foreground service is not throttled. |
| Torch stays on after force-stopping the app | Force-stop kills the process; the camera service normally releases the LED. Turn it off from Quick Settings if a specific device misbehaves. |

---

## 11. Technical summary & honest limitations

| Item | Value |
| --- | --- |
| Language / UI | Kotlin 2.0.21, Jetpack Compose (BOM 2024.12.01, Material 3) |
| Min / target / compile SDK | 26 (Android 8.0) / 35 / 35 |
| Build | AGP 8.7.3, Gradle 8.11.1, JDK 17, R8 + resource shrinking |
| Torch API | `CameraManager.setTorchMode()` + `TorchCallback` |
| Sensor | `TYPE_ACCELEROMETER`, `SENSOR_DELAY_GAME` (50 Hz) or `SENSOR_DELAY_UI` (16 Hz) on its own thread |
| Persistence | Preferences DataStore |
| Architecture | Application-scoped container → `StateFlow` → Compose; foreground service shares the same singletons |

Honest limits:

- **Doze**: in deep Doze some devices suspend the accelerometer entirely. The battery-exemption
  shortcut exists for exactly this reason; ShakeFlash does not try to defeat Doze.
- **OEM killers**: aggressive vendor task killers can still stop any service. Autostart settings are
  a user action, never something the app fakes.
- **Camera in use**: while another app owns the camera, the torch cannot be switched — that is an
  Android constraint, and the app surfaces it instead of pretending.
- **Android Go / some tablets** have no flash unit; the app detects this and shows "NO FLASH".
- This project intentionally keeps UI copy inline in Compose for readability; service/notification
  strings live in `res/values/strings.xml` and are ready for translation.

---

Built with Kotlin, Compose and a lot of respect for Android's background-execution rules.
