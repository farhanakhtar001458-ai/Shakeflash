# ShakeFlash keeps its own classes by default; R8 is enabled for release builds.
-keepattributes *Annotation*

# Compose / Kotlin metadata are handled by the default AGP rules.
# Keep the foreground service + receivers (referenced from the manifest only).
-keep class com.shakeflash.app.service.FlashlightService { *; }
-keep class com.shakeflash.app.receiver.** { *; }

# DataStore uses reflection-free serialization, but keep preferences keys readable in crash reports.
-keepnames class com.shakeflash.app.data.** { *; }
