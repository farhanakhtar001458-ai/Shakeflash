package com.shakeflash.app.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.shakeflash.app.R
import com.shakeflash.app.ShakeFlashApplication
import com.shakeflash.app.data.ShakeFlashSettings
import com.shakeflash.app.flashlight.FlashlightController
import com.shakeflash.app.flashlight.FlashlightController.TorchError
import com.shakeflash.app.flashlight.FlashlightController.TorchResult
import com.shakeflash.app.sensor.ShakeDetector
import com.shakeflash.app.util.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Foreground service that owns shake detection.
 *
 * Why a foreground service? Android freezes background work as soon as the app leaves the
 * screen. A foreground service with an ongoing notification is the *only* supported way to
 * keep an accelerometer listener alive while the phone is locked or another app is open.
 *
 * Lifecycle guarantees implemented here:
 *  - `START_STICKY`            → recreated by the system if the process is killed.
 *  - `stopWithTask="false"`    → survives being swiped away from Recent Apps.
 *  - `onTaskRemoved()`         → keeps running, logs the event.
 *  - BootReceiver              → restarts after reboot **only** if the user left it enabled.
 *  - `onDestroy()`             → unregisters the sensor; nothing keeps running silently.
 */
class FlashlightService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var settingsRepository: com.shakeflash.app.data.SettingsRepository
    private lateinit var flashlight: FlashlightController
    private lateinit var detector: ShakeDetector

    @Volatile
    private var latestSettings = ShakeFlashSettings()

    @Volatile
    private var foregroundStarted = false

    @Volatile
    private var detecting = false

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate")
        val container = (application as ShakeFlashApplication).container
        settingsRepository = container.settingsRepository
        flashlight = container.flashlightController
        detector = ShakeDetector(getSystemService(SensorManager::class.java)) { onShakeDetected() }

        FlashlightNotifications.createChannel(this)
        ServiceState.setRunning(true)

        serviceScope.launch {
            settingsRepository.settings.collect(::onSettingsChanged)
        }
        serviceScope.launch {
            flashlight.state.collect { updateNotification() }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand action=${intent?.action}")
        ensureForeground()

        when (intent?.action) {
            ACTION_STOP -> {
                stopEverything(disableInSettings = true)
                return START_NOT_STICKY
            }

            ACTION_TOGGLE_TORCH -> toggleFromNotification()
            ACTION_TORCH_ON -> setTorchFromNotification(true)
            ACTION_TORCH_OFF -> setTorchFromNotification(false)
            ACTION_START, null -> Unit
        }

        startDetectionIfPossible()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Called when the user swipes the app out of Recent Apps. Because the manifest sets
     * `android:stopWithTask="false"`, the service is *not* stopped; we just re-assert the
     * notification so the user keeps seeing that detection is still running.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        Log.i(TAG, "Task removed from recents — keeping foreground service alive")
        updateNotification()
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy")
        stopDetection()
        // If the service is torn down by the system while the LED is lit, do not leave it
        // burning: the camera client (our process) can outlive the service object.
        if (latestSettings.torchOffWhenServiceStops && flashlight.state.value.isOn) {
            flashlight.setTorch(false)
        }
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        serviceScope.cancel()
        ServiceState.setRunning(false)
        super.onDestroy()
    }

    // ---------------------------------------------------------------------------------
    // Settings & detection
    // ---------------------------------------------------------------------------------

    private fun onSettingsChanged(settings: ShakeFlashSettings) {
        val previous = latestSettings
        latestSettings = settings

        detector.updateSensitivity(settings.sensitivity)

        if (!settings.serviceEnabled) {
            // The user turned the switch off (UI, or a previous notification action).
            Log.i(TAG, "Service disabled by settings — stopping")
            stopEverything(disableInSettings = false)
            return
        }

        if (detecting && previous.lowPowerSensor != settings.lowPowerSensor) {
            detector.restart(settings.sensitivity, settings.lowPowerSensor)
        }
        updateNotification()
    }

    private fun startDetectionIfPossible() {
        if (detecting) return
        if (!flashlight.hasCameraPermission()) {
            Log.w(TAG, "Camera permission missing — not starting the sensor")
            ServiceEvents.emit(ServiceEvents.Event.PermissionRequired)
            updateNotification(errorText = getString(R.string.camera_permission_required))
            return
        }
        detecting = detector.start(latestSettings.sensitivity, latestSettings.lowPowerSensor)
        ServiceState.setDetecting(detecting)
        if (!detecting) {
            updateNotification(errorText = getString(R.string.sensor_unavailable))
        } else {
            updateNotification()
        }
    }

    private fun stopDetection() {
        detector.stop()
        detecting = false
        ServiceState.setDetecting(false)
    }

    private fun onShakeDetected() {
        val result = flashlight.toggle()
        when (result) {
            is TorchResult.Success -> {
                if (latestSettings.hapticFeedback) Haptics.tick(this)
                updateNotification()
            }

            is TorchResult.Failure -> {
                val message = when (result.error) {
                    TorchError.NO_FLASH_UNIT -> getString(R.string.torch_unavailable)
                    TorchError.CAMERA_BUSY -> getString(R.string.torch_busy)
                    TorchError.PERMISSION_DENIED -> getString(R.string.camera_permission_required)
                    TorchError.UNKNOWN -> getString(R.string.torch_unavailable)
                }
                ServiceEvents.emit(ServiceEvents.Event.Error(message))
                updateNotification(errorText = message)
            }
        }
    }

    private fun toggleFromNotification() {
        val result = flashlight.toggle()
        if (result is TorchResult.Failure) {
            updateNotification(errorText = getString(R.string.torch_busy))
        } else {
            updateNotification()
        }
    }

    private fun setTorchFromNotification(on: Boolean) {
        flashlight.setTorch(on)
        updateNotification()
    }

    // ---------------------------------------------------------------------------------
    // Foreground notification
    // ---------------------------------------------------------------------------------

    private fun ensureForeground() {
        if (foregroundStarted) return
        val notification = FlashlightNotifications.build(
            context = this,
            torchOn = flashlight.state.value.isOn,
            detecting = detecting,
        )
        try {
            ServiceCompat.startForeground(
                this,
                FlashlightNotifications.NOTIFICATION_ID,
                notification,
                foregroundServiceType(),
            )
            foregroundStarted = true
        } catch (error: Exception) {
            // e.g. ForegroundServiceStartNotAllowedException on Android 12+.
            Log.e(TAG, "startForeground() rejected by the system", error)
            ServiceEvents.emit(ServiceEvents.Event.Error(getString(R.string.service_start_failed)))
            stopSelf()
        }
    }

    private fun updateNotification(errorText: String? = null) {
        if (!foregroundStarted) return
        runCatching {
            ServiceCompat.startForeground(
                this,
                FlashlightNotifications.NOTIFICATION_ID,
                FlashlightNotifications.build(
                    context = this,
                    torchOn = flashlight.state.value.isOn,
                    detecting = detecting,
                    errorText = errorText,
                ),
                foregroundServiceType(),
            )
        }.onFailure { Log.w(TAG, "Could not update notification", it) }
    }

    private fun foregroundServiceType(): Int =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }

    // ---------------------------------------------------------------------------------
    // Shutdown
    // ---------------------------------------------------------------------------------

    private fun stopEverything(disableInSettings: Boolean) {
        stopDetection()
        if (latestSettings.torchOffWhenServiceStops) {
            flashlight.setTorch(false)
        }
        if (disableInSettings) {
            // Must survive onDestroy(), so it runs on an independent IO scope.
            CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                settingsRepository.setServiceEnabled(false)
            }
        }
        if (foregroundStarted) {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            foregroundStarted = false
        }
        ServiceState.setRunning(false)
        stopSelf()
    }

    companion object {
        private const val TAG = "FlashlightService"

        const val ACTION_START = "com.shakeflash.app.action.START"
        const val ACTION_STOP = "com.shakeflash.app.action.STOP"
        const val ACTION_TOGGLE_TORCH = "com.shakeflash.app.action.TOGGLE_TORCH"
        const val ACTION_TORCH_ON = "com.shakeflash.app.action.TORCH_ON"
        const val ACTION_TORCH_OFF = "com.shakeflash.app.action.TORCH_OFF"

        /** True when the service is already alive in this process. */
        val isRunning: Boolean get() = ServiceState.running.value
    }
}

/**
 * Small helper so the UI / boot receiver never has to deal with foreground-service
 * start rules themselves. Returns false when Android refuses the start.
 */
object FlashlightServiceController {

    fun start(context: Context): Boolean {
        val intent = Intent(context, FlashlightService::class.java).setAction(FlashlightService.ACTION_START)
        return try {
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (error: Exception) {
            Log.e("FlashlightService", "Could not start foreground service", error)
            false
        }
    }

    /** Asks a running service to shut down cleanly (torch off, sensor unregistered). */
    fun stop(context: Context) {
        if (!ServiceState.running.value) return
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FlashlightService::class.java).setAction(FlashlightService.ACTION_STOP),
            )
        }
    }

    fun sendIntent(context: Context, action: String) {
        runCatching {
            ContextCompat.startForegroundService(
                context,
                Intent(context, FlashlightService::class.java).setAction(action),
            )
        }
    }
}
