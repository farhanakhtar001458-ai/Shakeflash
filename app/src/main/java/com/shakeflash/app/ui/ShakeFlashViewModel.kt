package com.shakeflash.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shakeflash.app.R
import com.shakeflash.app.ShakeFlashApplication
import com.shakeflash.app.data.ShakeFlashSettings
import com.shakeflash.app.flashlight.FlashlightController
import com.shakeflash.app.flashlight.FlashlightController.TorchError
import com.shakeflash.app.flashlight.FlashlightController.TorchResult
import com.shakeflash.app.permissions.PermissionManager
import com.shakeflash.app.service.FlashlightServiceController
import com.shakeflash.app.service.ServiceEvents
import com.shakeflash.app.service.ServiceState
import com.shakeflash.app.util.BatteryInfo
import com.shakeflash.app.util.BatterySnapshot
import com.shakeflash.app.util.Haptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Single UI-facing state holder. It merges persisted settings, live torch state,
 * foreground-service state and system facts (battery + permission status) into one
 * immutable [UiState] that Compose renders.
 */
class ShakeFlashViewModel(application: Application) : AndroidViewModel(application) {

    data class UiState(
        val settings: ShakeFlashSettings = ShakeFlashSettings(),
        val torch: FlashlightController.TorchState = FlashlightController.TorchState(),
        val serviceRunning: Boolean = false,
        val detecting: Boolean = false,
        val permissionItems: List<PermissionManager.PermissionItem> = emptyList(),
        val battery: BatterySnapshot = BatterySnapshot(
            levelPercent = -1,
            isCharging = false,
            temperatureC = null,
            powerSaveMode = false,
            optimized = false,
        ),
        val ready: Boolean = false,
    ) {
        val allPermissionsGranted: Boolean
            get() = permissionItems.filter { it.required }.all { it.granted }

        val missingPermissions: List<PermissionManager.PermissionItem>
            get() = permissionItems.filter { it.required && !it.granted }

        /** "Active" only when the sensor is genuinely registered. */
        val detectionActive: Boolean get() = detecting && serviceRunning
    }

    private val appContext = application.applicationContext
    private val container = (application as ShakeFlashApplication).container
    private val repository = container.settingsRepository
    private val flashlight = container.flashlightController
    private val permissions = container.permissionManager

    private data class SystemSnapshot(
        val battery: BatterySnapshot,
        val permissionItems: List<PermissionManager.PermissionItem>,
    )

    private data class Runtime(
        val running: Boolean,
        val detecting: Boolean,
        val system: SystemSnapshot,
        val pendingSensitivity: Int?,
    )

    private val systemState = MutableStateFlow(snapshotSystem())
    private val pendingSensitivity = MutableStateFlow<Int?>(null)
    private var sensitivityJob: Job? = null

    private val messageChannel = Channel<String>(Channel.BUFFERED)

    /** Ready-to-show snackbar texts. */
    val messages: Flow<String> = messageChannel.receiveAsFlow()

    private val settingsCache = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = ShakeFlashSettings(),
    )

    private val runtime = combine(
        ServiceState.running,
        ServiceState.detecting,
        systemState,
        pendingSensitivity,
    ) { running, detecting, system, pending ->
        Runtime(running, detecting, system, pending)
    }

    val uiState: StateFlow<UiState> = combine(
        settingsCache,
        flashlight.state,
        runtime,
    ) { settings, torch, live ->
        val effectiveSettings = live.pendingSensitivity?.let { pending ->
            settings.copy(sensitivity = pending)
        } ?: settings

        UiState(
            settings = effectiveSettings,
            torch = torch,
            serviceRunning = live.running,
            detecting = live.detecting,
            permissionItems = live.system.permissionItems,
            battery = live.system.battery,
            ready = true,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UiState(),
    )

    init {
        viewModelScope.launch {
            ServiceEvents.events.collect { event ->
                when (event) {
                    is ServiceEvents.Event.Error -> messageChannel.trySend(event.message)
                    ServiceEvents.Event.PermissionRequired ->
                        messageChannel.trySend(text(R.string.msg_camera_permission_needed))
                }
            }
        }
    }

    // ---------------------------------------------------------------------------------
    // System refresh (permissions / battery change outside of Compose)
    // ---------------------------------------------------------------------------------

    fun refreshSystemState() {
        systemState.value = snapshotSystem()
    }

    fun onPermissionsResult() = refreshSystemState()

    private fun snapshotSystem(): SystemSnapshot = SystemSnapshot(
        battery = BatteryInfo.snapshot(appContext),
        permissionItems = permissions.statuses(),
    )

    // ---------------------------------------------------------------------------------
    // Actions
    // ---------------------------------------------------------------------------------

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                if (!permissions.hasCameraPermission()) {
                    messageChannel.trySend(text(R.string.msg_camera_permission_needed))
                    refreshSystemState()
                    return@launch
                }
                repository.setServiceEnabled(true)
                val started = FlashlightServiceController.start(appContext)
                if (!started) {
                    repository.setServiceEnabled(false)
                    messageChannel.trySend(text(R.string.msg_service_blocked))
                }
            } else {
                repository.setServiceEnabled(false)
                FlashlightServiceController.stop(appContext)
            }
            refreshSystemState()
        }
    }

    /** Torch toggle from the big dial. Works with or without the background service. */
    fun toggleTorch() {
        viewModelScope.launch {
            val settings = settingsCache.value
            when (val result = flashlight.setTorch(!flashlight.state.value.isOn)) {
                is TorchResult.Success -> {
                    if (settings.hapticFeedback) Haptics.tick(appContext, durationMs = 20)
                }

                is TorchResult.Failure -> messageChannel.trySend(messageFor(result.error))
            }
        }
    }

    fun setSensitivity(value: Int) {
        val clamped = value.coerceIn(
            ShakeFlashSettings.MIN_SENSITIVITY,
            ShakeFlashSettings.MAX_SENSITIVITY,
        )
        pendingSensitivity.value = clamped
        sensitivityJob?.cancel()
        sensitivityJob = viewModelScope.launch {
            // Coalesce slider frames: DataStore writes only after the finger pauses.
            delay(120)
            repository.setSensitivity(clamped)
            pendingSensitivity.value = null
        }
    }

    fun setHapticFeedback(enabled: Boolean) {
        viewModelScope.launch { repository.setHapticFeedback(enabled) }
    }

    fun setLowPowerSensor(enabled: Boolean) {
        viewModelScope.launch { repository.setLowPowerSensor(enabled) }
    }

    fun setTorchOffWhenServiceStops(enabled: Boolean) {
        viewModelScope.launch { repository.setTorchOffWhenServiceStops(enabled) }
    }

    fun sensitivityToPercent(value: Float): Int = (value * 100f).roundToInt().coerceIn(0, 100)

    private fun messageFor(error: TorchError): String = text(
        when (error) {
            TorchError.NO_FLASH_UNIT -> R.string.torch_unavailable
            TorchError.CAMERA_BUSY -> R.string.torch_busy
            TorchError.PERMISSION_DENIED -> R.string.camera_permission_required
            TorchError.UNKNOWN -> R.string.msg_torch_unknown
        },
    )

    private fun text(resId: Int): String = appContext.getString(resId)
}
