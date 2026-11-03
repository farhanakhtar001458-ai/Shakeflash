package com.shakeflash.app.flashlight

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Application-scoped torch owner.
 *
 * A single instance lives in [com.shakeflash.app.AppContainer] so that the Compose UI and
 * the foreground service always agree on the flashlight state, and so the
 * [CameraManager.TorchCallback] stays registered while the app is alive.
 *
 * The torch is *not* tied to a camera session: `setTorchMode` keeps working while the
 * screen is locked and while the app is in the background.
 */
class FlashlightController(context: Context) {

    data class TorchState(
        val available: Boolean = false,
        val isOn: Boolean = false,
        val lastError: TorchError? = null,
    )

    enum class TorchError { NO_FLASH_UNIT, CAMERA_BUSY, PERMISSION_DENIED, UNKNOWN }

    sealed interface TorchResult {
        data object Success : TorchResult
        data class Failure(val error: TorchError) : TorchResult
    }

    private val appContext = context.applicationContext
    private val cameraManager: CameraManager? =
        appContext.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
    private val mainHandler = Handler(Looper.getMainLooper())

    /** Back camera that actually has a flash unit, or null on devices without one. */
    val cameraId: String? = findFlashCameraId()

    private val _state = MutableStateFlow(TorchState(available = cameraId != null))
    val state: StateFlow<TorchState> = _state.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) {
                // Somebody (another app, the OS, or us) changed the torch: mirror the truth.
                _state.update { it.copy(available = true, isOn = enabled) }
            }
        }

        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) {
                _state.update { it.copy(available = false, isOn = false) }
            }
        }
    }

    init {
        val manager = cameraManager
        val id = cameraId
        if (manager != null && id != null) {
            runCatching { manager.registerTorchCallback(torchCallback, mainHandler) }
                .onFailure { Log.w(TAG, "Could not register torch callback", it) }
        }
    }

    /**
     * Turns the torch on/off. Safe to call from any thread and from the service or the UI.
     * Never throws — failures are reported as [TorchResult.Failure] and surfaced in [state].
     */
    fun setTorch(enabled: Boolean): TorchResult {
        val manager = cameraManager
        val id = cameraId
        if (manager == null || id == null) {
            _state.update { it.copy(available = false, isOn = false, lastError = TorchError.NO_FLASH_UNIT) }
            return TorchResult.Failure(TorchError.NO_FLASH_UNIT)
        }
        if (!hasCameraPermission()) {
            _state.update { it.copy(lastError = TorchError.PERMISSION_DENIED) }
            return TorchResult.Failure(TorchError.PERMISSION_DENIED)
        }
        return try {
            manager.setTorchMode(id, enabled)
            // Optimistically update; the TorchCallback will confirm.
            _state.update { it.copy(available = true, isOn = enabled, lastError = null) }
            TorchResult.Success
        } catch (error: CameraAccessException) {
            val mapped = when (error.reason) {
                CameraAccessException.CAMERA_IN_USE,
                CameraAccessException.MAX_CAMERAS_IN_USE,
                CameraAccessException.CAMERA_DISABLED,
                -> TorchError.CAMERA_BUSY

                else -> TorchError.UNKNOWN
            }
            Log.w(TAG, "setTorchMode failed (reason=${error.reason})", error)
            _state.update { it.copy(lastError = mapped) }
            TorchResult.Failure(mapped)
        } catch (error: SecurityException) {
            Log.w(TAG, "Torch requires camera permission on this device", error)
            _state.update { it.copy(lastError = TorchError.PERMISSION_DENIED) }
            TorchResult.Failure(TorchError.PERMISSION_DENIED)
        } catch (error: Exception) {
            Log.w(TAG, "Unexpected torch failure", error)
            _state.update { it.copy(lastError = TorchError.UNKNOWN) }
            TorchResult.Failure(TorchError.UNKNOWN)
        }
    }

    fun toggle(): TorchResult = setTorch(!_state.value.isOn)

    fun clearError() = _state.update { it.copy(lastError = null) }

    fun hasCameraPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.CAMERA,
    ) == PackageManager.PERMISSION_GRANTED

    private fun findFlashCameraId(): String? {
        val manager = cameraManager ?: return null
        return runCatching {
            val candidates = manager.cameraIdList.mapNotNull { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                val hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                if (!hasFlash) return@mapNotNull null
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                id to facing
            }
            // Prefer the back camera: that is the torch users expect.
            (candidates.firstOrNull { it.second == CameraCharacteristics.LENS_FACING_BACK }
                ?: candidates.firstOrNull())?.first
        }.getOrNull()
    }

    private companion object {
        const val TAG = "FlashlightController"
    }
}
