package com.shakeflash.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.shakeflash.app.ShakeFlashApplication
import com.shakeflash.app.service.FlashlightServiceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Restores the shake-detection service after a reboot or an app update.
 *
 * Deliberately conservative:
 *  - starts nothing unless the user left the service enabled;
 *  - bails out when the camera permission has been revoked;
 *  - never retries in a loop if Android denies the background foreground-service start.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in SUPPORTED_ACTIONS) {
            Log.d(TAG, "Ignoring action $action")
            return
        }

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            try {
                val container = (appContext as ShakeFlashApplication).container
                val settings = container.settingsRepository.settings.first()

                if (!settings.serviceEnabled) {
                    Log.i(TAG, "Service was disabled by the user — not restoring after $action")
                    return@launch
                }
                if (!container.permissionManager.hasCameraPermission()) {
                    Log.w(TAG, "Camera permission revoked — not restoring the service")
                    return@launch
                }

                val started = FlashlightServiceController.start(appContext)
                Log.i(TAG, "Restore after $action: started=$started")
            } catch (error: Exception) {
                Log.e(TAG, "Boot restore failed", error)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"

        val SUPPORTED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
        )
    }
}
